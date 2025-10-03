package no.nibio.vips.weather.proxy.parsers.frost.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import no.nibio.vips.entity.WeatherObservation;
import no.nibio.vips.weather.proxy.parsers.ParseWeatherDataException;
import no.nibio.vips.weather.proxy.parsers.TimeSpecBuilder;
import no.nibio.vips.weather.proxy.parsers.frost.aggregate.FrostAggregator;
import no.nibio.vips.weather.proxy.parsers.frost.client.FrostObservationsClient;
import no.nibio.vips.weather.proxy.parsers.frost.map.DatasetWeatherObservationMapper;
import no.nibio.vips.weather.proxy.parsers.frost.model.FrostResponse;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

@ApplicationScoped
@NoArgsConstructor
public class FrostWeatherObservationService {

    @Inject
    FrostObservationsClient client;

    @Inject
    DatasetWeatherObservationMapper mapper;

    @Inject
    FrostAggregator aggregator;

    public List<WeatherObservation> getWeatherObservations(
            String stationId,
            ZonedDateTime start,
            ZonedDateTime end,
            String[] elementIds,
            boolean enable10MinAggregation) throws ParseWeatherDataException {

        String timeSpec = new TimeSpecBuilder()
                .start(start)
                .end(end)
                .build();

        String elementParam = join(elementIds);
        FrostResponse frostResponse;
        try {
            String timeResolutions = enable10MinAggregation ? "PT10M" : "PT1H";
            frostResponse = client.getObservations(stationId, elementParam, timeSpec, timeResolutions, true);
        } catch (RuntimeException e) {
            throw new ParseWeatherDataException("Frost v2 fetch failed: " + e.getMessage());
        }

        List<WeatherObservation> observations = mapper.map(frostResponse);

        if (enable10MinAggregation) {
            observations = aggregator.aggregateToHourly(observations, stationId);
        }
        return observations;
    }

    private String join(String[] arr) {
        StringJoiner sj = new StringJoiner(",");
        Arrays.stream(arr).forEach(sj::add);
        return sj.toString();
    }
}
