package no.nibio.vips.weather.proxy.integration;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import no.nibio.vips.weather.proxy.integration.setup.APIStubs;
import no.nibio.vips.weather.proxy.integration.setup.WireMockTestResource;
import no.nibio.vips.weather.proxy.parsers.frost.service.FrostWeatherObservationService;
import org.junit.jupiter.api.*;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import no.nibio.vips.entity.WeatherObservation;

@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class)
public class FrostServiceIntegrationTest {

    @Inject
    FrostWeatherObservationService service;

    @BeforeEach
    void reset() {
        WireMockTestResource.reset();
        APIStubs.frostStubPT1H();
        APIStubs.frostStubPT10M();
        APIStubs.frostStubPT10MOppdal();
    }

    @Test
    void integration_test_1_hour_elements() throws Exception {
        ZonedDateTime start = ZonedDateTime.parse("2025-10-02T00:00:00Z");
        ZonedDateTime end = ZonedDateTime.parse("2025-10-02T01:00:00Z");

        List<WeatherObservation> obs = service.getWeatherObservations(
                "58070",
                start,
                end,
                new String[]{
                        "air_temperature", "mean(air_temperature PT1H)", "min(air_temperature PT1H)",
                        "max(air_temperature PT1H)", "sum(duration_of_leaf_wetness PT1H)",
                        "mean(surface_downwelling_shortwave_flux_in_air PT1H)", "accumulated(precipitation_amount)", "sum(precipitation_amount PT1H)",
                        "sum(duration_of_precipitation PT1H)", "max(wind_speed_of_gust PT1H)", "max(wind_speed PT1H)",
                        "max_wind_speed(wind_from_direction PT1H)", "max_wind_speed(wind_from_direction_of_gust PT1H)", "wind_speed",
                        "wind_from_direction", "surface_snow_thickness", "relative_humidity",
                        "mean(relative_humidity PT1H)", "min(relative_humidity PT1H)", "max(relative_humidity PT1H)",
                        "soil_temperature", "volume_fraction_of_water_in_soil"
                },
                false
        );

        WeatherObservation rr = obs.stream().filter(o -> "RR".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);
        WeatherObservation ff = obs.stream().filter(o -> "FF".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);
        WeatherObservation dd = obs.stream().filter(o -> "DD".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);
        WeatherObservation rra = obs.stream().filter(o -> "RRA".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);

        assertAll(
                () -> assertNotNull(rr),
                () -> assertNotNull(ff),
                () -> assertNotNull(dd),
                () -> assertNotNull(rra),
                () -> assertEquals(0.0, rr.getValue()),
                () -> assertEquals(1.79999995, ff.getValue()),
                () -> assertEquals(107.0, dd.getValue()),
                () -> assertEquals(492.048004, rra.getValue())
        );
    }

    @Test
    void integration_test_10_minute_elements() throws Exception {
        ZonedDateTime start = ZonedDateTime.parse("2025-10-02T00:00:00Z");
        ZonedDateTime end = ZonedDateTime.parse("2025-10-02T01:00:00Z");

        List<WeatherObservation> obs = service.getWeatherObservations(
                "58070",
                start,
                end,
                new String[]{
                        "air_temperature",
                        "wind_speed",
                        "wind_from_direction",
                        "relative_humidity"
                },
                true
        );

        WeatherObservation fm = obs.stream().filter(o -> "FM".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);
        WeatherObservation fm2 = obs.stream().filter(o -> "FM2".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);
        WeatherObservation dm2 = obs.stream().filter(o -> "DM2".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);
        WeatherObservation dm = obs.stream().filter(o -> "DM".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);
        WeatherObservation uu = obs.stream().filter(o -> "UU".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);
        WeatherObservation tx = obs.stream().filter(o -> "TX".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);

        assertAll(
                () -> assertNotNull(fm),
                () -> assertNotNull(fm2),
                () -> assertNotNull(dm2),
                () -> assertNotNull(dm),
                () -> assertEquals(1.6833333183333332, fm.getValue()),
                () -> assertEquals(1.1833333480000001, fm2.getValue()),
                () -> assertEquals(88.83333333333333, dm2.getValue()),
                () -> assertEquals(94.83333333333333, dm.getValue()),
                () -> assertNull(uu),
                () -> assertNull(tx)
        );
    }

    @Test
    void integration_test_10_minute_elements_oppdal() throws Exception {
        ZonedDateTime start = ZonedDateTime.parse("2025-10-02T00:00:00Z");
        ZonedDateTime end = ZonedDateTime.parse("2025-10-02T01:00:00Z");

        List<WeatherObservation> obs = service.getWeatherObservations(
                "63705",
                start,
                end,
                new String[]{
                        "air_temperature",
                        "wind_speed",
                        "wind_from_direction",
                        "relative_humidity"
                },
                true
        );

        WeatherObservation fm = obs.stream().filter(o -> "FM".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);
        WeatherObservation dm = obs.stream().filter(o -> "DM".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);
        WeatherObservation tm = obs.stream().filter(o -> "TM".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);
        WeatherObservation um = obs.stream().filter(o -> "UM".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);
        WeatherObservation ux = obs.stream().filter(o -> "UX".equals(o.getElementMeasurementTypeId())).findFirst().orElse(null);

        assertAll(
            () -> assertNotNull(fm),
            () -> assertNotNull(dm),
            () -> assertNotNull(tm),
            () -> assertNotNull(um),
            () -> assertNotNull(ux),
            () -> assertEquals(1.7666666716666668, fm.getValue()), // updated expected
            () -> assertEquals(294.1666666666667, dm.getValue()), // updated expected
            () -> assertEquals(7.350000065, tm.getValue()), // updated expected
            () -> assertEquals(65.33333333333333, um.getValue()), // updated expected
            () -> assertEquals(66.0, ux.getValue()) // updated expected
        );
    }
}
