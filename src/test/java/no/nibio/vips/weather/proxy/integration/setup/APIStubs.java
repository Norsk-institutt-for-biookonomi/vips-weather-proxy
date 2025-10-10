package no.nibio.vips.weather.proxy.integration.setup;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;

public class APIStubs {

    public static void frostStubPT1H() {
        WireMockTestResource.stubGetJson(
                "/api/v1/obs/met.no/kvkafka/get",
                "test-data/frost-api-response-1-station-pt1h.json",
                b -> b
                        .withQueryParam("stationids", matching("58070"))
                        .withQueryParam("elementids", matching(".*"))
                        .withQueryParam("time", matching(".*"))
                        .withQueryParam("timeresolutions", matching("PT1H"))
                        .withQueryParam("incobs", equalTo("true"))
                        .withQueryParam("levels", equalTo("all"))
                        .withQueryParam("performancecategories", equalTo("all"))
        );
    }

    public static void frostStubPT10M() {
        WireMockTestResource.stubGetJson(
                "/api/v1/obs/met.no/kvkafka/get",
                "test-data/frost-api-response-1-station-pt10m.json",
                b -> b
                        .withQueryParam("stationids", matching("58070"))
                        .withQueryParam("elementids", matching(".*"))
                        .withQueryParam("time", matching(".*"))
                        .withQueryParam("timeresolutions", matching("PT10M"))
                        .withQueryParam("incobs", equalTo("true"))
                        .withQueryParam("levels", equalTo("all"))
                        .withQueryParam("performancecategories", equalTo("all"))
        );
    }

    public static void frostStubPT10MOppdal() {
        WireMockTestResource.stubGetJson(
                "/api/v1/obs/met.no/kvkafka/get",
                "test-data/frost-api-response-oppdal-pt10m.json",
                b -> b
                        .withQueryParam("stationids", matching("63705"))
                        .withQueryParam("elementids", matching(".*"))
                        .withQueryParam("time", matching(".*"))
                        .withQueryParam("timeresolutions", matching("PT10M"))
                        .withQueryParam("incobs", equalTo("true"))
                        .withQueryParam("levels", equalTo("all"))
                        .withQueryParam("performancecategories", equalTo("all"))
        );
    }
}
