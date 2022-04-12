/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.adaptiverecognition.cloud.client;

/**
 *
 * @author laszlo.toth
 * @param <C>
 */
public abstract class ARCloudClientBuilder<C extends ARCloudClient> {

    protected ThreadLocal<String> endpoint;

    protected ThreadLocal<String> apiKey;

    protected ARCloudClientBuilder() {

    }

    public static ARCloudClientBuilder anprClientBuilder() {
        return new AnprClient.AnprClientBuilder();
    }

    public ARCloudClientBuilder endpoint(String endpoint) {
        this.endpoint.set(endpoint);
        return this;
    }

    public ARCloudClientBuilder apiKey(String apiKey) {
        this.apiKey.set(apiKey);
        return this;
    }

    public abstract C build();

}
