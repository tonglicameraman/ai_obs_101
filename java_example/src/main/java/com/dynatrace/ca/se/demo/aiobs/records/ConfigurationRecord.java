package com.dynatrace.ca.se.demo.aiobs.records;

import io.vavr.control.Option;
import lombok.val;

import java.util.concurrent.atomic.AtomicBoolean;

public record ConfigurationRecord(
        String otelEndpoint,
        String aiModel,
        String embeddingModel,
        String openaiApiKey,
        Boolean isDebugEnabled
) {
    public static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static ConfigurationRecord fromEnvironment() {
        val result = new ConfigurationRecord(
                getOrDefault("OTEL_ENDPOINT", "http://127.0.0.1:4317"),
                getOrDefault("AI_MODEL", "gpt-4o-mini"),
                getOrDefault("EMBEDDING_MODEL", "text-embedding-3-large"),
                System.getenv("OPENAI_API_KEY"),
                getOrDefault("DEBUG_ENABLED", "false").equalsIgnoreCase("true")
        );

        if (! initialized.getAndSet(true) && result.isDebugEnabled()) {
            System.err.println("ConfigurationRecord is \n\n\n" + result);
        }

        return result;
    }

    private static String getOrDefault(String key, String defaultValue) {
        return Option.of(System.getenv(key))
                .getOrElse(defaultValue);
    }
}
