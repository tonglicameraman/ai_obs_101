package com.dynatrace.ca.se.demo.aiobs.controller;

import com.dynatrace.ca.se.demo.aiobs.openllmetry.OpenLLMetryOpenAiListener;
import com.dynatrace.ca.se.demo.aiobs.records.CompletionResponse;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@Slf4j
public class AiObsController {
    private final Tracer tracer;
    private final ChatModel chatModel;
    private final Assistant assistant;

    @Autowired
    public AiObsController(Tracer tracer, ChatModel chatModel, EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel, OpenLLMetryOpenAiListener openLLMetryOpenAiListener) {
        this.tracer = tracer;

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();

        this.chatModel = chatModel;
        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();
    }

    private ResponseEntity<CompletionResponse> formatChatResponse(String response) {
        Span span = tracer.spanBuilder("AIObsController.formatChatResponse(String)")
                .startSpan();
        log.debug("Response from chat model: {}", response);
        val traceIdLog = span.getSpanContext().getTraceId();
        span.end();
        return ResponseEntity.ok(new CompletionResponse(response, Optional.ofNullable(traceIdLog)));
    }

    @GetMapping("/api/v1/thumbsUp")
    public void thumbsUp(String prompt, Optional<String> traceId) {
        Span span = tracer.spanBuilder("AiObsController.thumbsUp(String, Optional<String>)")
                .startSpan();
        val traceIdLog = traceId.map(s -> " - Trace ID: " + s).orElse("");
        log.info("Positive user feedback for search term: '{}' - {}", prompt, traceIdLog);
        span.end();
    }

    @GetMapping("/api/v1/thumbsDown")
    public void thumbsDown(String prompt, Optional<String> traceId) {
        Span span = tracer.spanBuilder("AiObsController.thumbsDown(String, Optional<String>)")
                .startSpan();
        val traceIdLog = traceId.map(s -> " Trace ID: " + s).orElse("");
        log.info("Negative user feedback for search term: '{}'{}", prompt, traceIdLog);
        span.end();
    }

    @GetMapping("/api/v1/completion")
    public ResponseEntity<CompletionResponse> submitCompletion(String framework, String prompt) {
        // Depending on the framework, you would call the appropriate method
        if ("llm".equals(framework)) {
            return formatChatResponse(llmChat(prompt));
        } else if ("rag".equals(framework)) {
            return formatChatResponse(ragChat(prompt));
        } else {
            Span span = tracer.spanBuilder("AiObsController.submitCompletion(String, String)")
                    .startSpan();
            span.setStatus(StatusCode.ERROR);
            val message = String.format("Invalid Mode %s", framework);
            span.setAttribute("error.type", message);

            log.warn(message);
            val result = ResponseEntity.status(404).body(new CompletionResponse(message, Optional.empty()));
            span.end();
            return result;
        }
    }

    private String llmChat(String location) {
        Span span = tracer.spanBuilder("AiObsController.llmChat(String)")
                .startSpan();
        val prompt = String.format("Give travel advice in a paragraph of max 50 words about %s", location);
        span.setAttribute(OpenLLMetryOpenAiListener.PROMPT_CONTENT_KEY, prompt);
        val result = chatModel.chat(prompt);
        span.end();
        return result;
    }

    private String ragChat(String location) {
        Span span = tracer.spanBuilder("AiObsController.ragChat(String)")
                .startSpan();
        val prompt = String.format("Give travel advice in a paragraph of max 50 words about %s", location);
        span.setAttribute(OpenLLMetryOpenAiListener.PROMPT_CONTENT_KEY, prompt);
        val result = assistant.chat(prompt);
        span.end();
        return result;
    }

    interface Assistant {
        String chat(String userMessage);
    }
}
