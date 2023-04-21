/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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
import org.springframework.http.HttpStatus;
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
 *
 * @author laszlo.toth
 */
public class VehicleClient implements ARCloudClient<VehicleRequest<?>, VehicleResult> {

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
     *
     * @return
     * @throws ARCloudException
     */
    public Locations getLocations() throws ARCloudException {
        return getLocations(null);
    }

    /**
     *
     * @param context
     * @return
     * @throws ARCloudException
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
     *
     * @return
     */
    public CompletableFuture<Locations> getLocationsAsync() {
        return getLocationsAsync(null);
    }

    /**
     *
     * @param context
     * @return
     */
    public CompletableFuture<Locations> getLocationsAsync(Map<?, ?> context) {
        ParameterizedTypeReference<List<Location>> ptr = new ParameterizedTypeReference<>() {
        };

        Mono<Locations> result = webClient.get().uri("countries").accept(MediaType.APPLICATION_JSON).retrieve()
                .onStatus(HttpStatus::is5xxServerError, response -> response.bodyToMono(String.class).flatMap(error -> {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.log(Level.DEBUG, "5xx error occured: {} ({} - {})", error, response.statusCode(),
                                response.rawStatusCode());
                    }
                    return Mono.error(new ARCloudException(response.statusCode().value(), error));
                })).bodyToMono(ptr).flatMap(locations -> Mono.just(new Locations(locations)));

        if (retry != null) {
            result = result.retryWhen(context != null ? retry.withRetryContext(Context.of(context)) : retry);
        }

        return result.toFuture();
    }

    /**
     *
     * @param request
     * @return
     * @throws ARCloudException
     */
    @Override
    public VehicleResult search(VehicleRequest<?> request) throws ARCloudException {
        return search(request, null);
    }

    /**
     *
     * @param request
     * @param context
     * @return
     * @throws ARCloudException
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

    /**
     *
     * @param request
     * @return
     */
    @Override
    public CompletableFuture<VehicleResult> searchAsync(VehicleRequest<?> request) {
        return searchAsync(request, null);
    }

    /**
     *
     * @param request
     * @param context
     * @return
     */
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
                .onStatus(HttpStatus::is4xxClientError, response -> response.bodyToMono(String.class).flatMap(error -> {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.log(Level.DEBUG, "4xx error occured: {} ({} - {})", error, response.statusCode(),
                                response.rawStatusCode());
                    }
                    return Mono.error(new ARCloudException(response.rawStatusCode(), error));
                }))
                .onStatus(HttpStatus::is5xxServerError, response -> response.bodyToMono(String.class).flatMap(error -> {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.log(Level.DEBUG, "5xx error occured: {} ({} - {})", error, response.statusCode(),
                                response.rawStatusCode());
                    }
                    return Mono.error(new ARCloudException(response.statusCode().value(), error));
                })).toEntity(VehicleResult.class).flatMap(entity -> {
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
     *
     */
    public static class VehicleClientBuilder extends ARCloudClientBuilder<VehicleRequest<?>, VehicleResult> {

        /**
         *
         */
        private final ThreadLocal<Boolean> disableCallStatistics = new ThreadLocal<>();

        /**
         *
         */
        private final ThreadLocal<Boolean> disableImageResizing = new ThreadLocal<>();

        /**
         *
         */
        private final ThreadLocal<Boolean> enableWideRangeAnalysis = new ThreadLocal<>();

        public VehicleClientBuilder() {
            this.disableCallStatistics.set(false);
            this.disableImageResizing.set(false);
            this.enableWideRangeAnalysis.set(false);
        }

        @Override
        public VehicleClientBuilder endpoint(String endpoint) {
            return (VehicleClientBuilder) super.endpoint(endpoint);
        }

        @Override
        public VehicleClientBuilder apiKey(String apiKey) {
            return (VehicleClientBuilder) super.apiKey(apiKey);
        }

        @Override
        public VehicleClientBuilder responseTimeout(Long responseTimeout) {
            return (VehicleClientBuilder) super.responseTimeout(responseTimeout);
        }

        @Override
        public VehicleClientBuilder retry(RetryBackoffSpec retry) {
            return (VehicleClientBuilder) super.retry(retry);
        }

        /**
         *
         * @param disableCallStatistics
         * @return
         */
        public VehicleClientBuilder disableCallStatistics(boolean disableCallStatistics) {
            this.disableCallStatistics.set(disableCallStatistics);
            return this;
        }

        /**
         *
         * @return
         */
        public boolean disableCallStatistics() {
            return this.disableCallStatistics.get();
        }

        /**
         *
         * @param disableImageResizing
         * @return
         */
        public VehicleClientBuilder disableImageResizing(boolean disableImageResizing) {
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
        public VehicleClientBuilder enableWideRangeAnalysis(boolean enableWideRangeAnalysis) {
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

        @Override
        public VehicleClient build() {
            return new VehicleClient(this);
        }
    }

}
