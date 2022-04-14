/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.adaptiverecognition.cloud.client;

import com.adaptiverecognition.cloud.Request;
import com.adaptiverecognition.cloud.Result;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author laszlo.toth
 * @param <R>
 * @param <A>
 */
public interface ARCloudClient<R extends Request, A extends Result> {

    /**
     *
     * @param request
     * @return
     * @throws ARCloudException
     */
    public A process(R request) throws ARCloudException;

    /**
     *
     * @param request
     * @param context
     * @return
     * @throws ARCloudException
     */
    public A process(R request, Map context) throws ARCloudException;

    /**
     *
     * @param request
     * @return
     * @throws com.adaptiverecognition.cloud.client.ARCloudException
     */
    public CompletableFuture<A> processAsync(R request) throws ARCloudException;

    /**
     *
     * @param request
     * @param context
     * @return
     * @throws com.adaptiverecognition.cloud.client.ARCloudException
     */
    public CompletableFuture<A> processAsync(R request, Map context) throws ARCloudException;
}
