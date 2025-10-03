package no.nibio.vips.weather.proxy.parsers.frost.client;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NoArgsConstructor;
import no.nibio.vips.weather.proxy.parsers.frost.ApiClient;
import no.nibio.vips.weather.proxy.parsers.frost.ApiException;
import no.nibio.vips.weather.proxy.parsers.frost.api.DefaultApi;
import no.nibio.vips.weather.proxy.parsers.frost.model.FrostResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@NoArgsConstructor
public class FrostObservationsClient {

    @ConfigProperty(name = "frost.base-url", defaultValue = "https://frost-beta.met.no")
    String baseUrl;

    @ConfigProperty(name = "frost.client-id")
    String clientId;

    @ConfigProperty(name = "frost.client-secret")
    String clientSecret;

    private DefaultApi api;

    @PostConstruct
    void initApi() {
        if (this.api == null) {
            ApiClient client = new ApiClient();
            client.setBasePath(baseUrl);
            client.setUsername(clientId);
            client.setPassword(clientSecret);
            this.api = new DefaultApi(client);
        }
    }

    public FrostResponse getObservations(String stationIds, String elementIds, String time, String timeResolutions, boolean includeObservations) {
        try {
            return api
                    .apiV1ObsMetNoKvkafkaGetGet(stationIds,time,timeResolutions)
                    .elementids(elementIds)
                    .incobs(includeObservations)
                    .levels("all")
                    .execute();
        } catch (ApiException e) {
            throw new RuntimeException("Fetch failed: HTTP " + e.getCode() + " " + e.getMessage(), e);
        }
    }
}
