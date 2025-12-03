package com.dynatrace.ca.se.demo.aiobs.openllmetry;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.vavr.collection.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class EmbeddingLoggingModel implements EmbeddingModel {
    private final EmbeddingModel embeddingModel;
    private final Tracer tracer;

    public EmbeddingLoggingModel(EmbeddingModel embeddingModel, Tracer tracer) {
        this.embeddingModel = embeddingModel;
        this.tracer = tracer;
    }

    @Override
    public Response<Embedding> embed(String text) {
        Span span = tracer
                .spanBuilder("EmbeddingLoggingModel.embed(String)")
                .startSpan();
        val result = embeddingModel.embed(text);

        logEmbedding(text, List.ofAll(result.content().vectorAsList()), span);

        span.end();
        return result;
    }

    @Override
    public Response<java.util.List<Embedding>> embedAll(java.util.List<TextSegment> list) {
        Span span = tracer
                .spanBuilder("EmbeddingLoggingModel.embed(List<TextSegment>)")
                .startSpan();
        val result = embeddingModel.embedAll(list);

        val vavrResultList = List.ofAll(result.content())
                .map(Embedding::vectorAsList)
                .map(List::ofAll);

        List.ofAll(list)
                .zip(vavrResultList)
                .forEach(tuple -> logEmbedding(tuple._1.text(), tuple._2, span));

        span.end();
        return result;
    }

    private void logEmbedding(String text, List<Float> vectorList, Span span) {
        val sanitizedText = text.replaceAll("'", "\\\\'");
        val embedding = vectorList.mkString("[", ", ", "]");

        log.info("Found embedding {} for text '{}'", embedding, sanitizedText);
    }
}
