package no.nibio.vips.weather.proxy.testconfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class MockApiServer implements QuarkusTestResourceLifecycleManager {
    
    private WireMockServer wireMockServer;
    
    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));
        wireMockServer.start();
        try {
            setupFrostApiMocks();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return Map.of();
    }
    
    @Override
    public void stop() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
    
    private void setupFrostApiMocks() throws IOException {
        wireMockServer.stubFor(get(urlMatching("/api/v1/obs/met.no/filter/get.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(loadTestJson("frost-api-response-1-station-pt1h.json"))));
    }
    
    private String loadTestJson(String filename) throws IOException {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test-data/" + filename);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
