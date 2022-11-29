/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.adaptiverecognition.cloud.client;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.adaptiverecognition.cloud.transport.TransportRequest;
import com.adaptiverecognition.cloud.transport.TransportResult;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.context.Context;
import reactor.util.retry.RetryBackoffSpec;

/**
 *
 * @author laszlo.toth
 */
public class TransportClient implements ARCloudClient<TransportRequest<?>, TransportResult> {

    private static final Logger LOGGER = LogManager.getLogger(TransportClient.class);

    private final RetryBackoffSpec retry;
    private final WebClient webClient;

    private TransportClient(TransportClientBuilder builder) {
        this.retry = builder.retry.get();

        HttpClient httpClient = HttpClient.create().followRedirect(true);

        Long responseTimeout = builder.responseTimeout.get();
        if (responseTimeout != null) {
            httpClient = httpClient.responseTimeout(Duration.ofMillis(responseTimeout));
        }

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(builder.endpoint.get())
                .defaultHeader("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .defaultHeader("X-Api-Key", builder.apiKey())
                .defaultHeader("X-Disable-Image-Resizing", String.valueOf(builder.disableImageResizing()))
                .defaultHeader("X-Enable-Wide-Range-Analysis", String.valueOf(builder.enableWideRangeAnalysis()))
                .defaultHeader("X-Disable-Checksum-Check", String.valueOf(builder.disableChecksumCheck()))
                .defaultHeader("X-Enable-Full-Us-Accr-Code", String.valueOf(builder.enableFullUsAccrCode()))
                .defaultHeader("X-Disable-Iso-Code", String.valueOf(builder.disableIsoCode()))
                .build();
    }

    /**
     *
     * @param request
     * @return
     * @throws ARCloudException
     */
    @Override
    public TransportResult process(TransportRequest<?> request) throws ARCloudException {
        return process(request, null);
    }

    /**
     *
     * @param request
     * @param context
     * @return
     * @throws ARCloudException
     */
    @Override
    public TransportResult process(
            TransportRequest<?> request, Map<?, ?> context)
            throws ARCloudException {
        try {
            return processAsync(request, context).get();
        } catch (InterruptedException | ExecutionException e) {
            // Restore interrupted state...
            Thread.currentThread().interrupt();
            throw new ARCloudException(500, e.getMessage(), e);
        }
    }

    /**
     *
     * @param request
     * @return
     */
    @Override
    public CompletableFuture<TransportResult> processAsync(
            TransportRequest<?> request)
            throws ARCloudException {
        return processAsync(request, null);
    }

    /**
     *
     * @param request
     * @return
     */
    @Override
    public CompletableFuture<TransportResult> processAsync(
            TransportRequest<?> request,
            Map<?, ?> context)
            throws ARCloudException {

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        if (request.getInputImages() != null) {
            request.getInputImages().forEach(inputImage -> builder
                    .part("image", new ByteArrayResource(inputImage.getImageSource()),
                            MediaType.parseMediaType("image/" + inputImage.getImageMimeType()))
                    .filename(inputImage.getImageName()));
        }

        String type;
        if (request.getType() == null || request.getType().length() == 0) {
            type = "";
        } else if (!request.getType().startsWith("/")) {
            type = "/" + request.getType();
        } else {
            type = request.getType();
        }

        Mono<TransportResult> result = webClient.post()
                .uri(type)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError,
                        response -> response.bodyToMono(String.class).flatMap(
                                error -> {
                                    if (LOGGER.isDebugEnabled()) {
                                        LOGGER.log(Level.DEBUG, "4xx error occured: {} ({} - {})", error,
                                                response.statusCode(),
                                                response.rawStatusCode());
                                    }
                                    return Mono.error(new ARCloudException(response.statusCode().value(), error));
                                }))
                .onStatus(HttpStatus::is5xxServerError,
                        response -> response.bodyToMono(String.class).flatMap(
                                error -> {
                                    if (LOGGER.isDebugEnabled()) {
                                        LOGGER.log(Level.DEBUG, "5xx error occured: {} ({} - {})", error,
                                                response.statusCode(),
                                                response.rawStatusCode());
                                    }
                                    return Mono.error(new ARCloudException(response.statusCode().value(), error));
                                }))
                .bodyToMono(TransportResult.class);

        if (retry != null) {
            result = result.retryWhen(context != null ? retry.withRetryContext(Context.of(context)) : retry);
        }

        return result.toFuture();
    }

