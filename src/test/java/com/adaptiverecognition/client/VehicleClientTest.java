package com.adaptiverecognition.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.adaptiverecognition.cloud.CarmenCloudException;
import com.adaptiverecognition.cloud.client.VehicleClient;
import com.adaptiverecognition.cloud.client.VehicleClient.VehicleClientBuilder;
import com.adaptiverecognition.cloud.vehicle.VehicleRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class VehicleClientTest {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Create the test case
     *
     */
    public VehicleClientTest() {
    }

    /**
     */
    @Test
    public void testApp() {
        String endpoint = System.getProperty("endpoint", "https://api.carmencloud.com/vehicle");
        String region = System.getProperty("region");
        String apiKey = System.getProperty("apiKey");
        String image = System.getProperty("image");
        if (region == null || apiKey == null || image == null) {
            System.out.println("Please set the following environment variables: region, apiKey, image");
            return;
        }
        VehicleClient vehicleClient = VehicleClientBuilder.vehicleClientBuilder().endpoint(endpoint).apiKey(apiKey)
                .disableCallStatistics(true).build();
        try {
            System.out.println(vehicleClient
                    .search(new VehicleRequest().services(VehicleRequest.Service.ANPR, VehicleRequest.Service.MMR)
                            .region(region).image(Files.readAllBytes(new File(image).toPath()), "test-image", false))
                    .toString());
        } catch (IOException e) {
            System.err.println("Error occured: " + e.toString());
        } catch (CarmenCloudException e) {
            TypeToken<Map<String, String>> typeToken = new TypeToken<Map<String, String>>() {
            };
            Map<String, String> result = gson.fromJson(e.getMessage(), typeToken);
            System.err.println("Error occured: " + e.getStatusCode() + " - " + result.get("message"));
        }
    }
}
