/**
 * Cloud API Client Java reference implementation.

 * License: Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * This file is part of the Adaptive Recognition Hungary Kft. 
 * Vehicle API and Transportation&Cargo API Client Java reference implementation.
 * 
 * This software is free to use in either commercial or non-commercial applications.
 * 
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.
 * 
 * Adaptive Recognition Hungary Kft.
 * H-1023 Budapest, Alkotas u. 41. Hungary
 * Web: https://adaptiverecognition.com/contact-us/
 * 
 */
package com.adaptiverecognition.cloud.client;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.adaptiverecognition.cloud.CarmenCloudException;
import com.adaptiverecognition.cloud.transport.TransportRequest;
import com.adaptiverecognition.cloud.transport.TransportResult;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.context.Context;
import reactor.util.retry.RetryBackoffSpec;

/**
 * The client for the Transportation &amp; Cargo API.
 *
 * @author laszlo.toth
 */
public class TransportClient implements CarmenCloudClient<TransportRequest, TransportResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportClient.class);

    private final RetryBackoffSpec retry;
    private final WebClient webClient;

    private TransportClient(TransportClientBuilder builder) {
        this.retry = builder.retry.get();

        HttpClient httpClient = HttpClient.create().followRedirect(true);

        Long responseTimeout = builder.responseTimeout.get();
        if (responseTimeout != null) {
            httpClient = httpClient.responseTimeout(Duration.ofMillis(responseTimeout));
        }

        this.webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(builder.endpoint.get()).defaultHeader("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .defaultHeader("X-Api-Key", builder.apiKey())
                .defaultHeader("X-Disable-Image-Resizing", String.valueOf(builder.disableImageResizing()))
                .defaultHeader("X-Enable-Wide-Range-Analysis", String.valueOf(builder.enableWideRangeAnalysis()))
                .defaultHeader("X-Disable-Checksum-Check", String.valueOf(builder.disableChecksumCheck()))
                .defaultHeader("X-Enable-Full-Us-Accr-Code", String.valueOf(builder.enableFullUsAccrCode()))
                .defaultHeader("X-Disable-Iso-Code", String.valueOf(builder.disableIsoCode())).build();
    }

    /**
     * <p>
     * Searches for ocr codes based on the request.
     * </p>
     *
     * @param request the request
     * @return the result
     * @throws CarmenCloudException if any error occurs
     */
    @Override
    public TransportResult search(TransportRequest request) throws CarmenCloudException {
        return search(request, null);
    }

    /**
     * <p>
     * Searches for ocr codes based on the request with a retry context.
     * </p>
     *
     * @param request the request
     * @param context the retry context
     * @return the result
     * @throws CarmenCloudException if any error occurs
     */
    @Override
    public TransportResult search(TransportRequest request, Map<?, ?> context) throws CarmenCloudException {
        try {
            return searchAsync(request, context).get();
        } catch (InterruptedException | ExecutionException e) {
            // Restore interrupted state...
            Thread.currentThread().interrupt();
            throw new CarmenCloudException(500, e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<TransportResult> searchAsync(TransportRequest request) throws CarmenCloudException {
        return searchAsync(request, null);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<TransportResult> searchAsync(TransportRequest request, Map<?, ?> context)
            throws CarmenCloudException {

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        if (request.getInputImages() != null) {
            request.getInputImages()
                    .forEach(inputImage -> builder
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

        Mono<TransportResult> result = webClient.post().uri(type).accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(builder.build())).retrieve()
                .onStatus(statusCode -> statusCode.is4xxClientError(),
                        response -> response.bodyToMono(String.class).flatMap(error -> {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("4xx error occured: {} ({})", error, response.statusCode());
                            }
                            return Mono.error(new CarmenCloudException(response.statusCode().value(), error));
                        }))
                .onStatus(statusCode -> statusCode.is5xxServerError(),
                        response -> response.bodyToMono(String.class).flatMap(error -> {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("5xx error occured: {} ({})", error, response.statusCode());
                            }
                            return Mono.error(new CarmenCloudException(response.statusCode().value(), error));
                        }))
                .toEntity(TransportResult.class).flatMap(entity -> {
                    TransportResult vr = entity.getBody();
                    if (vr != null) {
                        vr.setRequestId(entity.getHeaders().getFirst("x-amzn-requestid"));
                    }
                    return Mono.justOrEmpty(vr);
                });

        if (retry != null) {
            result = result.retryWhen(context != null ? retry.withRetryContext(Context.of(context)) : retry);
        }

        return result.toFuture();
    }

    /**
     * Creates a new client builder for the Transportation &amp; Cargo API.
     */
    public static class TransportClientBuilder extends CarmenCloudClientBuilder<TransportRequest, TransportResult> {

        private final ThreadLocal<Boolean> disableImageResizing = new ThreadLocal<>();
        private final ThreadLocal<Boolean> enableWideRangeAnalysis = new ThreadLocal<>();
        private final ThreadLocal<Boolean> disableChecksumCheck = new ThreadLocal<>();
        private final ThreadLocal<Boolean> enableFullUsAccrCode = new ThreadLocal<>();
        private final ThreadLocal<Boolean> disableIsoCode = new ThreadLocal<>();

        /**
         * Default constructor.
         */
        public TransportClientBuilder() {
            this.disableImageResizing.set(false);
            this.enableWideRangeAnalysis.set(false);
            this.disableChecksumCheck.set(false);
            this.enableFullUsAccrCode.set(false);
            this.disableIsoCode.set(false);
        }

        /** {@inheritDoc} */
        @Override
        public TransportClientBuilder endpoint(String endpoint) {
            return (TransportClientBuilder) super.endpoint(endpoint);
        }

        /** {@inheritDoc} */
        @Override
        public TransportClientBuilder apiKey(String apiKey) {
            return (TransportClientBuilder) super.apiKey(apiKey);
        }

        /** {@inheritDoc} */
        @Override
        public TransportClientBuilder responseTimeout(Long responseTimeout) {
            return (TransportClientBuilder) super.responseTimeout(responseTimeout);
        }

        /** {@inheritDoc} */
        @Override
        public TransportClientBuilder retry(RetryBackoffSpec retry) {
            return (TransportClientBuilder) super.retry(retry);
        }

        /**
         * <p>
         * Sets if image resizing should be disabled or not.
         * </p>
         * 
         * @param disableImageResizing if image resizing should be disabled or not
         * @return the builder
         */
        public TransportClientBuilder disableImageResizing(boolean disableImageResizing) {
            this.disableImageResizing.set(disableImageResizing);
            return this;
        }

        /**
         * <p>
         * Returns if image resizing is disabled or not.
         * </p>
         *
         * @return if image resizing is disabled or not
         */
        public boolean disableImageResizing() {
            return this.disableImageResizing.get();
        }

        /**
         * <p>
         * Sets if wide range analysis should be enabled or not.
         * </p>
         *
         * @param enableWideRangeAnalysis if wide range analysis should be enabled or
         * @return the builder
         */
        public TransportClientBuilder enableWideRangeAnalysis(boolean enableWideRangeAnalysis) {
            this.enableWideRangeAnalysis.set(enableWideRangeAnalysis);
            return this;
        }

        /**
         * <p>
         * Returns if wide range analysis is enabled or not.
         * </p>
         *
         * @return if wide range analysis is enabled or not
         */
        public boolean enableWideRangeAnalysis() {
            return this.enableWideRangeAnalysis.get();
        }

        /**
         * <p>
         * Sets if checksum check should be disabled or not.
         * </p>
         *
         * @param disableChecksumCheck if checksum check should be disabled or not
         * @return the builder
         */
        public TransportClientBuilder disableChecksumCheck(boolean disableChecksumCheck) {
            this.disableChecksumCheck.set(disableChecksumCheck);
            return this;
        }

        /**
         * <p>
         * Returns if checksum check is disabled or not.
         * </p>
         *
         * @return if checksum check is disabled or not
         */
        public boolean disableChecksumCheck() {
            return this.disableChecksumCheck.get();
        }

        /**
         * <p>
         * Sets if full US Accr Code should be enabled or not.
         * </p>
         *
         * @param enableFullUsAccrCode if full US Accr Code should be enabled or not
         * @return the builder
         */
        public TransportClientBuilder enableFullUsAccrCode(boolean enableFullUsAccrCode) {
            this.enableFullUsAccrCode.set(enableFullUsAccrCode);
            return this;
        }

        /**
         * <p>
         * Returns if full US Accr Code is enabled or not.
         * </p>
         *
         * @return if full US Accr Code is enabled or not
         */
        public boolean enableFullUsAccrCode() {
            return this.enableFullUsAccrCode.get();
        }

        /**
         * <p>
         * Sets if ISO code should be disabled or not.
         * </p>
         *
         * @param disableIsoCode if ISO code should be disabled or not
         * @return the builder
         */
        public TransportClientBuilder disableIsoCode(boolean disableIsoCode) {
            this.disableIsoCode.set(disableIsoCode);
            return this;
        }

        /**
         * <p>
         * Returns if ISO code is disabled or not.
         * </p>
         *
         * @return if ISO code is disabled or not
         */
        public boolean disableIsoCode() {
            return this.disableIsoCode.get();
        }

        /** {@inheritDoc} */
        @Override
        public TransportClient build() {
            return new TransportClient(this);
        }
    }

}
