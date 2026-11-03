# API de Solicitações de Atendimento

Desafio técnico backend: **Java 21 + Spring Boot 4**.

API REST para um sistema de solicitações de atendimento: o **cliente** cadastra uma solicitação em 3 etapas (podendo salvar o rascunho e continuar depois), envia, e um **analista** responsável pela UF do atendimento analisa e decide. As solicitações são indexadas no **Elasticsearch** para busca com filtros e paginação, e as ações críticas são **auditadas via AOP**.

**As decisões técnicas e o porquê de cada uma estão em [docs/DECISOES.md](docs/Decisoes.md).**

---

## Sumário

- [Como rodar](#como-rodar)
- [Credenciais e usuários](#credenciais-e-usuários)
- [Testes](#rodando-os-testes)
- [Endpoints](#endpoints)
- [Arquitetura](#arquitetura)
---

## Como rodar

**Pré-requisito:** Docker (com Docker Compose).

```bash
docker compose up -d --build
```

O comando sobe três containers: a aplicação, o PostgreSQL e o Elasticsearch. A aplicação só inicia depois que o banco e o Elastic estão saudáveis.

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

Para parar (mantendo os dados) ou zerar tudo:

```bash
docker compose down      # para os containers, mantém os volumes
docker compose down -v   # para e apaga os dados
```

### Variáveis de ambiente

Todas têm valores padrão **apenas para desenvolvimento local**. Em produção, devem ser definidas no ambiente (ou por um gerenciador de segredos).

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
| `PUT` | `/admin/users/{id}/covered-ufs` | ADMIN | Altera as UFs de cobertura de um analista |

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

**Monólito modular**, organizado por funcionalidade, com camadas dentro de cada módulo.

```
com.sea.atendimento
├── auth/            → como se entra no sistema: cadastro e login
│   ├── service/
│   └── web/ (controller + dto)
├── user/            → quem são os usuários: entidade, UFs de cobertura, criação pelo admin
│   ├── bootstrap/   → criação do admin inicial
│   ├── domain/
│   ├── infra/
│   ├── service/
│   └── web/
├── solicitation/    → o fluxo de solicitação: etapas, envio e análise
│   ├── domain/      → entidade com as regras (domínio rico) e o endereço (objeto de valor)
│   ├── infra/       → repository JPA e cliente do ViaCEP
│   ├── service/     → casos de uso do cliente e do analista, e o contrato de consulta de CEP
│   └── web/
├── search/          → busca no Elasticsearch: documento, indexação e consulta
│   ├── bootstrap/   → sincronização do índice na inicialização
│   ├── infra/
│   ├── service/
│   └── web/
└── shared/          → a base comum do sistema
    ├── audit/       → @Audit e o aspecto de auditoria
    ├── config/      → segurança, Swagger, PasswordEncoder
    ├── domain/      → núcleo compartilhado: Role e Uf
    ├── exception/   → exceções e o tratamento global de erros
    ├── security/    → JWT e filtro de autenticação
    └── utils/
```