    /**
     *
     */
    public static class TransportClientBuilder
            extends ARCloudClientBuilder<TransportRequest<?>, TransportResult> {

        /**
         *
         */
        private final ThreadLocal<Boolean> disableImageResizing = new ThreadLocal<>();

        /**
         *
         */
        private final ThreadLocal<Boolean> enableWideRangeAnalysis = new ThreadLocal<>();

        /**
         *
         */
        private final ThreadLocal<Boolean> disableChecksumCheck = new ThreadLocal<>();

        /**
        *
        */
        private final ThreadLocal<Boolean> enableFullUsAccrCode = new ThreadLocal<>();

        /**
         *
         */
        private final ThreadLocal<Boolean> disableIsoCode = new ThreadLocal<>();

        public TransportClientBuilder() {
            this.disableImageResizing.set(false);
            this.enableWideRangeAnalysis.set(false);
            this.disableChecksumCheck.set(false);
            this.enableFullUsAccrCode.set(false);
            this.disableIsoCode.set(false);
        }

        @Override
        public TransportClientBuilder endpoint(String endpoint) {
            return (TransportClientBuilder) super.endpoint(endpoint);
        }

        @Override
        public TransportClientBuilder apiKey(String apiKey) {
            return (TransportClientBuilder) super.apiKey(apiKey);
        }

        @Override
        public TransportClientBuilder responseTimeout(Long responseTimeout) {
            return (TransportClientBuilder) super.responseTimeout(responseTimeout);
        }

        @Override
        public TransportClientBuilder retry(RetryBackoffSpec retry) {
            return (TransportClientBuilder) super.retry(retry);
        }

        /**
         *
         * @param disableImageResizing
         * @return
         */
        public TransportClientBuilder disableImageResizing(boolean disableImageResizing) {
            this.disableImageResizing.set(disableImageResizing);
            return this;
        }

        /**
         *
         * @return
         */
        public boolean disableImageResizing() {
            return this.disableImageResizing.get();
        }

        /**
         *
         * @param enableWideRangeAnalysis
         * @return
         */
        public TransportClientBuilder enableWideRangeAnalysis(boolean enableWideRangeAnalysis) {
            this.enableWideRangeAnalysis.set(enableWideRangeAnalysis);
            return this;
        }

        /**
         *
         * @return
         */
        public boolean enableWideRangeAnalysis() {
            return this.enableWideRangeAnalysis.get();
        }

        /**
         *
         * @param disableChecksumCheck
         * @return
         */
        public TransportClientBuilder disableChecksumCheck(boolean disableChecksumCheck) {
            this.disableChecksumCheck.set(disableChecksumCheck);
            return this;
        }

        /**
         *
         * @return
         */
        public boolean disableChecksumCheck() {
            return this.disableChecksumCheck.get();
        }

        /**
         *
         * @param enableFullUsAccrCode
         * @return
         */
        public TransportClientBuilder enableFullUsAccrCode(boolean enableFullUsAccrCode) {
            this.enableFullUsAccrCode.set(enableFullUsAccrCode);
            return this;
        }

        /**
         *
         * @return
         */
        public boolean enableFullUsAccrCode() {
            return this.enableFullUsAccrCode.get();
        }

        /**
         *
         * @param disableIsoCode
         * @return
         */
        public TransportClientBuilder disableIsoCode(boolean disableIsoCode) {
            this.disableIsoCode.set(disableIsoCode);
            return this;
        }

        /**
         *
         * @return
         */
        public boolean disableIsoCode() {
            return this.disableIsoCode.get();
        }

        @Override
        public TransportClient build() {
            return new TransportClient(this);
        }
    }

}
