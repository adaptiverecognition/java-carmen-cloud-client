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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.adaptiverecognition.cloud.ARCloudException;
import com.adaptiverecognition.cloud.vehicle.Locations;
import com.adaptiverecognition.cloud.vehicle.Locations.Location;
import com.adaptiverecognition.cloud.vehicle.VehicleRequest;
import com.adaptiverecognition.cloud.vehicle.VehicleRequest.Service;
import com.adaptiverecognition.cloud.vehicle.VehicleResult;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.context.Context;
import reactor.util.retry.RetryBackoffSpec;

/**
 * The client for the Vehicle API.
 *
 * @author laszlo.toth
 */
public class VehicleClient implements CarmenCloudClient<VehicleRequest<?>, VehicleResult> {

    private static final Logger LOGGER = LogManager.getLogger(VehicleClient.class);

    private final RetryBackoffSpec retry;
    private final WebClient webClient;

    private VehicleClient(VehicleClientBuilder builder) {
        this.retry = builder.retry.get();

        HttpClient httpClient = HttpClient.create().followRedirect(true);

        Long responseTimeout = builder.responseTimeout.get();
        if (responseTimeout != null) {
            httpClient = httpClient.responseTimeout(Duration.ofMillis(responseTimeout));
        }

        this.webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(builder.endpoint.get()).defaultHeader("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .defaultHeader("X-Api-Key", builder.apiKey())
                .defaultHeader("X-Disable-Call-Statistics", String.valueOf(builder.disableCallStatistics()))
                .defaultHeader("X-Disable-Image-Resizing", String.valueOf(builder.disableImageResizing()))
                .defaultHeader("X-Enable-Wide-Range-Analysis", String.valueOf(builder.enableWideRangeAnalysis()))
                .build();
    }

    /**
     * <p>
     * Gets the available locations.
     * </p>
     *
     * @return the locations
     * @throws ARCloudException if an error occurs
     */
    public Locations getLocations() throws ARCloudException {
        return getLocations(null);
    }

    /**
     * <p>
     * Gets the available locations with a retry context.
     * </p>
     *
     * @param context the retry context
     * @return the locations
     * @throws ARCloudException if an error occurs
     */
    public Locations getLocations(Map<?, ?> context) throws ARCloudException {
        try {
            return getLocationsAsync(context).get();
        } catch (InterruptedException | ExecutionException e) {
            // Restore interrupted state...
            Thread.currentThread().interrupt();
            if (e.getCause() instanceof ARCloudException) {
                throw (ARCloudException) e.getCause();
            } else {
                throw new ARCloudException(500, e.getMessage(), e);
            }

        }
    }

    /**
     * <p>
     * Gets the available locations asynchronously.
     * </p>
     *
     * @return the locations
     */
    public CompletableFuture<Locations> getLocationsAsync() {
        return getLocationsAsync(null);
    }

    /**
     * <p>
     * Gets the available locations asynchronously with a retry context.
     * </p>
     *
     * @param context the retry context
     * @return the locations
     */
    public CompletableFuture<Locations> getLocationsAsync(Map<?, ?> context) {
        ParameterizedTypeReference<List<Location>> ptr = new ParameterizedTypeReference<>() {
        };

        Mono<Locations> result = webClient.get().uri("countries").accept(MediaType.APPLICATION_JSON).retrieve()
                .onStatus(statusCode -> statusCode.is5xxServerError(),
                        response -> response.bodyToMono(String.class).flatMap(error -> {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.log(Level.DEBUG, "5xx error occured: {} ({})", error, response.statusCode());
                            }
                            return Mono.error(new ARCloudException(response.statusCode().value(), error));
                        }))
                .bodyToMono(ptr).flatMap(locations -> Mono.just(new Locations(locations)));

        if (retry != null) {
            result = result.retryWhen(context != null ? retry.withRetryContext(Context.of(context)) : retry);
        }

        return result.toFuture();
    }

    /**
     * <p>
     * Searches for vehicles (anpr, mmr, and adr informations) based on the request.
     * </p>
     *
     * @param request the request
     * @return the result
     * @throws ARCloudException if an error occurs
     */
    @Override
    public VehicleResult search(VehicleRequest<?> request) throws ARCloudException {
        return search(request, null);
    }

