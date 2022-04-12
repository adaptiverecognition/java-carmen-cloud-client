/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.adaptiverecognition.cloud.client;

import com.adaptiverecognition.cloud.anpr.AnprRequest;
import com.adaptiverecognition.cloud.anpr.AnprResult;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 *
 * @author laszlo.toth
 */
public class AnprClient implements ARCloudClient<AnprRequest, AnprResult> {

    private final String endpoint;
    private final String apiKey;
    private final String region;

    private AnprClient(AnprClientBuilder builder) {
        this.endpoint = builder.endpoint.get();
        this.apiKey = builder.apiKey.get();
        this.region = builder.region.get();
    }

    @Override
    public AnprResult process(AnprRequest request) throws InterruptedException, ExecutionException {
        return processAsync(request).get();
    }

    @Override
    public CompletableFuture<AnprResult> processAsync(AnprRequest request) {

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new ByteArrayResource(request.getImageSource()), MediaType.parseMediaType(request.getImageMimeType()));
        builder.part("location", request.getLocation());
        builder.part("maxreads", request.getMaxreads());
        builder.part("service", String.join(",", (List<String>) request.getServices().stream().map(s -> s.getValue())));

        WebClient webClient = WebClient.builder()
                .baseUrl(endpoint)
                .defaultHeader("X-Api-Key", apiKey)
                .build();
        return webClient.post()
                .uri(region)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(AnprResult.class)
                .toFuture();
    }

    public static class AnprClientBuilder extends ARCloudClientBuilder<AnprClient> {

        private ThreadLocal<String> region;

        public ARCloudClientBuilder region(String region) {
            this.region.set(region);
            return this;
        }

        @Override
        public AnprClient build() {
            return new AnprClient(this);
        }
    }

}
