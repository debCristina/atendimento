# Decisões técnicas

Este documento registra as principais decisões de arquitetura e design do projeto, e o motivo de cada uma — no espírito dos ADRs (*Architecture Decision Records*). Para instruções de execução, veja o [README](../README.md).

---

## Sumário

- [Domínio rico](#domínio-rico)
- [Núcleo compartilhado](#núcleo-compartilhado)
- [Endereço como objeto de valor](#endereço-como-objeto-de-valor)
- [Cadastro em múltiplas etapas](#cadastro-em-múltiplas-etapas)
- [Integração com o ViaCEP](#integração-com-o-viacep)
- [Segurança](#segurança)
- [Administrador inicial](#administrador-inicial)
- [Busca com Elasticsearch](#busca-com-elasticsearch)
- [Tratamento de erros](#tratamento-de-erros)
- [Auditoria com AOP](#auditoria-com-aop)
- [Banco de dados](#banco-de-dados)

---

## Domínio rico

As regras de negócio da solicitação ficam na própria entidade `Solicitation`: ela só pode ser criada por um método de fábrica, só aceita edição em `DRAFT`, controla a ordem das etapas e as transições de status (`submit`, `startReview`, `decide`). Os services apenas orquestram: buscam, verificam dono/UF e delegam. Isso permite testar as regras com testes unitários puros, sem banco nem Spring.

## Núcleo compartilhado

O papel do usuário (`Role`) e a UF (`Uf`) são usados por vários módulos — principalmente pela segurança, que é transversal —, por isso ficam em `shared/domain`. Isso evita uma dependência circular entre `shared` e `user`. Pelo mesmo motivo, o `JwtService` recebe apenas o id e o papel para gerar o token, e não a entidade de usuário. O núcleo é mantido pequeno de propósito: só entram conceitos que realmente atravessam os módulos.

## Endereço como objeto de valor

O endereço é um `@Embeddable`: no Java, uma classe coesa com comportamento próprio (normalização do CEP); no banco, colunas na própria tabela `solicitations`. Ele não tem identidade nem existe sem a solicitação, então uma tabela separada seria complexidade sem ganho.

## Cadastro em múltiplas etapas

- A solicitação nasce com a etapa 1 e é salva como rascunho a cada etapa.
- As etapas seguem a ordem (a 3 exige a 2), mas qualquer etapa pode ser corrigida enquanto em `DRAFT`, sem o `currentStep` regredir.
- Cada etapa é validada ao ser salva, e **tudo é revalidado no envio** — inclusive a data preferencial, que pode ter vencido entre o salvamento e o envio.
- O envio incompleto devolve **todas** as pendências de uma vez, cada uma com a etapa e o campo.
- Validações de formato ficam nos DTOs (Bean Validation, com `trim` aplicado antes da validação); regras que envolvem mais de um campo ou o tempo (como `HIGH` ≥ 100 e a data no passado) ficam na entidade, usando o fuso `America/Sao_Paulo`.

## Integração com o ViaCEP

- A consulta é feita pela interface `CepLookup`, definida pela camada de serviço; o `ViaCepClient` é a implementação na infraestrutura. A regra de negócio não depende do fornecedor, e os testes usam uma implementação falsa.
- O ViaCEP responde `200` com `{"erro": true}` para CEP inexistente; isso é tratado como "não encontrado".
- **CEP inexistente → 422; serviço indisponível → 503.** Em ambos os casos, a etapa 2 não é concluída.
- Toda chamada tem **timeout de 3 segundos**, para que uma lentidão do ViaCEP não esgote as threads da aplicação.

## Segurança

- **JWT (HS256), API stateless.** O token carrega apenas o id e o papel. As UFs do analista não vão no token: são consultadas no banco, para que uma mudança de cobertura valha imediatamente.
- **Três níveis de controle:** papel (no `SecurityConfig`), dono e UF (nos services, porque dependem dos dados).
- **404 em vez de 403** para solicitações de outro cliente ou fora da UF do analista: não se revela que o recurso existe.
- O cadastro público não aceita o campo `role`, evitando *mass assignment*.
- O login responde a mesma mensagem para e-mail inexistente e senha errada, evitando a enumeração de usuários.
- Senhas com BCrypt; configurações sensíveis vêm de variáveis de ambiente.

## Administrador inicial

Como o cadastro público só cria clientes e só um admin cria outros usuários, o primeiro admin é criado por um **seeder** (`ApplicationRunner`) na inicialização, apenas se ainda não existir nenhum admin. E-mail e senha vêm de variáveis de ambiente, e o hash é gerado pelo próprio `PasswordEncoder` — o mesmo padrão usado pela imagem oficial do PostgreSQL. Inserir pela migration foi descartado porque deixaria o hash fixo no repositório.

As rotinas que rodam na inicialização (a criação do admin e a sincronização do índice de busca) ficam num pacote `bootstrap` em cada módulo, porque não são regras de negócio, nem casos de uso, nem acesso a dados.

## Busca com Elasticsearch

- O **PostgreSQL é a fonte da verdade**; o Elasticsearch guarda uma cópia otimizada para busca.
- A indexação acontece por **evento**: os services publicam `SolicitationChangedEvent`, e um listener indexa **somente após o commit** (`@TransactionalEventListener(AFTER_COMMIT)`). Nada que sofreu rollback chega ao índice, e o módulo de solicitação não conhece o Elastic.
- Se o Elastic falhar, a requisição do cliente não é afetada (consistência eventual). Na inicialização, o índice é **reconstruído a partir do banco**.
- Campos de texto usam o analisador `brazilian` (busca por "instalacao" encontra "Instalação"); filtros exatos usam `keyword`.
- **A UF é sempre forçada** para a cobertura do analista: se ele pedir uma UF que não cobre, recebe uma lista vazia.
- Rascunhos são indexados (o enunciado pede indexação ao criar e ao salvar etapas), mas nunca aparecem na busca da análise.
- A ordenação aceita apenas uma lista fechada de campos; o `dateTo` é inclusivo e considera o fuso de São Paulo.

## Tratamento de erros

Todas as respostas de erro seguem o mesmo formato (`ApiError`), definido num único `@RestControllerAdvice`. As exceções descrevem o problema em termos de negócio, e só o handler as traduz para HTTP:

| Status | Quando |
|---|---|
| 400 | Formato inválido (Bean Validation, JSON malformado, parâmetros inválidos) |
| 401 | Sem token, token inválido/expirado, credenciais incorretas |
| 403 | Papel sem permissão para a rota |
| 404 | Recurso inexistente ou sem acesso (dono/UF) |
| 409 | Conflito com o estado atual (e-mail já cadastrado, ação não permitida no status) |
| 422 | Regra de negócio não atendida (envio incompleto, CEP inexistente, etapa fora de ordem) |
| 503 | Serviço externo indisponível (ViaCEP) |

Os erros 401 e 403 acontecem nos filtros do Spring Security, antes dos controllers; por isso são redirecionados ao mesmo handler, mantendo o formato único. Erros inesperados devolvem uma mensagem genérica, com o detalhe apenas no log.

## Auditoria com AOP

A anotação `@Audit` marca as **ações críticas**: envio da solicitação, início da análise, decisão e criação de usuários internos. Um aspecto `@Around` registra quem fez, a ação, o recurso, o resultado (sucesso ou falha, com o motivo) e a duração, num log dedicado (`AUDIT`), sem misturar esse código com a regra de negócio. O aspecto roda por fora da transação (`@Order(0)`), para só registrar sucesso após o commit. Edições de rascunho e consultas não são auditadas, para que o registro não vire ruído.

## Banco de dados

O schema é versionado com **Flyway**; o Hibernate fica em modo `validate`, garantindo que as entidades correspondem às tabelas. Valores monetários usam `NUMERIC`/`BigDecimal`.
