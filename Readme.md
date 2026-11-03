# API de Solicitações de Atendimento

Desafio técnico backend: **Java 21 + Spring Boot 4**.

API REST para um sistema de solicitações de atendimento: o **cliente** cadastra uma solicitação em 3 etapas (podendo salvar o rascunho e continuar depois), envia, e um **analista** responsável pela UF do atendimento analisa e decide. As solicitações são indexadas no **Elasticsearch** para busca com filtros e paginação, e as ações críticas são **auditadas via AOP**.

---

## Sumário

- [Como rodar](#como-rodar)
- [Credenciais e usuários](#credenciais-e-usuários)
- [Rodando os testes](#rodando-os-testes)
- [Endpoints](#endpoints)
- [Arquitetura](#arquitetura)
- [Decisões técnicas](#decisões-técnicas)
- [Interpretações do enunciado](#interpretações-do-enunciado)
- [Melhorias futuras](#melhorias-futuras)

---

## Como rodar

**Pré-requisito:** Docker (com Docker Compose).

```bash
docker compose up -d --build
```

O comando sobe três containers: a aplicação, o PostgreSQL e o Elasticsearch. A aplicação só inicia depois que o banco e o Elastic estão saudáveis (healthchecks).

| Serviço | Endereço |
|---|---|
| API | http://localhost:8080 |
| Swagger (documentação interativa) | http://localhost:8080/swagger-ui.html |
| PostgreSQL (acesso pelo host) | `localhost:5433` |
| Elasticsearch | http://localhost:9200 |

Para acompanhar o log da aplicação:

```bash
docker compose logs -f app
```

Para parar ou zerar tudo:

```bash
docker compose down      # para os containers, mantém os volumes
docker compose down -v   # para e apaga os dados
```

### Variáveis de ambiente

Todas têm valores padrão **apenas para desenvolvimento local**. Em produção, devem ser definidas no ambiente.

| Variável | Uso | Padrão (dev) |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexão com o PostgreSQL | banco local do Compose |
| `ELASTIC_URI` | Endereço do Elasticsearch | `http://localhost:9200` |
| `JWT_SECRET` | Chave de assinatura dos tokens (mín. 32 caracteres) | chave de desenvolvimento |
| `JWT_EXPIRATION` | Validade do token | `1h` |
| `ADMIN_NAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Administrador inicial | ver abaixo |
| `VIACEP_BASE_URL`, `VIACEP_TIMEOUT` | Integração com o ViaCEP | `https://viacep.com.br/ws`, `3s` |

> **Em produção, defina obrigatoriamente `JWT_SECRET` e `ADMIN_PASSWORD`.** Os valores padrão estão públicos neste repositório.

---

## Credenciais e usuários

Na primeira inicialização, um **administrador inicial** é criado automaticamente:

| E-mail | Senha |
|---|---|
| `admin@atendimento.com` | `admin12345` |

A partir dele:

- **Clientes** se cadastram por `POST /auth/register` (o papel é sempre `CLIENT`).
- **Analistas e outros admins** são criados pelo admin em `POST /admin/users`, informando as UFs de cobertura do analista.

Para usar as rotas protegidas, faça login em `POST /auth/login` e envie o token no cabeçalho `Authorization: Bearer <token>`. No Swagger, use o botão **Authorize**.

---

## Rodando os testes

```bash
./mvnw test
```

O teste de integração usa **Testcontainers** (sobe PostgreSQL e Elasticsearch descartáveis), então o Docker precisa estar em execução.

| Teste | Tipo | O que cobre |
|---|---|---|
| `SolicitationTest` | Unitário | Regras da entidade: estado inicial, ordem das etapas, regra HIGH ≥ 100, progresso que não regride, pendências por etapa no envio, edição bloqueada após envio, decisão final |
| `AnalystSolicitationServiceTest` | Unitário (Mockito) | Regras de acesso do analista: UF de cobertura, admin vendo todas as UFs, rascunhos invisíveis, publicação do evento de indexação |
| `SolicitationFlowIntegrationTest` | Integração | Fluxo completo via HTTP: cadastro, login, criação de analista, as 3 etapas, envio, análise e aprovação, além de uma verificação de permissão (403) |

---

## Endpoints

### Autenticação (públicos)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/auth/register` | Cadastra um cliente (sempre `CLIENT`) |
| `POST` | `/auth/login` | Autentica e devolve o token JWT |

### Administração (`ADMIN`)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/admin/users` | Cria um `ANALYST` (com UFs) ou um `ADMIN` |

### Cliente (`CLIENT`, apenas nas próprias solicitações)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/solicitations` | Cria o rascunho com a etapa 1 (dados básicos) |
| `GET` | `/solicitations` | Lista as solicitações do cliente |
| `GET` | `/solicitations/{id}` | Consulta uma solicitação (para retomar o preenchimento) |
| `PUT` | `/solicitations/{id}/basic-data` | Corrige a etapa 1 |
| `PUT` | `/solicitations/{id}/address` | Etapa 2: endereço (consulta o ViaCEP) |
| `PUT` | `/solicitations/{id}/confirmation` | Etapa 3: prioridade, data, valor e termos |
| `POST` | `/solicitations/{id}/submit` | Envia a solicitação (validação completa) |

### Análise (`ANALYST` e `ADMIN`, respeitando a cobertura de UF)

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/analyst/solicitations/{id}` | Consulta uma solicitação |
| `POST` | `/analyst/solicitations/{id}/start` | `SUBMITTED` → `IN_REVIEW` |
| `POST` | `/analyst/solicitations/{id}/decide` | Aprova ou rejeita, com comentário (10 a 1000 caracteres) |
| `GET` | `/analyst/solicitations/search` | Busca com filtros `q`, `status`, `serviceType`, `priority`, `state`, `dateFrom`, `dateTo`, `page`, `size`, `sort` |

A resposta da busca segue o formato `{ items, page, size, total }`.

---

## Arquitetura

**Monólito modular**, organizado por funcionalidade, com camadas inspiradas no DDD dentro de cada módulo.

```
com.sea.atendimento
├── auth/            → como se entra no sistema: cadastro e login
│   ├── service/
│   └── web/ (controller + dto)
├── user/            → quem são os usuários: entidade, papéis, UFs, criação pelo admin
│   ├── domain/
│   ├── infra/
│   ├── service/
│   └── web/
├── solicitation/    → o fluxo de solicitação: etapas, envio e análise
│   ├── domain/      → entidade com as regras (domínio rico) e o endereço (objeto de valor)
│   ├── infra/       → repository JPA e cliente do ViaCEP
│   ├── service/     → casos de uso do cliente e do analista
│   └── web/
├── search/          → busca no Elasticsearch: documento, indexação e consulta
│   ├── infra/
│   ├── service/
│   └── web/
└── shared/          → o que serve ao sistema todo
    ├── audit/       → @Audit e o aspecto de auditoria
    ├── config/      → segurança, Swagger, PasswordEncoder
    ├── exception/   → exceções e o tratamento global de erros
    ├── security/    → JWT, filtro de autenticação
    └── utils/
```

**Regra de dependência entre camadas:** `web → service → domain`, e `infra → domain`. O domínio não depende de nenhuma outra camada.

**Regra entre módulos:** `auth` e `solicitation` usam `user`; `search` escuta eventos de `solicitation`. O módulo `solicitation` não conhece o Elasticsearch.

---

## Decisões técnicas

### Domínio rico
As regras de negócio da solicitação ficam na própria entidade `Solicitation`: ela só pode ser criada por um método de fábrica, só aceita edição em `DRAFT`, controla a ordem das etapas e as transições de status (`submit`, `startReview`, `decide`). Os services apenas orquestram: buscam, verificam dono/UF e delegam. Isso permite testar as regras com testes unitários puros, sem banco nem Spring.

### Endereço como objeto de valor
O endereço é um `@Embeddable`: no Java, uma classe coesa com comportamento próprio (normalização do CEP); no banco, colunas na própria tabela `solicitations`. Ele não tem identidade nem existe sem a solicitação, então uma tabela separada seria complexidade sem ganho.

### Cadastro em múltiplas etapas
- A solicitação nasce com a etapa 1 e é salva como rascunho a cada etapa.
- As etapas seguem a ordem (a 3 exige a 2), mas qualquer etapa pode ser corrigida enquanto em `DRAFT`, sem o `currentStep` regredir.
- Cada etapa é validada ao ser salva, e **tudo é revalidado no envio** — inclusive a data preferencial, que pode ter vencido entre o salvamento e o envio.
- O envio incompleto devolve **todas** as pendências de uma vez, cada uma com a etapa e o campo.
- Validações de formato ficam nos DTOs (Bean Validation, com `trim` aplicado antes da validação); regras que envolvem mais de um campo ou o tempo (como `HIGH` ≥ 100 e a data no passado) ficam na entidade, usando o fuso `America/Sao_Paulo`.

### Integração com o ViaCEP
- A consulta é feita pela interface `CepLookup`, definida pela camada de serviço; o `ViaCepClient` é a implementação na infraestrutura. A regra de negócio não depende do fornecedor, e os testes usam uma implementação falsa.
- O ViaCEP responde `200` com `{"erro": true}` para CEP inexistente; isso é tratado como "não encontrado".
- **CEP inexistente → 422; serviço indisponível → 503.** Em ambos os casos, a etapa 2 não é concluída.
- Toda chamada tem **timeout de 3 segundos**, para que uma lentidão do ViaCEP não esgote as threads da aplicação.

### Segurança
- **JWT (HS256), API stateless.** O token carrega apenas o id e o papel do usuário. As UFs do analista não vão no token: são consultadas no banco, para que uma mudança de cobertura valha imediatamente.
- **Três níveis de controle:** papel (no `SecurityConfig`), dono e UF (nos services, porque dependem dos dados).
- **404 em vez de 403** para solicitações de outro cliente ou fora da UF do analista: não se revela que o recurso existe.
- O cadastro público não aceita o campo `role`, evitando *mass assignment*.
- O login responde a mesma mensagem para e-mail inexistente e senha errada, evitando a enumeração de usuários.
- Senhas com BCrypt; nenhum segredo real no repositório (configurações sensíveis vêm de variáveis de ambiente).

### Administrador inicial
Como o cadastro público só cria clientes e só um admin cria outros usuários, o primeiro admin é criado por um **seeder** (`ApplicationRunner`) na inicialização, apenas se ainda não existir nenhum admin. E-mail e senha vêm de variáveis de ambiente e o hash é gerado pelo próprio `PasswordEncoder` — o mesmo padrão usado pela imagem oficial do PostgreSQL. A alternativa de inserir pela migration foi descartada porque deixaria o hash fixo no repositório.

### Busca com Elasticsearch
- O **PostgreSQL é a fonte da verdade**; o Elasticsearch guarda uma cópia otimizada para busca.
- A indexação acontece por **evento**: os services publicam `SolicitationChangedEvent`, e um listener indexa **somente após o commit** (`@TransactionalEventListener(AFTER_COMMIT)`). Assim, nada que sofreu rollback chega ao índice, e o módulo de solicitação não conhece o Elastic.
- Se o Elastic falhar, a requisição do cliente não é afetada (consistência eventual). Na inicialização, o índice é **reconstruído a partir do banco**.
- Campos de texto usam o analisador `brazilian` (busca por "instalacao" encontra "Instalação"); filtros exatos usam `keyword`.
- **A UF é sempre forçada** para a cobertura do analista: se ele pedir uma UF que não cobre, recebe uma lista vazia.
- Rascunhos são indexados (o enunciado pede indexação ao criar e ao salvar etapas), mas nunca aparecem na busca da análise.
- A ordenação aceita apenas uma lista fechada de campos; o `dateTo` é inclusivo e considera o fuso de São Paulo.

### Tratamento de erros
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

### Auditoria com AOP
A anotação `@Audit` marca as **ações críticas**: envio da solicitação, início da análise, decisão e criação de usuários internos. Um aspecto `@Around` registra quem fez, a ação, o recurso, o resultado (sucesso ou falha, com o motivo) e a duração, num log dedicado (`AUDIT`), sem misturar esse código com a regra de negócio. O aspecto roda por fora da transação (`@Order(0)`), para só registrar sucesso após o commit. Edições de rascunho e consultas não são auditadas, para que o registro não vire ruído.

### Banco de dados
O schema é versionado com **Flyway**; o Hibernate fica em modo `validate`, garantindo que as entidades correspondem às tabelas. Valores monetários usam `NUMERIC`/`BigDecimal`.

---

## Interpretações do enunciado

Pontos que o enunciado deixa em aberto e como foram resolvidos:

- **O `start` é opcional:** o `decide` aceita solicitações em `SUBMITTED` ou `IN_REVIEW`.
- **Rascunhos não são visíveis para a análise**, nem na busca nem na consulta por id.
- **O admin "faz tudo"** no sentido de gestão e análise: cria usuários internos e acessa as rotas de análise em todas as UFs. As rotas do cliente são exclusivas do cliente, porque operam sobre as próprias solicitações.
- **A checagem de UF vale para todas as ações do analista** (consultar, iniciar, decidir e buscar), e não apenas para a consulta por id.
- **`dateFrom`/`dateTo` filtram pela data de criação** da solicitação.
- **A prioridade é escolhida pelo cliente**; a regra de valor mínimo para `HIGH` evita que todo pedido seja marcado como urgente.

---

## Melhorias futuras

- Persistir a auditoria numa tabela `audit_logs`, além do log.
- Expor métricas de negócio com Micrometer (por exemplo, decisões por tipo).
- *Refresh token*, para renovar o acesso sem pedir a senha a cada hora.
- Consultar o ViaCEP antes de abrir a transação, para não reservar uma conexão do banco durante a chamada externa.
- Garantir a indexação com o padrão *outbox*, em vez de depender do evento pós-commit.
- Desligar o Swagger em produção e transformar a reindexação completa num comando sob demanda.
- Limitar tentativas de login (*rate limiting*).