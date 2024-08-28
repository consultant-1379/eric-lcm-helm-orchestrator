/*******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.management.lcm.tracing;

import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.extension.trace.jaeger.sampler.JaegerRemoteSampler;
import io.opentelemetry.sdk.extension.trace.jaeger.sampler.JaegerRemoteSamplerBuilder;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.util.PathMatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Configuration for DST Tracing.
 * <p>
 * Contains methods to configure export protocol and remote sampling strategy
 */
@Configuration
public class OtelConfiguration {

    @Value("${ericsson.tracing.exporter.endpoint}")
    private String tracingEndpoint;

    @Value("${ericsson.tracing.sampler.jaeger-remote.endpoint}")
    private String jaegerEndpoint;

    @Value("${SERVICE_ID:unknown_service}")
    private String serviceId;

    @Value("${ericsson.tracing.ca-cert}")
    private String caCert;

    @Value("${ericsson.tracing.client-cert}")
    private String clientCert;

    @Value("${ericsson.tracing.client-key}")
    private String clientKey;

    @Value("${security.tls.enabled}")
    private boolean tlsEnabled;

    @Value("${security.serviceMesh.enabled}")
    private boolean serviceMeshEnabled;

    @Bean
    @ConditionalOnExpression("${ericsson.tracing.enabled} && 'grpc'.equals('${ericsson.tracing.exporter.protocol}')")
    //You only need to provide this bean, if you want to use grpc (port 4317)
    //vs http (port 4318) channel for span export.
    //Otherwise Spring's OtlpAutoConfiguration class by default configures
    //OtlpHttpSpanExporter bean.
    public OtlpGrpcSpanExporter otlpExporterGrpc() throws IOException {
        OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder();
        setCertificatesIfRequired(builder);
        return builder
                .setEndpoint(changeUrlIfRequired(tracingEndpoint))
                .build();
    }

    @Bean
    @ConditionalOnExpression("${ericsson.tracing.enabled} && 'http'.equals('${ericsson.tracing.exporter.protocol}')")
    //You only need to provide this bean, if you want to use http (port 4318)
    //vs grpc (port 4317) channel for span export.
    //Otherwise Spring's OtlpAutoConfiguration class by default configures
    //OtlpHttpSpanExporter bean.
    public OtlpHttpSpanExporter otlpExporterHttp() throws IOException {
        OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder();
        setCertificatesIfRequired(builder);
        return builder
                .setEndpoint(changeUrlIfRequired(tracingEndpoint))
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "ericsson.tracing", name = "enabled", havingValue = "true")
    public JaegerRemoteSampler jaegerRemoteSampler() throws IOException {
        JaegerRemoteSamplerBuilder builder = JaegerRemoteSampler.builder();
        setCertificatesIfRequired(builder);
        return builder
                .setEndpoint(changeUrlIfRequired(jaegerEndpoint))
                .setPollingInterval(Duration.ofSeconds(30))
                .setInitialSampler(Sampler.alwaysOff())
                .setServiceName(serviceId)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "ericsson.tracing", name = "enabled", havingValue = "true")
    public ObservationRegistryCustomizer<ObservationRegistry> skipActuatorEndpointsFromObservation(PathMatcher pathMatcher) {
        return registry -> registry.observationConfig().observationPredicate((name, context) -> {
            if (context instanceof ServerRequestObservationContext) {
                return !pathMatcher.match("/actuator/**", ((ServerRequestObservationContext) context).getCarrier().getRequestURI());
            } else {
                return true;
            }
        });
    }

    private void setCertificatesIfRequired(OtlpGrpcSpanExporterBuilder builder) throws IOException {
        if (tlsEnabled && !serviceMeshEnabled) {
            builder.setTrustedCertificates(Files.readAllBytes(Path.of(caCert)))
                    .setClientTls(Files.readAllBytes(Path.of(clientKey)), Files.readAllBytes(Path.of(clientCert)));
        }
    }

    private void setCertificatesIfRequired(OtlpHttpSpanExporterBuilder builder) throws IOException {
        if (tlsEnabled && !serviceMeshEnabled) {
            builder.setTrustedCertificates(Files.readAllBytes(Path.of(caCert)))
                    .setClientTls(Files.readAllBytes(Path.of(clientKey)), Files.readAllBytes(Path.of(clientCert)));
        }
    }

    private void setCertificatesIfRequired(JaegerRemoteSamplerBuilder builder) throws IOException {
        if (tlsEnabled && !serviceMeshEnabled) {
            builder.setTrustedCertificates(Files.readAllBytes(Path.of(caCert)))
                    .setClientTls(Files.readAllBytes(Path.of(clientKey)), Files.readAllBytes(Path.of(clientCert)));
        }
    }

    private String getNotSecureUrlIfRequired(String url) {
        return url.contains("https://") ? url.replace("https://", "http://") : url;
    }

    private String changeUrlIfRequired(String endpoint) {
        return (tlsEnabled && !serviceMeshEnabled) ? endpoint : getNotSecureUrlIfRequired(endpoint);
    }

}
