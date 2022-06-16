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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.adaptiverecognition.cloud.vehicle.VehicleRequest;
import com.adaptiverecognition.cloud.vehicle.VehicleResult;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.context.Context;
import reactor.util.retry.RetryBackoffSpec;

/**
 *
 * @author laszlo.toth
 */
public class VehicleClient implements ARCloudClient<VehicleRequest, VehicleResult> {

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
                .defaultHeader("X-Api-Key", builder.apiKey.get())
                .build();
    }

    /**
     *
     * @param request
     * @return
     * @throws ARCloudException
     */
    @Override
    public VehicleResult process(VehicleRequest request) throws ARCloudException {
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
    public VehicleResult process(VehicleRequest request, Map context) throws ARCloudException {
        try {
            return processAsync(request, context).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ARCloudException(500, e.getMessage(), e);
        }
    }

    /**
     *
     * @param request
     * @return
     */
    @Override
    public CompletableFuture<VehicleResult> processAsync(VehicleRequest request) throws ARCloudException {
        return processAsync(request, null);
    }

    /**
     *
     * @param request
     * @return
     */
    @Override
    public CompletableFuture<VehicleResult> processAsync(VehicleRequest request, Map context) throws ARCloudException {

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        if (request.getServices() != null && !request.getServices().isEmpty()) {
            builder.part("service", String.join(",",
                    (List<String>) request.getServices().stream().map(s -> s.getValue()).collect(Collectors.toList())));
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
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    return response.bodyToMono(String.class).flatMap(error -> {
                        return Mono.error(new ARCloudException(response.statusCode().value(), error));
                    });
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    return response.bodyToMono(String.class).flatMap(error -> {
                        return Mono.error(new ARCloudException(response.statusCode().value(), error));
                    });
                })
                .bodyToMono(VehicleResult.class);

        if (retry != null) {
            result = result.retryWhen(context != null ? retry.withRetryContext(Context.of(context)) : retry);
        }

        return result.toFuture();
    }

    /**
     *
     */
    public static class VehicleClientBuilder extends ARCloudClientBuilder<VehicleClientBuilder, VehicleClient> {

        /**
         *
         * @return
         */
        @Override
        public VehicleClient build() {
            return new VehicleClient(this);
        }
    }

}