package com.dynatrace.ca.se.demo.aiobs.records;

import java.util.Optional;

public record CompletionResponse(String message, Optional<String> traceId) {
}
