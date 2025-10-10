package no.nibio.vips.weather.proxy.parsers.frost.map;

import jakarta.enterprise.context.ApplicationScoped;
import no.nibio.vips.entity.WeatherObservation;
import no.nibio.vips.weather.proxy.parsers.frost.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@ApplicationScoped
public class DatasetWeatherObservationMapper {

    private final Map<String, Map<Integer, String>> elementLevelMappings = FrostElementMappings.ELEMENT_LEVEL_MAPPINGS;

    public List<WeatherObservation> map(FrostResponse frostResponse) {
        if (frostResponse == null) {
            return Collections.emptyList();
        }
        List<WeatherObservation> out = new ArrayList<>();
        for (TimeSeriesItem series : frostResponse.getData().getTseries()) {
            if (series == null) continue;
            Header header = series.getHeader();

            HeaderExtra extra = header.getExtra();

            ElementInfo elementExtra = extra.getElement();

            if (elementExtra == null) continue;
            String elementId = elementExtra.getId();
            if (isBlank(elementId)) continue;

            Integer level = extractLevel(header);
            Integer sensorChannel = extractSensor(header);

            List<Observation> obsList = series.getObservations();

            for (Observation obs : obsList) {
                if (obs == null) continue;
                WeatherObservation mapped = mapSingleObservation(elementId, level, sensorChannel, obs);
                if (mapped != null) {
                    out.add(mapped);
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    private WeatherObservation mapSingleObservation(String elementId,
                                                    Integer level,
                                                    Integer sensorChannel,
                                                    Observation obs) {
        ObservationBody body = obs.getBody();

        Double value = extractValue(body);
        if (value == null || value.isNaN()) return null;

        if ("volume_fraction_of_water_in_soil".equals(elementId)) {
            value = value / 100.0;
        }

        OffsetDateTime odt = obs.getTime();

        ZonedDateTime zdt = odt.toZonedDateTime().plusHours(1);

        String vipsCode = resolveVipsCode(elementId, level);
        if (vipsCode == null) return null;

        if ("sum(duration_of_leaf_wetness PT1H)".equals(elementId)
                && "BT".equals(vipsCode)
                && sensorChannel != null
                && sensorChannel == 1) {
            vipsCode = "BTf";
        }

        WeatherObservation weatherObs = new WeatherObservation();
        weatherObs.setTimeMeasured(Date.from(zdt.toInstant()));
        weatherObs.setElementMeasurementTypeId(vipsCode);
        weatherObs.setValue(value);
        weatherObs.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
        return weatherObs;
    }

    private Double extractValue(ObservationBody body) {
        return tryParse(body.getValue());
    }

    private Double tryParse(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || "NaN".equalsIgnoreCase(t) || "-".equals(t)) return null;
        try {
            BigDecimal bd = new BigDecimal(t);
            bd = bd.setScale(2, RoundingMode.HALF_UP); // round to 2 decimals
            return bd.doubleValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resolveVipsCode(String elementId, Integer level) {
        Map<Integer, String> levels = elementLevelMappings.get(elementId);
        if (levels == null) return null;
        if (level != null) {
            String exact = levels.get(level);
            if (exact != null) return exact;
        }
        return levels.get(null);
    }

    private Integer extractLevel(Header header) {
        if (header == null) return null;
        return header.getId().getLevel();
    }

    private Integer extractSensor(Header header) {
        if (header == null) return null;
        return header.getId().getSensor();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
