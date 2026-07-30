/*
 * Copyright (c) 2015 NIBIO <https://www.nibio.no/>. 
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */
package no.nibio.vips.weather.proxy.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nibio.vips.entity.WeatherObservation;
import no.nibio.vips.util.WeatherElements;
import no.nibio.vips.weather.proxy.util.DOMUtils;

/**
 * @copyright 2015-2021 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 * @author Brita Linnestad <brita.linnestad@nibio.no>
 */
public class MetNoLocationForecastProvider {

    //private final static String YR_API_URL = "https://api.met.no/weatherapi/locationforecast/1.9/?lat={0};lon={1}";
    private final static String YR_API_URLXML = "https://api.met.no/weatherapi/locationforecast/2.0/classic?lat={0}&lon={1}";
    private final static String YR_API_URLJSON = "https://api.met.no/weatherapi/locationforecast/2.0/complete?lat={0}&lon={1}";
    private final static String YR_API_URLJSON_WITH_ALTITUDE = "https://api.met.no/weatherapi/locationforecast/2.0/complete?lat={0}&lon={1}&altitude={2}";

    private final static String[][] elementMeasurementTypes = {
        {"precipitation_amount", "RR", "SUM"},
        {"air_pressure_at_sea_level", "POM", "AVG"},
        {"air_temperature", "TT", "AVG"},
        {"relative_humidity", "UU", "AVG"},
        {"wind_from_direction", "DD", "AVG"},
        {"wind_speed", "FF", "AVG"},
        {"wind_speed_of_gust", "FG", "AVG"}
    };


