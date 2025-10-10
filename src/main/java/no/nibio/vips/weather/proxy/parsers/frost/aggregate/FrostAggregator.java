package no.nibio.vips.weather.proxy.parsers.frost.aggregate;

import jakarta.enterprise.context.ApplicationScoped;
import no.nibio.vips.entity.WeatherObservation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class FrostAggregator {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final String OPPDAL_STATION_ID = "63705";

    private static Map<String, FrostAggregationRule> RULES = new HashMap<>();

    public List<WeatherObservation> aggregateToHourly(List<WeatherObservation> tenMinute, String stationId) {
        if (tenMinute == null || tenMinute.isEmpty()) return List.of();

        addRules(stationId);
        Map<ZonedDateTime, Map<String, List<WeatherObservation>>> byHourAndElement =
                tenMinute.stream()
                        .collect(Collectors.groupingBy(
                                w -> toUTC(w.getTimeMeasured()).truncatedTo(ChronoUnit.HOURS),
                                Collectors.groupingBy(WeatherObservation::getElementMeasurementTypeId)));

        List<WeatherObservation> result = new ArrayList<>();

        for (Map.Entry<ZonedDateTime, Map<String, List<WeatherObservation>>> hourEntry : byHourAndElement.entrySet()) {
            Date hourDate = Date.from(hourEntry.getKey().toInstant());
            for (Map.Entry<String, List<WeatherObservation>> elementEntry : hourEntry.getValue().entrySet()) {
                List<WeatherObservation> bucket = elementEntry.getValue();
                if (bucket.isEmpty()) continue;

                String element = elementEntry.getKey();
                FrostAggregationRule rule = RULES.get(element);

                double arithmeticMean = new BigDecimal(mean(bucket))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

                double maxValue = new BigDecimal(max(bucket))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

                WeatherObservation template = bucket.get(0);

                if (rule == null) {
                    continue;
                }

                if (rule.isMean()) {
                    result.add(clone(template, rule.getMeanId(), arithmeticMean, hourDate));
                }
                if (rule.isMax()) {
                    result.add(clone(template, rule.getMaxId(), maxValue, hourDate));
                }
            }
        }
        return result;
    }

    private double mean(List<WeatherObservation> list) {
        return list.stream().mapToDouble(WeatherObservation::getValue).average().orElse(Double.NaN);
    }

    private double max(List<WeatherObservation> list) {
        return list.stream().mapToDouble(WeatherObservation::getValue).max().orElse(Double.NaN);
    }

    private WeatherObservation clone(WeatherObservation src, String elementId, double value, Date time) {
        WeatherObservation w = new WeatherObservation();
        w.setTimeMeasured(time);
        w.setLogIntervalId(src.getLogIntervalId());
        w.setElementMeasurementTypeId(elementId);
        w.setValue(value);
        return w;
    }

    private ZonedDateTime toUTC(Date date) {
        return date.toInstant().atZone(UTC);
    }

    private void addRules(String stationId) {
        RULES.put("FF",  new FrostAggregationRule(true,  false,  "FM",  null));
        RULES.put("FF2", new FrostAggregationRule(true,  false,  "FM2", null));
        RULES.put("DD",  new FrostAggregationRule(true,  false,  "DM",  null));
        RULES.put("DD2", new FrostAggregationRule(true,  false,  "DM2", null));
        if(stationId.equals(OPPDAL_STATION_ID)) {
            RULES.put("TT",  new FrostAggregationRule(true,  false,"TM",  null));
            RULES.put("UU",  new FrostAggregationRule(true,  true,   "UM",  "UX"));
        }
    }
}
