/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.adaptiverecognition.cloud.client;

import java.time.Duration;

import org.springframework.web.reactive.function.client.WebClientRequestException;

import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

/**
 *
 * @author laszlo.toth
 * @param <B>
 * @param <C>
 */
public abstract class ARCloudClientBuilder<B extends ARCloudClientBuilder, C extends ARCloudClient> {

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
    protected final ThreadLocal<Boolean> disableCallStatistics = new ThreadLocal<>() {
        {
            set(false);
        }
    };

    /**
     *
     */
    protected final ThreadLocal<Boolean> disableImageResizing = new ThreadLocal<>() {
        {
            set(false);
        }
    };;

    /**
     *
     */
    protected final ThreadLocal<Boolean> enableWideRangeAnalysis = new ThreadLocal<>() {
        {
            set(false);
        }
    };;

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
     */
    protected ARCloudClientBuilder() {
    }

    /**
     *
     * @return
     */
    public static VehicleClient.VehicleClientBuilder anprClientBuilder() {
        return new VehicleClient.VehicleClientBuilder();
    }

    /**
     *
     * @param endpoint
     * @return
     */
    public B endpoint(String endpoint) {
        this.endpoint.set(endpoint);
        return (B) this;
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
    public B apiKey(String apiKey) {
        this.apiKey.set(apiKey);
        return (B) this;
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
     * @param disableCallStatistics
     * @return
     */
    public B disableCallStatistics(boolean disableCallStatistics) {
        this.disableCallStatistics.set(disableCallStatistics);
        return (B) this;
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
    public B disableImageResizing(boolean disableImageResizing) {
        this.disableImageResizing.set(disableImageResizing);
        return (B) this;
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
    public B enableWideRangeAnalysis(boolean enableWideRangeAnalysis) {
        this.enableWideRangeAnalysis.set(enableWideRangeAnalysis);
        return (B) this;
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
     * @param responseTimeout
     * @return
     */
    public B responseTimeout(Long responseTimeout) {
        this.responseTimeout.set(responseTimeout);
        return (B) this;
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
    public B retry(RetryBackoffSpec retry) {
        this.retry.set(retry);
        return (B) this;
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
    public abstract C build();

}