    /**
     * <p>
     * Searches for vehicles (anpr, mmr, and adr informations) based on the request
     * with a retry context.
     * </p>
     *
     * @param request the request
     * @param context the retry context
     * @return the result
     * @throws ARCloudException if an error occurs
     */
    @Override
    public VehicleResult search(VehicleRequest<?> request, Map<?, ?> context) throws ARCloudException {
        try {
            return searchAsync(request, context).get();
        } catch (InterruptedException | ExecutionException e) {
            // Restore interrupted state...
            Thread.currentThread().interrupt();
            if (e.getCause() instanceof ARCloudException) {
                throw (ARCloudException) e.getCause();
            } else {
                throw new ARCloudException(500, e.getMessage(), e);
            }

        }
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<VehicleResult> searchAsync(VehicleRequest<?> request) {
        return searchAsync(request, null);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<VehicleResult> searchAsync(VehicleRequest<?> request, Map<?, ?> context) {

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        if (request.getServices() != null && !request.getServices().isEmpty()) {
            builder.part("service", String.join(",",
                    request.getServices().stream().map(Service::getValue).collect(Collectors.toList())));
        }
        if (request.getInputImage() != null) {
            builder.part("image", new ByteArrayResource(request.getInputImage().getImageSource()),
                    MediaType.parseMediaType("image/" + request.getInputImage().getImageMimeType()))
                    .filename(request.getInputImage().getImageName());
        }
        if (request.getLocation() != null) {
            builder.part("location", request.getLocation());
        }
        if (request.getRoi() != null) {
            builder.part("roi", request.getRoi());
        }
        if (request.getMaxreads() != null) {
            builder.part("maxreads", request.getMaxreads());
        }

        String region;
        if (request.getRegion() == null || request.getRegion().length() == 0) {
            region = "";
        } else if (!request.getRegion().startsWith("/")) {
            region = "/" + request.getRegion();
        } else {
            region = request.getRegion();
        }

        Mono<VehicleResult> result = webClient.post().uri(region).accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(builder.build())).retrieve()
                .onStatus(statusCode -> statusCode.is4xxClientError(),
                        response -> response.bodyToMono(String.class).flatMap(error -> {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.log(Level.DEBUG, "4xx error occured: {} ({})", error, response.statusCode());
                            }
                            return Mono.error(new ARCloudException(response.statusCode().value(), error));
                        }))
                .onStatus(statusCode -> statusCode.is5xxServerError(),
                        response -> response.bodyToMono(String.class).flatMap(error -> {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.log(Level.DEBUG, "5xx error occured: {} ({})", error, response.statusCode());
                            }
                            return Mono.error(new ARCloudException(response.statusCode().value(), error));
                        }))
                .toEntity(VehicleResult.class).flatMap(entity -> {
                    VehicleResult vr = entity.getBody();
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
     * Creates a new client builder for the Vehicle API.
     */
    public static class VehicleClientBuilder extends CarmenCloudClientBuilder<VehicleRequest<?>, VehicleResult> {

        private final ThreadLocal<Boolean> disableCallStatistics = new ThreadLocal<>();
        private final ThreadLocal<Boolean> disableImageResizing = new ThreadLocal<>();
        private final ThreadLocal<Boolean> enableWideRangeAnalysis = new ThreadLocal<>();

        /**
         * Default constructor.
         */
        public VehicleClientBuilder() {
            this.disableCallStatistics.set(false);
            this.disableImageResizing.set(false);
            this.enableWideRangeAnalysis.set(false);
        }

        /** {@inheritDoc} */
        @Override
        public VehicleClientBuilder endpoint(String endpoint) {
            return (VehicleClientBuilder) super.endpoint(endpoint);
        }

        /** {@inheritDoc} */
        @Override
        public VehicleClientBuilder apiKey(String apiKey) {
            return (VehicleClientBuilder) super.apiKey(apiKey);
        }

        /** {@inheritDoc} */
        @Override
        public VehicleClientBuilder responseTimeout(Long responseTimeout) {
            return (VehicleClientBuilder) super.responseTimeout(responseTimeout);
        }

        /** {@inheritDoc} */
        @Override
        public VehicleClientBuilder retry(RetryBackoffSpec retry) {
            return (VehicleClientBuilder) super.retry(retry);
        }

        /**
         * <p>
         * Sets if call statistics should be disabled. Default is false.
         * </p>
         *
         * @param disableCallStatistics if call statistics should be disabled
         * @return the builder
         */
        public VehicleClientBuilder disableCallStatistics(boolean disableCallStatistics) {
            this.disableCallStatistics.set(disableCallStatistics);
            return this;
        }

        /**
         * <p>
         * Returns if call statistics are disabled.
         * </p>
         *
         * @return if call statistics are disabled
         */
        public boolean disableCallStatistics() {
            return this.disableCallStatistics.get();
        }

        /**
         * <p>
         * Sets if image resizing should be disabled. Default is false.
         * </p>
         *
         * @param disableImageResizing if image resizing should be disabled
         * @return the builder
         */
        public VehicleClientBuilder disableImageResizing(boolean disableImageResizing) {
            this.disableImageResizing.set(disableImageResizing);
            return this;
        }

        /**
         * <p>
         * Returns if image resizing is disabled.
         * </p>
         *
         * @return if image resizing is disabled
         */
        public boolean disableImageResizing() {
            return this.disableImageResizing.get();
        }

        /**
         * <p>
         * Sets if wide range analysis should be enabled. Default is false.
         * </p>
         *
         * @param enableWideRangeAnalysis if wide range analysis should be enabled
         * @return the builder
         */
        public VehicleClientBuilder enableWideRangeAnalysis(boolean enableWideRangeAnalysis) {
            this.enableWideRangeAnalysis.set(enableWideRangeAnalysis);
            return this;
        }

        /**
         * <p>
         * Returns if wide range analysis is enabled.
         * </p>
         *
         * @return if wide range analysis is enabled
         */
        public boolean enableWideRangeAnalysis() {
            return this.enableWideRangeAnalysis.get();
        }

        /** {@inheritDoc} */
        @Override
        public VehicleClient build() {
            return new VehicleClient(this);
        }
    }

}
