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

import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.adaptiverecognition.cloud.ARCloudException;
import com.adaptiverecognition.cloud.Request;
import com.adaptiverecognition.cloud.Result;

import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

/**
 * Base builder class for all API clients.
 *
 * @author laszlo.toth
 * @param <R> the request type
 * @param <A> the result type
 */
public abstract class CarmenCloudClientBuilder<R extends Request<?>, A extends Result> {

    /**
     * <p>
     * The endpoint URL.
     * </p>
     */
    protected final ThreadLocal<String> endpoint = new ThreadLocal<>();

    /**
     * <p>
     * The API key.
     * </p>
     */
    protected final ThreadLocal<String> apiKey = new ThreadLocal<>();

    /**
     * <p>
     * The response timeout.
     * </p>
     */
    protected final ThreadLocal<Long> responseTimeout = new ThreadLocal<>();

    /**
     * <p>
     * The retry configuration.
     * </p>
     */
    protected final ThreadLocal<RetryBackoffSpec> retry = new ThreadLocal<>();

    /**
     * <p>
     * Creates a vehicle client builder.
     * </p>
     *
     * @return the builder
     */
    public static VehicleClient.VehicleClientBuilder vehicleClientBuilder() {
        return new VehicleClient.VehicleClientBuilder();
    }

    /**
     * <p>
     * Sets the endpoint URL. Returns itself to allow chaining.
     * </p>
     *
     * @param endpoint the endpoint URL
     * @return itself
     */
    public CarmenCloudClientBuilder<R, A> endpoint(String endpoint) {
        this.endpoint.set(endpoint);
        return this;
    }

    /**
     * <p>
     * Returns the endpoint URL.
     * </p>
     *
     * @return the endpoint URL
     */
    public String endpoint() {
        return this.endpoint.get();
    }

    /**
     * <p>
     * Sets the API key. Returns itself to allow chaining.
     * </p>
     *
     * @param apiKey the API key
     * @return itself
     */
    public CarmenCloudClientBuilder<R, A> apiKey(String apiKey) {
        this.apiKey.set(apiKey);
        return this;
    }

    /**
     * <p>
     * Returns the API key.
     * </p>
     *
     * @return the API key
     */
    public String apiKey() {
        return this.apiKey.get();
    }

    /**
     * <p>
     * Sets the response timeout. Returns itself to allow chaining.
     * </p>
     *
     * @param responseTimeout the response timeout in milliseconds
     * @return itself
     */
    public CarmenCloudClientBuilder<R, A> responseTimeout(Long responseTimeout) {
        this.responseTimeout.set(responseTimeout);
        return this;
    }

    /**
     * <p>
     * Returns the response timeout.
     * </p>
     *
     * @return the response timeout in milliseconds
     */
    public Long responseTimeout() {
        return this.responseTimeout.get();
    }

    /**
     * <p>
     * Sets the retry configuration. Returns itself to allow chaining.
     * </p>
     *
     * @param retry the retry configuration
     * @return itself
     */
    public CarmenCloudClientBuilder<R, A> retry(RetryBackoffSpec retry) {
        this.retry.set(retry);
        return this;
    }

    /**
     * <p>
     * Returns the retry configuration.
     * </p>
     *
     * @return the retry configuration
     */
    public RetryBackoffSpec retry() {
        return this.retry.get();
    }

    /**
     * <p>
     * Returns the default retry configuration. Retries 3 times with 1 second delay
     * for the following exceptions:
     * </p>
     * <ul>
     * <li>{@link WebClientRequestException}</li>
     * <li>{@link ARCloudException} with status code 429 or 5xx</li>
     * </ul>
     * 
     * @return the default retry configuration
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
     * <p>
     * Builds the API client.
     * </p>
     *
     * @return the API client
     */
    public abstract CarmenCloudClient<R, A> build();

}
