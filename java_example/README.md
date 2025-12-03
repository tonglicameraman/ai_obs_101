# AI Obs Demo

This project is a basic port of its [Python-based brother](https://github.com/Dynatrace/obslab-llm-observability) to Java

## How to start the various components

### Start the AI Observability Server

```bash
#!/usr/bin/bash
export PINECONE_API_KEY=***** 
export OTEL_ENDPOINT=http://127.0.0.1:4317
export API_TOKEN=*****
export OPENAI_API_KEY=*****
clear && ./mvnw clean spring-boot:run
```

### Start the OTEL Collector

:warning: By default, the app is sending data using the OTLP gRPC protocol. A collector is needed to forward the data to
Dynatrace. The collector can be run in a Docker container.

```bash
docker run \
   -v $(pwd)/otel-collector-config.yaml:/etc/otelcol/otel-collector-config.yaml \
   -p 4317:4317 \
   -p 4318:4318 \
   ghcr.io/dynatrace/dynatrace-otel-collector/dynatrace-otel-collector:0.32.0 --config=/etc/otelcol/otel-collector-config.yaml
```

#### Content of the otel-collector-config.yaml

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  cumulativetodelta:

exporters:
  otlphttp:
    endpoint: "https://*****.sprint.dynatracelabs.com/api/v2/otlp"
    headers:
      Authorization: "Api-Token *****"

service:
  pipelines:
    traces:
      receivers: [ otlp ]
      processors: [ ]
      exporters: [ otlphttp ]
    metrics:
      receivers: [ otlp ]
      processors: [ cumulativetodelta ]
      exporters: [ otlphttp ]
    logs:
      receivers: [ otlp ]
      processors: [ ]
      exporters: [ otlphttp ]
```

### How to create load?

In the `load-generator` folder, you can run:

```bash
sbt Gatling/test
```

You can configure the following values:

| Environment values | Default        | Description                               |
|--------------------|----------------|-------------------------------------------|
| TEST_DURATION      | 1              | How long should the test run (in minutes) |
| TARGET_SERVER      | localhost:8080 | Server to call for the test               |
