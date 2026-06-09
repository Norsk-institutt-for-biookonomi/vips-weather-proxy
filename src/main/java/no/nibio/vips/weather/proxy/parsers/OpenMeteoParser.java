/*
 * Copyright (c) 2026 NIBIO <http://www.nibio.no/>. 
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

import static java.util.Map.entry;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openmeteo.sdk.Variable;
import com.openmeteo.sdk.VariableWithValues;
import com.openmeteo.sdk.VariablesSearch;
import com.openmeteo.sdk.VariablesWithTime;
import com.openmeteo.sdk.WeatherApiResponse;
import no.nibio.vips.entity.WeatherObservation;
import no.nibio.vips.util.InvalidAggregationTypeException;
import no.nibio.vips.util.WeatherObservationListException;
import no.nibio.vips.util.WeatherUtil;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Gets data from the Open Meteo API.
 * Read about the API here: 
 * 
 * @copyright 2017 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Brita Linnestad <brita.linnestad@nibio.no>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */

public class OpenMeteoParser {
    
    private static Logger LOGGER = LoggerFactory.getLogger(OpenMeteoParser.class);
    
    private String HISTORIC_ENDPOINT_TPL = "https://historical-forecast-api.open-meteo.com/v1/forecast?format=flatbuffers&latitude=%1$s&longitude=%2$s&start_date=%3$s&end_date=%4$s&%5$s&timezone=%6$s";
    private String FORECAST_ENDPOINT_TPL = "https://api.open-meteo.com/v1/forecast?format=flatbuffers&latitude=%1$s&longitude=%2$s&%3$s&timezone=%4$s";
    private Integer minimumObservationsPerDay = 15; 

    private TimeZone openMeteoTimeZone = TimeZone.getTimeZone("UTC");
    
    OkHttpClient httpClient = new OkHttpClient();
    
    private class OpenMeteoParameter {
        
        public OpenMeteoParameter(Integer variable)
        {
            this.variable = variable;
        }
        
        
        public OpenMeteoParameter(Integer variable, Integer altitude)
        {
            this(variable);
            this.altitude = altitude;
        }
        
        private Integer variable;
        private Integer altitude;

        /**
         * @return the variable
         */
        
        
        public Integer getVariable() {
            return variable;
        }

        /**
         * @param variable the variable to set
         */
        public void setVariable(Integer variable) {
            this.variable = variable;
        }
        /**
         * @return the altitude
         */
        public Integer getAltitude() {
            return altitude;
        }

        /**
         * @param altitude the altitude to set
         */
        public void setAltitude(Integer altitude) {
            this.altitude = altitude;
        }

    }
    
    // Parameter mapping
    private final Map<String, String> LMTToOpenMeteoRequestHourly = Map.ofEntries(
            entry("TM", "temperature_2m"), 
            entry("RR", "precipitation"), 
            entry("UM", "relative_humidity_2m"),
            entry("FM", "wind_speed_10m"), 
            entry("Q0", "shortwave_radiation") 
    ); 
    
    
    private final Map<String, OpenMeteoParameter> LMTToOpenMeteoResponseHourly = Map.ofEntries(
            entry("TM", new OpenMeteoParameter(Variable.temperature, 2)),  
            entry("RR", new OpenMeteoParameter(Variable.precipitation)),
            entry("UM", new OpenMeteoParameter(Variable.relative_humidity,2)),
            entry("FM", new OpenMeteoParameter(Variable.wind_speed,10)),
            entry("Q0", new OpenMeteoParameter(Variable.shortwave_radiation))

    );
    
