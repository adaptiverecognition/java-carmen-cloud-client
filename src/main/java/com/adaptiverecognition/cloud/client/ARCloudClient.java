/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.adaptiverecognition.cloud.client;

import com.adaptiverecognition.cloud.Request;
import com.adaptiverecognition.cloud.Result;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author laszlo.toth
 * @param <R>
 * @param <A>
 */
public interface ARCloudClient<R extends Request, A extends Result> {

    public A process(R request) throws InterruptedException, ExecutionException;

    public CompletableFuture<A> processAsync(R request);
}
