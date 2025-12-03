package com.dynatrace.ca.se.demo.aiobs;

import com.dynatrace.ca.se.demo.aiobs.records.ConfigurationRecord;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ServiceAttributes;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiObsConfig {
    @Bean
    public ConfigurationRecord configurationRecord() {
        return ConfigurationRecord.fromEnvironment();
    }

    @Bean
    Tracer tracer() {
        val openTelemetry = openTelemetry();
        return openTelemetry.getTracer("ai-obs-demo-java"); //TODO Make configurable
    }

    @Bean
    OpenTelemetry openTelemetry() {
        val endpoint = configurationRecord().otelEndpoint();
        val timeout = 10;

        val exporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint)
                .setTimeout(Duration.ofSeconds(timeout))
                .build();

        val resource = Resource.getDefault().merge(
                Resource.builder()
                        .put(ServiceAttributes.SERVICE_NAME, "ai-obs-demo-java") //TODO Make configurable
                        .build()
        );

        val batchSpanProcessor = BatchSpanProcessor.builder(exporter)
                .setMaxExportBatchSize(256) // Lower this if needed
                .setMaxQueueSize(2048)      // Lower this if needed
                .build();

        val tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(batchSpanProcessor)
                .setResource(resource)
                .build();

        val logExporter = OtlpGrpcLogRecordExporter.builder()
                .setEndpoint(endpoint)
                .setTimeout(Duration.ofSeconds(timeout))
                .build();

        val logProvider = SdkLoggerProvider.builder()
                .addLogRecordProcessor(
                        BatchLogRecordProcessor.builder(logExporter)
                                .setMaxExportBatchSize(256)
                                .setMaxQueueSize(2048)
                                .build()
                )
                .setResource(resource)
                .build();

        val metricExporter = OtlpGrpcMetricExporter.builder()
                .setEndpoint(endpoint)
                .setTimeout(Duration.ofSeconds(timeout))
                .build();

        val meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(
                        PeriodicMetricReader.builder(metricExporter)
                                .setInterval(Duration.ofSeconds(timeout))
                                .build()
                )
                .setResource(resource)
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setLoggerProvider(logProvider)
                .setMeterProvider(meterProvider)
                .buildAndRegisterGlobal();
    }
}
