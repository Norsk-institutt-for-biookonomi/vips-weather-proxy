package no.nibio.vips.weather.proxy.integration.setup;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class WireMockTestResource implements QuarkusTestResourceLifecycleManager {

    private static WireMockServer server;

    public static int getPort() {
        return server.port();
    }
    public static void configure() {
        WireMock.configureFor("localhost", getPort());
    }

    public static void reset() {
        configure();
        WireMock.reset();
    }

    public static void stubGetJson(String urlPath, String classpathResource,
                                   UnaryOperator<MappingBuilder> customize) {
        configure();
        String body = readClasspathResource(classpathResource);
        MappingBuilder builder = get(urlPathEqualTo(urlPath));
        if (customize != null) {
            builder = customize.apply(builder);
        }
        stubFor(builder.willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
    }

    private static String readClasspathResource(String resourcePath) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) throw new IllegalArgumentException("Resource not found: " + resourcePath);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + resourcePath, e);
        }
    }

    @Override
    public Map<String, String> start() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        WireMock.configureFor("localhost", server.port());

        Map<String, String> cfg = new HashMap<>();
        String baseUrl = "http://localhost:" + server.port();
        cfg.put("frost.base-url", baseUrl);
        cfg.put("frost.client-id", "test-frost-client-id");
        cfg.put("frost.client-secret", "test-frost-client-secret");
        return cfg;
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }
}