    /**
     * Get weather forecasts as daily values. Approximately 9 days ahead.
     * @param longitude
     * @param latitude
     * @param altitude
     * @return
     */
    public List<WeatherObservation> getWeatherForecastsDaily(Double longitude, Double latitude, Double altitude) throws ParseWeatherDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getWeatherForecastsDaily'");
    }

    /**
     * Get weather forecasts as hourly values. Shorter timespan than daily values approximately 54 hours into the future
     * @param longitude
     * @param latitude
     * @param altitude
     * @return
     */
    public List<WeatherObservation> getWeatherForecastsHourly(Double longitude, Double latitude, Double altitude) throws ParseWeatherDataException {
        JsonNode timeseries = this.getTimeSeries(longitude, latitude, altitude);       
        return this.getWeatherForecastsHourlyFromMetnoData(timeseries);
    }

    public List<WeatherObservation> getWeatherForecastsHourlyFromMetnoData(JsonNode timeseries) throws ParseWeatherDataException {
        List<WeatherObservation> weatherForecastObservations = new ArrayList<>();

        for (JsonNode timeSeriesElement : timeseries) {
                // If the node contains a next_1_hours node, it is an hourly value. Skip otherwise.
                JsonNode next1hours = timeSeriesElement.findValue("next_1_hours");
                if (next1hours == null) {
                    continue;
                }
                JsonNode timeSeriesInstant = timeSeriesElement.findValue("instant");
                Instant instant = Instant.parse(timeSeriesElement.findValue("time").asText());
                if (instant != null) {
                    String element;
                Double elementValue;
                for (int j = 0; j < elementMeasurementTypes.length - 1; j++) {
                    element = MetNoLocationForecastProvider.elementMeasurementTypes[j][0];
                    JsonNode elementNode = timeSeriesInstant.findValue(element);
                    elementValue = elementNode != null ? elementNode.asDouble() : null;
                    // Precipitation is not an instantaneous value, but a sum over the last hour. Therefore, we need to get it from the next_1_hours node.
                    if(elementValue == null) {
                        JsonNode next1hoursElementNode = next1hours.findValue(element);
                        elementValue = next1hoursElementNode != null ? next1hoursElementNode.asDouble() : null;
                    }

                        if (elementValue != null) {
                            WeatherObservation obs = new WeatherObservation();
                            Double value = elementValue;
                            obs.setElementMeasurementTypeId(MetNoLocationForecastProvider.elementMeasurementTypes[j][1]);
                            obs.setTimeMeasured(Date.from(instant));
                            obs.setValue(value);
                            obs.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
                            weatherForecastObservations.add(obs);
                        }
                    }
                }

        }
        return weatherForecastObservations;
    }


    public JsonNode getTimeSeries(Double longitude, Double latitude, Double altitude) throws ParseWeatherDataException {
        JsonNode timeseries = null;
        try {
            URI yrURI = altitude != null ? new URI(MessageFormat.format(MetNoLocationForecastProvider.YR_API_URLJSON_WITH_ALTITUDE,
            latitude.toString(),
            longitude.toString(),
            altitude.toString())) 
            : new URI(MessageFormat.format(MetNoLocationForecastProvider.YR_API_URLJSON,
            latitude,
            longitude.toString()));

            URLConnection connection = yrURI.toURL().openConnection();
            connection.setRequestProperty("User-Agent", "lmt.nibio.no lmt@nibio.no");
            connection.addRequestProperty("accept", "application/json");
            connection.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String all = "";
            String line;

            while ((line = in.readLine()) != null) {
                all += line;
            }

            ObjectMapper oMapper = new ObjectMapper();
            JsonNode jNode = oMapper.readTree(all);

            timeseries = jNode.findValue("timeseries");
        } catch (URISyntaxException | IOException ex) {
            ex.printStackTrace();
            throw new ParseWeatherDataException("Failed to parse weather data. " + ex.getClass().getName() + ": " + ex.getMessage() + " for coordinates: " + latitude + ", " + longitude + ", " + altitude);
        }
        return timeseries;
    }
    

    public List<WeatherObservation> getWeatherForecastsJSON(Double longitude, Double latitude, Double altitude) throws ParseException {
        List<WeatherObservation> yrValues = new ArrayList<>();

        SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        dFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Integer log_interval = WeatherObservation.LOG_INTERVAL_ID_1H;

        try {
            URL yrURL = new URL(MessageFormat.format(MetNoLocationForecastProvider.YR_API_URLJSON,
                    String.valueOf(latitude.floatValue()),
                    String.valueOf(longitude.floatValue())));

            URLConnection connection = yrURL.openConnection();
            connection.setRequestProperty("User-Agent", "lmt.nibio.no lmt@nibio.no");
            connection.addRequestProperty("accept", "application/json");
            connection.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String all = "";
            String line;

            while ((line = in.readLine()) != null) {
                all += line;
            }

            ObjectMapper oMapper = new ObjectMapper();
            JsonNode jNode = oMapper.readTree(all);

            JsonNode timeseries = jNode.findValue("timeseries");

            for (int i = 0; i < timeseries.size() - 1; i++) {
                JsonNode instant = timeseries.get(i).findValue("instant");
                JsonNode next1hours = timeseries.get(i).findValue("next_1_hours");
                JsonNode next6hours = timeseries.get(i).findValue("next_6_hours");

                Date thisHour = dFormat.parse((timeseries.get(i).findValue("time").toString()).replaceAll("[\"|\"$]", ""));
                Date nextHour = dFormat.parse((timeseries.get(i + 1).findValue("time").toString()).replaceAll("[\"|\"$]", ""));
                long difference = (nextHour.getTime() - thisHour.getTime()) / 3600000; //convert to 1 hour 

                String element;
                String elementValue = null;
               
                if (instant != null) {
                    for (int j = 2; j < elementMeasurementTypes.length - 1; j++) {
                        element = MetNoLocationForecastProvider.elementMeasurementTypes[j][0];
                        elementValue = (instant.findValue(element).toString());

                        if (!elementValue.equals("null")) {
                            WeatherObservation obs = new WeatherObservation();

                            Double value = Double.parseDouble(elementValue);

                            obs.setElementMeasurementTypeId(MetNoLocationForecastProvider.elementMeasurementTypes[j][1]);
                            obs.setTimeMeasured(thisHour);
                            obs.setValue(value);
                            obs.setLogIntervalId(log_interval);

                            yrValues.add(obs);
                        }
                    }
                }
                
                if (next1hours != null) {
                    element = MetNoLocationForecastProvider.elementMeasurementTypes[0][0];
                    elementValue = (next1hours.findValue(element).toString());

                    if (!elementValue.equals("null")) {
                        Double value = Double.parseDouble(elementValue);

                        WeatherObservation obs = new WeatherObservation();

                        obs.setElementMeasurementTypeId(MetNoLocationForecastProvider.elementMeasurementTypes[0][1]);
                        obs.setTimeMeasured(thisHour);
                        obs.setValue(value);
                        obs.setLogIntervalId(log_interval);

                        yrValues.add(obs);
                    }
                } else if (next6hours != null) {
                    element = MetNoLocationForecastProvider.elementMeasurementTypes[0][0];
                    elementValue = (next6hours.findValue(element).toString());
                    if (!elementValue.equals("null")) {
                        Double value = Double.parseDouble(elementValue);
                        WeatherObservation obs = new WeatherObservation();

                        obs.setElementMeasurementTypeId(MetNoLocationForecastProvider.elementMeasurementTypes[0][1]);
                        obs.setTimeMeasured(dFormat.parse((timeseries.get(i).findValue("time").toString()).replaceAll("[\"|\"$]", "")));

                        obs.setValue(value);
                        obs.setLogIntervalId(log_interval);

                        yrValues.add(obs);
                    }
                }


                if (difference > 1) {
                    log_interval = WeatherObservation.LOG_INTERVAL_ID_6H;
                }

            }
        } catch (IOException | ParseException ex) {
            ex.printStackTrace();
        }

        return this.createHourlyDataFromYr(yrValues);

    }

    private List<WeatherObservation> createHourlyDataFromYr(List<WeatherObservation> yrValues) {

        List<WeatherObservation> hourlyData = new ArrayList<>();
        List<WeatherObservation> POM = new ArrayList<>();
        List<WeatherObservation> TM = new ArrayList<>();
        List<WeatherObservation> UM = new ArrayList<>();
        List<WeatherObservation> FF2 = new ArrayList<>();
        List<WeatherObservation> DD2 = new ArrayList<>();
        List<WeatherObservation> RR = new ArrayList<>();

        Collections.sort(yrValues);
        Calendar cal = Calendar.getInstance();

        for (WeatherObservation yrValue : yrValues) {
            // Need to do this in order to make it work properly!
            yrValue.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);

            switch (yrValue.getElementMeasurementTypeId()) {
                case WeatherElements.TEMPERATURE_INSTANTANEOUS:
                    if (!TM.isEmpty()) {
                        WeatherObservation previousObs = TM.get(TM.size() - 1);
                        TM.addAll(this.getInterpolatedObservations(previousObs, yrValue, WeatherElements.TEMPERATURE_MEAN));
                    }
                    yrValue.setElementMeasurementTypeId(WeatherElements.TEMPERATURE_MEAN);
                    TM.add(yrValue);
                    break;
                case WeatherElements.RELATIVE_HUMIDITY_INSTANTANEOUS:
                    if (!UM.isEmpty()) {
                        WeatherObservation previousObs = UM.get(UM.size() - 1);
                        UM.addAll(this.getInterpolatedObservations(previousObs, yrValue, WeatherElements.RELATIVE_HUMIDITY_MEAN));
                    }
                    yrValue.setElementMeasurementTypeId(WeatherElements.RELATIVE_HUMIDITY_MEAN);
                    UM.add(yrValue);
                    break;
                case WeatherElements.WIND_SPEED_10MIN_10M:
                    if (!FF2.isEmpty()) {
                        WeatherObservation previousObs = FF2.get(FF2.size() - 1);
                        FF2.addAll(this.getInterpolatedObservations(previousObs, yrValue, WeatherElements.WIND_SPEED_10MIN_2M));
                    }
                    yrValue.setElementMeasurementTypeId(WeatherElements.WIND_SPEED_10MIN_2M);
                    FF2.add(yrValue);
                    break;
                case WeatherElements.WIND_DIR_10MIN_10M:
                    if (!DD2.isEmpty()) {
                        WeatherObservation previousObs = DD2.get(DD2.size() - 1);
                        DD2.addAll(this.getInterpolatedObservations(previousObs, yrValue, WeatherElements.WIND_DIR_10MIN_2M));
                    }
                    yrValue.setElementMeasurementTypeId(WeatherElements.WIND_DIR_10MIN_2M);
                    DD2.add(yrValue);
                    break;    
                case WeatherElements.PRECIPITATION:
                    if (!RR.isEmpty()) {
                        WeatherObservation previousObs = RR.get(RR.size() - 1);
                        cal.setTime(previousObs.getTimeMeasured());
                        cal.add(Calendar.HOUR_OF_DAY, 1);
                        while (cal.getTime().compareTo(yrValue.getTimeMeasured()) < 0) {
                            WeatherObservation dummy = new WeatherObservation();
                            dummy.setTimeMeasured(cal.getTime());
                            dummy.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
                            dummy.setElementMeasurementTypeId(WeatherElements.PRECIPITATION);
                            dummy.setValue(0.0);
                            RR.add(dummy);
                            cal.add(Calendar.HOUR_OF_DAY, 1);
                        }
                    }
                    RR.add(yrValue);
                    break;
                default:
                    break;
            }
        }
        hourlyData.addAll(TM);
        hourlyData.addAll(UM);
        hourlyData.addAll(RR);
        hourlyData.addAll(FF2);
        hourlyData.addAll(DD2);
        return hourlyData;
    }

    public List<WeatherObservation> getWeatherForecastsXML(Double longitude, Double latitude, Double altitude) throws ParseWeatherDataException {
        List<WeatherObservation> yrValues = new ArrayList<>();
        URL yrURL;

        try {
            yrURL = new URL(MessageFormat.format(
                    MetNoLocationForecastProvider.YR_API_URLXML,
                    String.valueOf(latitude.floatValue()),
                    String.valueOf(longitude.floatValue())));

            URLConnection connection = yrURL.openConnection();
            connection.setRequestProperty("User-Agent", "lmt.nibio.no lmt@nibio.no");
            connection.addRequestProperty("accept", "application/xml");
            connection.connect();

            //System.out.println("yrURL=" + yrURL.toString());
            // TODO: Parse with DOM parser
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(connection.getInputStream());

            NodeList nodes = doc.getElementsByTagName("time");
            Long hourDelta = 3600l * 1000l;
            Long threeHoursDelta = 3 * hourDelta;
            Long sixHoursDelta = 6 * hourDelta;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
            Map<Date, WeatherObservation> RRMap = new HashMap<>();
            Date earliestHourlyPrecipitationObservation = null;

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                Date fromTime = format.parse(node.getAttributes().getNamedItem("from").getNodeValue());
                Date toTime = format.parse(node.getAttributes().getNamedItem("to").getNodeValue());
                Node node2 = DOMUtils.getNode("location", node.getChildNodes());

                // TODO: Handle different kinds of elements and durations?
                // The instantaneous measured values                
                if (fromTime.compareTo(toTime) == 0 && DOMUtils.getNode("temperature", node2.getChildNodes()) != null) {
                    WeatherObservation TT = new WeatherObservation();
                    TT.setTimeMeasured(fromTime);
                    TT.setElementMeasurementTypeId(WeatherElements.TEMPERATURE_INSTANTANEOUS);
                    TT.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
                    TT.setValue(Double.parseDouble(DOMUtils.getNodeAttr("temperature", "value", node2.getChildNodes())));
                    yrValues.add(TT);
                }
                if (fromTime.compareTo(toTime) == 0 && DOMUtils.getNode("humidity", node2.getChildNodes()) != null) {
                    WeatherObservation UU = new WeatherObservation();
                    UU.setTimeMeasured(fromTime);
                    UU.setElementMeasurementTypeId(WeatherElements.RELATIVE_HUMIDITY_INSTANTANEOUS);
                    UU.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
                    UU.setValue(Double.parseDouble(DOMUtils.getNodeAttr("humidity", "value", node2.getChildNodes())));
                    yrValues.add(UU);
                }
                if (fromTime.compareTo(toTime) == 0 && DOMUtils.getNode("windSpeed", node2.getChildNodes()) != null) {
                    WeatherObservation FF = new WeatherObservation();
                    FF.setTimeMeasured(fromTime);
                    FF.setElementMeasurementTypeId(WeatherElements.WIND_SPEED_10MIN_10M);
                    FF.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
                    FF.setValue(Double.parseDouble(DOMUtils.getNodeAttr("windSpeed", "mps", node2.getChildNodes())));
                    yrValues.add(FF);
                }
                // The aggregated values (duration known, but comes in parallel)
                if (fromTime.compareTo(toTime) != 0 && DOMUtils.getNode("precipitation", node2.getChildNodes()) != null) {
                    WeatherObservation RR = new WeatherObservation();
                    RR.setTimeMeasured(fromTime);
                    RR.setElementMeasurementTypeId(WeatherElements.PRECIPITATION);
                    RR.setValue(Double.parseDouble(DOMUtils.getNodeAttr("precipitation", "value", node2.getChildNodes())));

                    if (toTime.getTime() - fromTime.getTime() == hourDelta) {
                        RR.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
                        if (earliestHourlyPrecipitationObservation == null) {
                            earliestHourlyPrecipitationObservation = RR.getTimeMeasured();
                        }
                    } else if (toTime.getTime() - fromTime.getTime() == threeHoursDelta) {
                        RR.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_3H);
                    } else if (toTime.getTime() - fromTime.getTime() == sixHoursDelta) {
                        RR.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_6H);
                    } else {
                        continue;
                    }

                    // The earliest observations may be with 6 or 3 hour resolution
                    // In order to avoid overestimation of rain in the beginning,
                    // We skip ahead until we find observations with 1 hour resolution
                    if (!RR.getLogIntervalId().equals(WeatherObservation.LOG_INTERVAL_ID_1H)
                            && (earliestHourlyPrecipitationObservation == null || earliestHourlyPrecipitationObservation.after(RR.getTimeMeasured()))) {
                        continue;
                    }

                    // We keep the rain observation with the highest resolution
                    WeatherObservation obsWithSameTimeStamp = RRMap.get(RR.getTimeMeasured());
                    if (obsWithSameTimeStamp != null) {
                        // Replace if better resolution
                        if ((RR.getLogIntervalId().equals(WeatherObservation.LOG_INTERVAL_ID_3H) && obsWithSameTimeStamp.getLogIntervalId().equals(WeatherObservation.LOG_INTERVAL_ID_6H))
                                || (RR.getLogIntervalId().equals(WeatherObservation.LOG_INTERVAL_ID_1H) && obsWithSameTimeStamp.getLogIntervalId().equals(WeatherObservation.LOG_INTERVAL_ID_3H))
                                || (RR.getLogIntervalId().equals(WeatherObservation.LOG_INTERVAL_ID_1H) && obsWithSameTimeStamp.getLogIntervalId().equals(WeatherObservation.LOG_INTERVAL_ID_6H))) {
                            RRMap.remove(RR.getTimeMeasured());
                            RRMap.put(RR.getTimeMeasured(), RR);
                        }
                    } else {
                        // No duplicate, so safely store
                        RRMap.put(RR.getTimeMeasured(), RR);
                    }
                }
            }
            yrValues.addAll(RRMap.values());
        } catch (IOException | ParserConfigurationException | SAXException | ParseException ex) {
            //ex.printStackTrace();
            throw new ParseWeatherDataException(ex.getClass().getName() + ": " + ex.getMessage());
        }

        return this.createHourlyDataFromYr(yrValues);
    }

    private List<WeatherObservation> getInterpolatedObservations(WeatherObservation start, WeatherObservation end, String elementMeasurementTypeId) {
        List<WeatherObservation> retVal = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        Double difference = end.getValue() - start.getValue();
        Long steps = (end.getTimeMeasured().getTime() - start.getTimeMeasured().getTime()) / 3600000;
        Double delta = difference / steps;
        cal.setTime(start.getTimeMeasured());
        cal.add(Calendar.HOUR_OF_DAY, 1);
        int counter = 1;
        while (cal.getTime().compareTo(end.getTimeMeasured()) < 0) {
            WeatherObservation interpolated = new WeatherObservation();
            interpolated.setElementMeasurementTypeId(elementMeasurementTypeId);
            interpolated.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
            interpolated.setTimeMeasured(cal.getTime());
            interpolated.setValue(start.getValue() + (delta * counter++));
            retVal.add(interpolated);
            cal.add(Calendar.HOUR_OF_DAY, 1);
        }
        return retVal;
    }

    
}
