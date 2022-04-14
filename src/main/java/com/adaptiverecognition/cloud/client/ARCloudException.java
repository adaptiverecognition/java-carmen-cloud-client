/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.adaptiverecognition.cloud.client;

/**
 *
 * @author laszlo.toth
 */
public class ARCloudException extends Exception {

    private final int statusCode;

    /**
     *
     * @param statusCode
     * @param message
     */
    public ARCloudException(int statusCode, String message) {
        this(statusCode, message, null);
    }

    /**
     *
     * @param statusCode
     * @param message
     * @param cause
     */
    public ARCloudException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     *
     * @return
     */
    public int getStatusCode() {
        return this.statusCode;
    }
}
