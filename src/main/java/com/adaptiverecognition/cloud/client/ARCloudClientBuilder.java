/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.adaptiverecognition.cloud.client;

import java.time.Duration;

import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.adaptiverecognition.cloud.ARCloudException;
import com.adaptiverecognition.cloud.Request;
import com.adaptiverecognition.cloud.Result;

import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

/**
 *
 * @author laszlo.toth
 * @param <R>
 * @param <A>
 */
public abstract class ARCloudClientBuilder<R extends Request<?>, A extends Result> {

    /**
     *
     */
    protected final ThreadLocal<String> endpoint = new ThreadLocal<>();

    /**
     *
     */
    protected final ThreadLocal<String> apiKey = new ThreadLocal<>();

    /**
     *
     */
    protected final ThreadLocal<Long> responseTimeout = new ThreadLocal<>();

    /**
     *
     */
    protected final ThreadLocal<RetryBackoffSpec> retry = new ThreadLocal<>();

    /**
     *
     * @return
     */
    public static VehicleClient.VehicleClientBuilder vehicleClientBuilder() {
        return new VehicleClient.VehicleClientBuilder();
    }

    /**
     *
     * @param endpoint
     * @return
     */
    public ARCloudClientBuilder<R, A> endpoint(String endpoint) {
        this.endpoint.set(endpoint);
        return this;
    }

    /**
     *
     * @return
     */
    public String endpoint() {
        return this.endpoint.get();
    }

    /**
     *
     * @param apiKey
     * @return
     */
    public ARCloudClientBuilder<R, A> apiKey(String apiKey) {
        this.apiKey.set(apiKey);
        return this;
    }

    /**
     *
     * @return
     */
    public String apiKey() {
        return this.apiKey.get();
    }

    /**
     *
     * @param responseTimeout
     * @return
     */
    public ARCloudClientBuilder<R, A> responseTimeout(Long responseTimeout) {
        this.responseTimeout.set(responseTimeout);
        return this;
    }

    /**
     *
     * @return
     */
    public Long responseTimeout() {
        return this.responseTimeout.get();
    }

    /**
     *
     * @param retry
     * @return
     */
    public ARCloudClientBuilder<R, A> retry(RetryBackoffSpec retry) {
        this.retry.set(retry);
        return this;
    }

    /**
     *
     * @return
     */
    public RetryBackoffSpec retry() {
        return this.retry.get();
    }

    /**
     *
     * @return
     */
    public RetryBackoffSpec defaultRetry() {
        return Retry.fixedDelay(3, Duration.ofSeconds(1)).filter(throwable -> {
            if (throwable instanceof WebClientRequestException) {
                return true;
            }
            if (throwable instanceof ARCloudException) {
                int statusCode = ((ARCloudException) throwable).getStatusCode();
                return statusCode == 429 || statusCode >= 500;
            }
            return false;
        });
    }

    /**
     *
     * @return
     */
    public abstract ARCloudClient<R, A> build();

}