    public List<WeatherObservation> getWeatherObservations(Double longitude, Double latitude, TimeZone timeZone, Date timeStart, Date timeEnd, List<String> elementMeasurementTypes, Integer logIntervalId) throws ParseWeatherDataException, WeatherObservationListException, InvalidAggregationTypeException
    {  
        Map<String, List<WeatherObservation>> serialized = new HashMap<>();
        WeatherUtil wUtil = new WeatherUtil();
        
        Date today = new Date();
        Boolean needForecasts = timeEnd.after(today);
        Boolean needHistoric = timeStart.before(today);
         
        SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd");
        dFormat.setTimeZone(this.openMeteoTimeZone);
        // Converting to UTC for OpenMeteo.
        String startDate = dFormat.format(timeStart);
        String endDate = dFormat.format(needForecasts ? today : timeEnd);
        
        String encodedTimeZone;
        encodedTimeZone = this.openMeteoTimeZone.getID(); 
        
        // if elementMeasurementTypes is NULL/empty, then return all Open-Meteo provided parameters
        if(elementMeasurementTypes == null || elementMeasurementTypes.isEmpty())
        {
            elementMeasurementTypes = List.of("TM","UM","RR","FM","Q0"); // Only unique parameters, no duplicates
        }
        // Remove any parameters that Open-Meteo does not provide
        elementMeasurementTypes = new ArrayList(elementMeasurementTypes.stream().filter(p->
                this.LMTToOpenMeteoRequestHourly.containsKey(p)
            ).collect(Collectors.toList()));
        
        
        String historicURL = needHistoric ? String.format(this.HISTORIC_ENDPOINT_TPL,
           latitude, longitude, startDate, endDate,
           this.getParametersURLString(elementMeasurementTypes), encodedTimeZone
        ) 
            : null;
        LOGGER.debug(historicURL);
        String forecastURL = needForecasts ? String.format(this.FORECAST_ENDPOINT_TPL,
                latitude, longitude,
                this.getParametersURLString(elementMeasurementTypes),
                encodedTimeZone
            )
            : null;
        LOGGER.debug(forecastURL);
        
        try 
        {
            
            WeatherApiResponse historicResponse = needHistoric ? this.getWeatherApiResponse(historicURL) : null;
            WeatherApiResponse forecastResponse = needForecasts ? this.getWeatherApiResponse(forecastURL) : null;
            
            LOGGER.debug("historicData is " + (historicResponse==null ? "" : "not ") + "null, forecastdata is " + (forecastResponse==null ? "" : "not ") + "null");
            
            List<WeatherObservation> historicData = needHistoric ? getWeatherDataFromWeatherApiResponse(historicResponse, elementMeasurementTypes) : null;
            List<WeatherObservation> forecastData = needForecasts ? getWeatherDataFromWeatherApiResponse(forecastResponse, elementMeasurementTypes) : null;
            
            
            // Create a TreeMap to store WeatherObservation objects with timeMeasured as the key
            Map<String, TreeMap<Date, WeatherObservation>> weatherDataMap = new HashMap<>();

            
            // Add all data from both lists to the map
            if (historicData != null) {
                for (WeatherObservation historicObs : historicData) {
                    weatherDataMap.computeIfAbsent(historicObs.getElementMeasurementTypeId(), k -> new TreeMap<>()).put(historicObs.getTimeMeasured(), historicObs);
                }
            }
            if (forecastData != null) {
                for (WeatherObservation forecast : forecastData) {
                    weatherDataMap.computeIfAbsent(forecast.getElementMeasurementTypeId(), k -> new TreeMap<>()).put(forecast.getTimeMeasured(), forecast);
                }
            }
            // Convert the values of each TreeMap to a single List
            List<WeatherObservation> weatherDataHourly = new ArrayList<>();
            for (TreeMap<Date, WeatherObservation> map : weatherDataMap.values()) {
                weatherDataHourly.addAll(map.values());
            }

            // Filter out all values outside of timeStart and timeEnd
            weatherDataHourly = weatherDataHourly.stream()
                    .filter(obs -> !obs.getTimeMeasured().before(timeStart) && obs.getTimeMeasured().compareTo(timeEnd) < 0)
                    .collect(Collectors.toList());
            
            
            // Sort the final list by date
            weatherDataHourly.sort(Comparator.comparing(WeatherObservation::getTimeMeasured));
           
            //If daily values, then aggregate
            if(logIntervalId == 2){
                
                List<WeatherObservation> weatherDataDaily = new ArrayList<>();
                
                for (WeatherObservation wo : weatherDataHourly) {
                     serialized.computeIfAbsent(wo.getElementMeasurementTypeId(), k -> new ArrayList<>()).add(wo);
                }
                
                for(String param:serialized.keySet())
                {
                  
                   List<WeatherObservation> paramObs = serialized.get(param);
                   paramObs = wUtil.removeDuplicateWeatherObservations(paramObs, 0.05);
                  
                   switch (param) {
                    case "RR":
                        weatherDataDaily.addAll(wUtil.getAggregatedDailyValues(paramObs, timeZone,minimumObservationsPerDay, WeatherUtil.AGGREGATION_TYPE_SUM));
                        break;
                    case "Q0":
                        weatherDataDaily.addAll(wUtil.getAggregatedDailyValues(paramObs, timeZone,minimumObservationsPerDay, WeatherUtil.AGGREGATION_TYPE_SUM_GLOBAL_RADIATION));
                        break;    
                    default:
                        weatherDataDaily.addAll(wUtil.getAggregatedDailyValues(paramObs, timeZone, minimumObservationsPerDay, WeatherUtil.AGGREGATION_TYPE_AVERAGE));
                        break;
                    }
                }
                
                Collections.sort(weatherDataDaily);
                return weatherDataDaily;
                }    
            
            return weatherDataHourly;
            
        } catch( IOException  ex) //| InvalidAggregationTypeException | WeatherObservationListException
        {
            ex.printStackTrace();
        }
        
        return null;
        
    }
    
