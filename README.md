# Carmen Cloud Client
The Carmen Cloud Client provides a convenient way to leverage the features of the Adaptive Recognition's Carmen Cloud platform. With the Carmen Cloud platform, you can extract the needed information from the uploaded images, such as license plate numbers, vehicle make and model, and vehicle identification numbers (VINs). 

# Building the client
Before proceeding, install a [JDK](https://jdk.java.net/archive/) (must be Java 17 or later) and [Apache Maven](https://maven.apache.org/install.html).

Ensure `JAVA_HOME` is set correctly and the `mvn` executable is available on your PATH.

Run the following command in a terminal/console.
```bash
mvn clean install
```

This compiles the client into your local maven repository.

# Using the client

To depend on this project in Apache Maven, add the following to your pom.xml file.
```xml
<dependencies>
    <dependency>
        <groupId>com.adaptiverecognition</groupId>
        <artifactId>carmen-cloud-client</artifactId>
        <version>4.2.0</version>
    </dependency>
</dependencies>
```

For more information on managing dependencies with Maven and publishing artifacts, see:
* [https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)
* [http://central.sonatype.org/pages/ossrh-guide.html](http://central.sonatype.org/pages/ossrh-guide.html)

# Developer Guide

## Creating a client
To create a client, use the client builder. You can obtain an instance of the builder via a static factory method located in the `CarmenCloudClientBuilder` class.

```java
VehicleClient.VehicleClientBuilder vehicleClientBuilder = CarmenCloudClientBuilder.vehicleClientBuilder();
```

Or

```java
TransportClient.TransportClientBuilder transportClientBuilder = CarmenCloudClientBuilder.transportClientBuilder();
```

The builder exposes many fluent configuration methods that can be chained to configure an API client. Here's a simple example that sets a few optional configuration options and then builds the vehicle API client.
```java
VehicleClient.VehicleClientBuilder vehicleClientBuilder = CarmenCloudClientBuilder.vehicleClientBuilder();
VehicleClient client = vehicleClientBuilder
    .endpoint("https://api.cloud.adaptiverecognition.com")
    .apiKey("*****")
    .responseTimeout(10_000L)
    .retry(vehicleClientBuilder.defaultRetry())
    .disableImageResizing(true)
    .enableWideRangeAnalysis(true)
    .build();
```

## API key
An API key must be provided for the client builder. After you obtain your API key on the [https://cloud.adaptiverecognition.com](Cloud Console), it can be set via the client builder. It is recommended to treat the API key as sensitive and not hard-code it in your source code.

```java
VehicleClient client = CarmenCloudClientBuilder.vehicleClientBuilder()
    .apiKey("YOUR-API-KEY-GOES-HERE")
    .build();
```

After it is configured, the API key is sent with every request made to the API endpoint via the `X-Api-Key` header.

## Making requests
After a client is configured and created, you can make a request to the API endpoint. The request class has setters for any parameters and payload that are defined in the API. The response class exposes getters for the modeled payload.
```java
VehicleClient client = CarmenCloudClientBuilder.vehicleClientBuilder()
    .endpoint("https://api.cloud.adaptiverecognition.com")
    .apiKey("*****").build();
VehicleResult result = client.search(new VehicleRequest()
	.region("eur")
	.location("HUN")
	.services(VehicleRequest.Service.ANPR,VehicleRequest.Service.MMR)
	.image(new FileInputStream(new File("")).readAllBytes(), "test-image.jpg"));
```

## Exception Handling

The `CarmenCloudException` expose the status code the HTTP response for logging or debugging purposes.
```java
try {
    client.search(...);
} catch(CarmenCloudException e) {
    int statusCode = e.getStatusCode();
    // ...
}
```

## Retries
Out of the box, the generated client retries on throttling and server errors (HTTP status code 429 and 500). If a different retry policy is desired, a custom one can be set via the client builder.

The easiest way to create a custom retry policy is to use the `reactor.util.retry.Retry` class. It provides a declarative API to specify when and how to retry.

```java
/**
 * The policy below will retry if the cause of the failed request matches any of the exceptions
 * given OR if the HTTP response from the service has one of the provided status codes.
 */
VehicleClient vehicleClient = CarmenCloudClientBuilder.vehicleClientBuilder()
    .endpoint("https://api.cloud.adaptiverecognition.com")
    .apiKey("*****")
    .responseTimeout(10_000L)
    .retry(Retry.backoff(3 /* retry count */, Duration.ofSeconds(1)).doBeforeRetry((rs) -> {
          int retryNumber = (int) rs.totalRetriesInARow() + 1;
          // ...
        }).filter(throwable -> {
          if (throwable instanceof WebClientRequestException) {
            return true;
          }
          if (throwable instanceof CarmenCloudException) {
            int statusCode = ((CarmenCloudException) throwable).getStatusCode();
            return statusCode == 429 || statusCode >= 500;
          }
          return false;
        })).build();
```
