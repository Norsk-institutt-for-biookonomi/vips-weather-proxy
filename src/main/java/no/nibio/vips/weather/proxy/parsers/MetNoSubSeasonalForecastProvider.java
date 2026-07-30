/*
 * Copyright (c) 2015 NIBIO <https://www.nibio.no/>.
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * 
 */

package no.nibio.vips.weather.proxy.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nibio.vips.entity.WeatherObservation;

public class MetNoSubSeasonalForecastProvider {
    private final static String SUBSEASONAL_URL_TPL =
            "https://api.met.no/weatherapi/subseasonal/1.0/complete?lat={0}&lon={1}";

    private final static Map<String, String> elementMeasurementTypes = Map.of(
            "precipitation_amount", "RR",
            "air_temperature_mean", "TM",
            "air_temperature_max", "TX",
            "air_temperature_min", "TN");


    public List<WeatherObservation> getSubSeasonalForecasts(Double longitude, Double latitude)
            throws ParseWeatherDataException, OutOfAreaException {
        try {
            List<WeatherObservation> observations = new ArrayList<>();
            URL subSeasonalURL = new URL(MessageFormat.format(MetNoSubSeasonalForecastProvider.SUBSEASONAL_URL_TPL,
                    latitude,
                    longitude));
            SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            dFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            HttpURLConnection conn = (HttpURLConnection) subSeasonalURL.openConnection();
            conn.setRequestProperty("User-Agent", "lmt.nibio.no lmt@nibio.no");
            conn.addRequestProperty("accept", "application/json");
            conn.connect();
            if(conn.getResponseCode() == 422)
            {
                throw new OutOfAreaException("The requested location is outside of the service's area. See this link for details: https://api.met.no/weatherapi/subseasonal/1.0/documentation");
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String all = "";
            String line;

            while ((line = in.readLine()) != null) {
                all += line;
            }
            ObjectMapper oMapper = new ObjectMapper();
            JsonNode jNode = oMapper.readTree(all);

            JsonNode timeseries = jNode.findValue("timeseries");

            for (JsonNode timeInstant : timeseries) {
                // Get timestamp
                Instant time = Instant.parse(timeInstant.findValue("time").asText());
                JsonNode next24hours = timeInstant.findValue("data").findValue("next_24_hours").findValue("details");
                for (String key : elementMeasurementTypes.keySet()) {
                    Double elementValue = Double.parseDouble(next24hours.findValue(key).asText());
                    WeatherObservation obs = new WeatherObservation();


                    obs.setElementMeasurementTypeId(elementMeasurementTypes.get(key));
                    obs.setTimeMeasured(Date.from(time));
                    obs.setValue(elementValue);
                    obs.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1D);
                    observations.add(obs);
                }
            }
           return observations;

        } catch (IOException ex) {
            throw new ParseWeatherDataException(ex.getMessage());
        }
    }
}
