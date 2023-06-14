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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.adaptiverecognition.cloud.CarmenCloudException;
import com.adaptiverecognition.cloud.Request;
import com.adaptiverecognition.cloud.Result;

/**
 *
 * Base interface for all API clients.
 * 
 * @author laszlo.toth
 * @param <R> the request type
 * @param <A> the result type
 */
public interface CarmenCloudClient<R extends Request<?>, A extends Result> {

    /**
     * <p>
     * The default search function
     * </p>
     *
     * @param request the request
     * @return the result
     * @throws CarmenCloudException if the request fails
     */
    public A search(R request) throws CarmenCloudException;

    /**
     * <p>
     * Search function with retry context.
     * </p>
     *
     * @param request the request
     * @param context the retry context. See {@link reactor.util.context.Context}
     *                for more details.
     * @return the result
     * @throws CarmenCloudException if the request fails
     */
    public A search(R request, Map<?, ?> context) throws CarmenCloudException;

    /**
     * <p>
     * Asynchronous search function.
     * </p>
     *
     * @param request the request
     * @return the result
     * @throws CarmenCloudException if the request fails
     */
    public CompletableFuture<A> searchAsync(R request) throws CarmenCloudException;

    /**
     * <p>
     * Asynchronous search function with retry context.
     * </p>
     *
     * @param request the request
     * @param context the retry context. See {@link reactor.util.context.Context}
     *                for more details.
     * @return the result
     * @throws CarmenCloudException if the request fails
     */
    public CompletableFuture<A> searchAsync(R request, Map<?, ?> context) throws CarmenCloudException;
}