    private WeatherApiResponse getWeatherApiResponse(String url) throws IOException
    {
        LOGGER.debug(url);
        Request request = new Request.Builder()
                .url(url).method("GET",null)
                .build();
        
        Call call = this.httpClient.newCall(request);
        Response response = call.execute();
       
        if(response.isSuccessful())
        {
            byte[] responseIN = response.body().bytes();
            ByteBuffer buffer = ByteBuffer.wrap(responseIN).order(ByteOrder.LITTLE_ENDIAN);
            WeatherApiResponse mApiResponse = WeatherApiResponse.getRootAsWeatherApiResponse((ByteBuffer) buffer.position(4));
            buffer.clear();
            return mApiResponse;
        }
        LOGGER.debug(response.message());
        return null;
    }
    
    private List<WeatherObservation> getWeatherDataFromWeatherApiResponse(WeatherApiResponse ApiResponse, List<String> elementMeasurementTypes) {
      
        List<WeatherObservation> retVal = new ArrayList<>();
        VariablesWithTime variablesWithTime =  ApiResponse.hourly();
        ArrayList<String> parameters = new ArrayList<>(elementMeasurementTypes);
        List<VariableWithValues> paramValues = new ArrayList<>();
            
        for(String parameter:parameters)
        {
            paramValues.add(this.getVariableWithValues(variablesWithTime, parameter));                 
        }
               
        int count=0;
        for(VariableWithValues paramData: paramValues)
        {               
            for(int row=0;row<paramData.valuesLength();row++)
            {
                WeatherObservation obs = new WeatherObservation();
                obs.setTimeMeasured(Date.from(Instant.ofEpochSecond(variablesWithTime.time()+ row*variablesWithTime.interval())));
                obs.setElementMeasurementTypeId(elementMeasurementTypes.get(count));
                obs.setValue(Double.valueOf(paramData.values(row)));
                obs.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
                 
                retVal.add(obs);
            }
                
            count++;
       
        }
     
        return retVal;
    }
    
    public String getParametersURLString(List<String> elementMeasurementTypes)
    {
        return "hourly=" + String.join(",", elementMeasurementTypes.stream()
                    .filter(p->this.LMTToOpenMeteoRequestHourly.containsKey(p))
                    .map(p->this.LMTToOpenMeteoRequestHourly.get(p)).collect(Collectors.toList()));
        
    }

    private VariableWithValues getVariableWithValues(VariablesWithTime variablesWithTime, String parameter) {
       
        Map<String, OpenMeteoParameter> mapping = this.LMTToOpenMeteoResponseHourly;

        if(mapping.containsKey(parameter))
        {
            OpenMeteoParameter param = mapping.get(parameter); 
            VariablesSearch search = new VariablesSearch(variablesWithTime).variable(param.getVariable());
            if(param.getAltitude() != null)
            {
                search = search.altitude(param.getAltitude());
            }
            return search.first();
        }
        
        return null;
        
    }
}
