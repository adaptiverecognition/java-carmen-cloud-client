/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.adaptiverecognition.cloud.client;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

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
public class VehicleClient implements ARCloudClient<VehicleRequest<VehicleRequest.Service>, VehicleResult> {

    private final RetryBackoffSpec retry;
    private final WebClient webClient;

    private VehicleClient(VehicleClientBuilder builder) {
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
                .defaultHeader("X-Disable-Call-Statistics", String.valueOf(builder.disableCallStatistics()))
                .defaultHeader("X-Disable-Image-Resizing", String.valueOf(builder.disableImageResizing()))
                .defaultHeader("X-Enable-Wide-Range-Analysis", String.valueOf(builder.enableWideRangeAnalysis()))
                .build();
    }

    /**
     *
     * @param request
     * @return
     * @throws ARCloudException
     */
    @Override
    public VehicleResult process(VehicleRequest<VehicleRequest.Service> request) throws ARCloudException {
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
    public VehicleResult process(VehicleRequest<VehicleRequest.Service> request, Map<?, ?> context)
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
    public CompletableFuture<VehicleResult> processAsync(VehicleRequest<VehicleRequest.Service> request)
            throws ARCloudException {
        return processAsync(request, null);
    }

    /**
     *
     * @param request
     * @return
     */
    @Override
    public CompletableFuture<VehicleResult> processAsync(VehicleRequest<VehicleRequest.Service> request,
            Map<?, ?> context)
            throws ARCloudException {

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        if (request.getServices() != null && !request.getServices().isEmpty()) {
            builder.part("service", String.join(",",
                    request.getServices().stream().map(Service::getValue).collect(Collectors.toList())));
        }
        if (request.getImageSource() != null) {
            builder.part("image", new ByteArrayResource(request.getImageSource()),
                    MediaType.parseMediaType("image/" + request.getImageMimeType()))
                    .filename(request.getImageName());
        }
        if (request.getLocation() != null) {
            builder.part("location", request.getLocation());
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

        Mono<VehicleResult> result = webClient.post()
                .uri(region)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError,
                        response -> response.bodyToMono(String.class).flatMap(
                                error -> Mono.error(new ARCloudException(response.statusCode().value(), error))))
                .onStatus(HttpStatus::is5xxServerError,
                        response -> response.bodyToMono(String.class).flatMap(
                                error -> Mono.error(new ARCloudException(response.statusCode().value(), error))))
                .bodyToMono(VehicleResult.class);

        if (retry != null) {
            result = result.retryWhen(context != null ? retry.withRetryContext(Context.of(context)) : retry);
        }

        return result.toFuture();
    }

    /**
     *
     */
    public static class VehicleClientBuilder
            extends ARCloudClientBuilder<VehicleRequest<VehicleRequest.Service>, VehicleResult> {

        @Override
        public VehicleClientBuilder endpoint(String endpoint) {
            return (VehicleClientBuilder) super.endpoint(endpoint);
        }

        @Override
        public VehicleClientBuilder apiKey(String apiKey) {
            return (VehicleClientBuilder) super.apiKey(apiKey);
        }

        @Override
        public VehicleClientBuilder disableCallStatistics(boolean disableCallStatistics) {
            return (VehicleClientBuilder) super.disableCallStatistics(disableCallStatistics);
        }

        @Override
        public VehicleClientBuilder disableImageResizing(boolean disableImageResizing) {
            return (VehicleClientBuilder) super.disableImageResizing(disableImageResizing);
        }

        @Override
        public VehicleClientBuilder enableWideRangeAnalysis(boolean enableWideRangeAnalysis) {
            return (VehicleClientBuilder) super.enableWideRangeAnalysis(enableWideRangeAnalysis);
        }

        @Override
        public VehicleClientBuilder responseTimeout(Long responseTimeout) {
            return (VehicleClientBuilder) super.responseTimeout(responseTimeout);
        }

        @Override
        public VehicleClientBuilder retry(RetryBackoffSpec retry) {
            return (VehicleClientBuilder) super.retry(retry);
        }

        @Override
        public VehicleClient build() {
            return new VehicleClient(this);
        }
    }

}
