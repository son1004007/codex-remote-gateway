# AI Gateway

## Purpose

The AI Gateway separates application capabilities from vendor-specific APIs. Codex session control and generic model access are related but should not be the same abstraction.

## Responsibilities

- provider registration;
- model configuration;
- capability discovery;
- request normalization;
- streaming normalization;
- timeout/retry policy;
- rate-limit handling;
- error normalization;
- usage/latency telemetry;
- server-side credential resolution.

## Non-responsibilities

The gateway should not:

- decide whether a destructive shell command is safe;
- own Git workspace authorization;
- expose provider API keys to clients;
- silently switch providers where semantic differences could affect correctness.

## Provider abstraction

Illustrative Java model:

```java
public interface AiProvider {
    String id();
    ProviderCapabilities capabilities();
    AiResponse execute(AiRequest request);
    Flux<AiStreamEvent> stream(AiRequest request);
}
```

Requests should carry capability-oriented fields and an explicit provider/model selection when reproducibility matters.

## Initial providers

### OpenAI / Codex-compatible

Use for OpenAI model/API capabilities and integrations that are separate from the Codex App Server session protocol.

### Gemini

Provide a dedicated adapter so Google-specific request/response structures remain inside the provider module.

### Groq

Provide a dedicated adapter and normalize supported capabilities rather than assuming all OpenAI-compatible endpoints behave identically.

## Configuration

Example conceptual configuration:

```yaml
ai:
  providers:
    openai:
      enabled: true
      credential-ref: OPENAI_API_KEY
      default-model: configured-model
    gemini:
      enabled: false
      credential-ref: GEMINI_API_KEY
    groq:
      enabled: false
      credential-ref: GROQ_API_KEY
```

Do not commit real keys.

## Routing policy

Start with explicit routing:

```text
request -> requested provider -> requested/default model -> execute
```

Only add automatic routing after measurable requirements exist. Future policies may consider capability, latency, cost, quota, or task class, but hidden provider substitution makes debugging and reproducibility harder.

## Failure policy

Classify failures rather than returning raw vendor exceptions:

- authentication failure;
- authorization failure;
- quota/rate limit;
- invalid request;
- unsupported capability;
- provider timeout;
- transient provider failure;
- content/policy rejection;
- internal gateway failure.

Retries should be bounded and used only for retryable failures. Streaming retries require special care because partial output may already have been consumed.

## Observability

Record metadata such as:

- provider;
- model;
- capability;
- request ID;
- start/end time;
- latency;
- success/failure class;
- token/usage data when supplied;
- retry count.

Do not persist sensitive prompt or response content by default merely for metrics. Content retention should be an explicit policy.
