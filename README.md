# nfe-ai-validate

Exemplo de uso: **IA dentro de uma aplicação corporativa**, não ao lado dela.

Caso de uso: receber os dados de uma NF-e, deixar o LLM identificar inconsistências
fiscais devolvendo um JSON estruturado, e a aplicação validar e decidir. Sem chatbot.

> Acompanha a série de posts sobre integrar IA em ERPs/CRMs existentes. O foco não é
> "usar IA pra escrever código", e sim construir sistemas que usam IA como parte da
> experiência — ancorada nos dados e nas regras de negócio do próprio sistema.

## Os três conceitos demonstrados

| Conceito | Onde está | O que mostra |
|---|---|---|
| **Structured output** | `domain/AnaliseFiscal` + `.entity(...)` | O LLM devolve um *record* Java tipado, não texto livre |
| **Tool / function calling** | `tool/TabelaFiscalTool` | O modelo consulta a tabela NCM **da aplicação** em vez de alucinar |
| **Guardrails** | `service/GuardrailService` | Validação determinística pré e pós LLM. A IA sugere; o **código decide** |

O `ValidacaoNfeService` é o orquestrador: `guardrail → LLM (tool + structured) → guardrail`.
O Spring Boot é o maestro; o LLM é só uma das etapas.

## Stack

- Java 21, Spring Boot 3.4.x, build com **Gradle** (wrapper)
- Spring AI 1.1.x (linha estável — a 2.0 ainda é milestone com breaking changes)
- OpenAI (`gpt-4o-mini`)

> O `gradle.properties` aponta o Gradle para um JDK 21 — o Gradle 8.14 (homologado para o
> Spring Boot 3.4) não roda sobre o Java 25. Ajuste o caminho se seu JDK 21 estiver em outro lugar.

## Como rodar

Precisa de uma `OPENAI_API_KEY`:

```bash
export OPENAI_API_KEY=sk-...
```

### Via API REST

```bash
./gradlew bootRun
```

```bash
curl -X POST http://localhost:8080/api/nfe/validar \
  -H "Content-Type: application/json" \
  -d @exemplo-nfe.json
```

### Via CLI

```bash
./gradlew bootRun --args='--spring.profiles.active=cli'
```

### Testes (não exigem API key)

Os testes cobrem os guardrails determinísticos e rodam **offline**:

```bash
./gradlew test
```

## Aviso

PoC didático. NCMs e regras fiscais são simplificados — não use como validador
fiscal de produção.
