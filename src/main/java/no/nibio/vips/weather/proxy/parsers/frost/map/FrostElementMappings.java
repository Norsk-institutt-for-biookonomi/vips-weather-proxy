package no.nibio.vips.weather.proxy.parsers.frost.map;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class FrostElementMappings {

    public static final Map<String, Map<Integer, String>> ELEMENT_LEVEL_MAPPINGS = create();

    private static Map<String, Map<Integer, String>> create() {
        Map<String, Map<Integer, String>> m = new HashMap<>();

        // Air temperature (original Python mapping)
        m.put("air_temperature", map(entry(2, "TT"), entry(null, "TT")));
        m.put("mean(air_temperature PT1H)", map(entry(null, "TM")));
        m.put("min(air_temperature PT1H)", map(entry(null, "TN")));
        m.put("max(air_temperature PT1H)", map(entry(null, "TX")));

        // Leaf wetness & radiation
        m.put("sum(duration_of_leaf_wetness PT1H)", map(entry(null, "BT")));
        m.put("mean(surface_downwelling_shortwave_flux_in_air PT1H)", map(entry(null, "Q0")));

        // Precipitation & duration
        m.put("sum(precipitation_amount PT1H)", map(entry(null, "RR")));
        m.put("accumulated(precipitation_amount)", map(entry(null, "RRA")));
        m.put("sum(duration_of_precipitation PT1H)", map(entry(null, "RRmm")));

        // Wind (mean)
        m.put("wind_speed", map(entry(2, "FF2"), entry(10, "FF"), entry(null, "FF")));
        m.put("wind_from_direction", map(entry(2, "DD2"), entry(10, "DD"), entry(null, "DD")));

        // Wind gust / maxima (levels 2 & 10 like Python logic - include null fallback)
        m.put("max(wind_speed_of_gust PT1H)", map(entry(2, "FG2"), entry(10, "FG"), entry(null, "FG")));
        m.put("max(wind_speed PT1H)", map(entry(2, "FX2"), entry(10, "FX"), entry(null, "FX")));
        m.put("max_wind_speed(wind_from_direction PT1H)", map(entry(2, "DX2"), entry(10, "DX"), entry(null, "DX")));
        m.put("max_wind_speed(wind_from_direction_of_gust PT1H)", map(entry(2, "DG2"), entry(10, "DG"), entry(null, "DG")));

        // Snow
        m.put("surface_snow_thickness", map(entry(null, "SS")));

        // Humidity
        m.put("relative_humidity", map(entry(null, "UU")));
        m.put("mean(relative_humidity PT1H)", map(entry(null, "UM")));
        m.put("min(relative_humidity PT1H)", map(entry(null, "UN")));
        m.put("max(relative_humidity PT1H)", map(entry(null, "UX")));

        // Soil temperature (depths) + fallback
        m.put("soil_temperature", map(
                entry(5, "TJM5"),
                entry(10, "TJM10"),
                entry(20, "TJM20"),
                entry(30, "TJM30"),
                entry(50, "TJM50"),
                entry(100, "TJM100")
        ));

        // Soil moisture (volume fraction) depths + fallback
        m.put("volume_fraction_of_water_in_soil", map(
                entry(5, "VAN5"),
                entry(10, "VAN10"),
                entry(20, "VAN20"),
                entry(40, "VAN40"),
                entry(60, "VAN60")
        ));

        m.replaceAll((k, v) -> Collections.unmodifiableMap(v));
        return Collections.unmodifiableMap(m);
    }

    @SafeVarargs
    private static Map<Integer, String> map(Map.Entry<Integer, String>... entries) {
        Map<Integer, String> m = new HashMap<>();
        for (Map.Entry<Integer, String> e : entries) {
            m.put(e.getKey(), e.getValue());
        }
        return m;
    }

    private static Map.Entry<Integer, String> entry(Integer k, String v) {
        return new AbstractMap.SimpleEntry<>(k, v);
    }
}
