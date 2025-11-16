#!/bin/bash

# Set environment variables
export PINECONE_API_KEY=<YOUR_PINECONE_API_KEY>
export OTEL_ENDPOINT=https://<YOUR_DYNATRACE_TENANT>.dynatracelabs.com/api/v2/otlp
export API_TOKEN=<YOUR_DYNATRACE_API_TOKEN>
export OLLAMA_ENDPOINT="http://host.docker.internal:11434" # Pointing to local Ollama instance, adjust if necessary

docker build -t ai_obs_101 . && docker run --rm \
  -e PINECONE_API_KEY \
  -e OTEL_ENDPOINT \
  -e API_TOKEN \
  -e OLLAMA_ENDPOINT \
  -p 8080:8080 \
  ai_obs_101:latest
