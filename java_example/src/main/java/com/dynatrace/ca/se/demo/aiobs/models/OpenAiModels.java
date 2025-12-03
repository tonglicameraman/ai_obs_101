package com.dynatrace.ca.se.demo.aiobs.models;

import com.dynatrace.ca.se.demo.aiobs.openllmetry.EmbeddingLoggingModel;
import com.dynatrace.ca.se.demo.aiobs.openllmetry.OpenLLMetryOpenAiListener;
import com.dynatrace.ca.se.demo.aiobs.records.ConfigurationRecord;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAiModels {
    private final ConfigurationRecord configuration;
    private final OpenLLMetryOpenAiListener openLLMetryOpenAiListener;
    private final Tracer tracer;

    @Autowired
    public OpenAiModels(ConfigurationRecord configuration, OpenLLMetryOpenAiListener openLLMetryOpenAiListener, Tracer tracer) {
        this.openLLMetryOpenAiListener = openLLMetryOpenAiListener;
        this.configuration = configuration;
        this.tracer = tracer;
    }

    @Bean
    public ChatModel buildChatModel() {
        return OpenAiChatModel.builder()
                .listeners(List.of(openLLMetryOpenAiListener))
                .apiKey(configuration.openaiApiKey())
                .modelName(configuration.aiModel())
                .build();
    }

    @Bean
    public EmbeddingModel buildEmbeddingModel() {
        return new EmbeddingLoggingModel(
                OpenAiEmbeddingModel.builder()
                        .apiKey(configuration.openaiApiKey())
                        .modelName(configuration.embeddingModel())
                        .build(),
                tracer);
    }
}
