/*
 * Copyright (c) 2016 NIBIO <http://www.nibio.no/>. 
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
import java.net.URL;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import no.nibio.vips.entity.WeatherObservation;
import no.nibio.vips.util.WeatherElements;

/**
 * Reads/parses data from a Davis weather station using the fruitweb service: http://www.fruitweb.info/ 
 * @copyright 2016 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class FruitWebDavisDataParser {
    // Sample: https://fruitweb.info/sc/getFile.php?id=283&date=2016-01-01&pw=Kghr528Ub5
    // id and password combined must be the "remote ID" in VIPSLogic form
    public final static String FRUITWEB_URL_TEMPLATE = "https://fruitweb.info/sc/getFile.php?{0}&date={1}&sensors=all";
    //public final static String FRUITWEB_URL_TEMPLATE = "https://www.fruitweb.info/sc/getFile.php?{0}&date={1}";
    // Metos parameters, including name and aggregation type
    private final static String[][] ELEMENT_MEASUREMENT_TYPES = {
        {"RAIN",WeatherElements.PRECIPITATION,"SUM"},
        {"LW1",WeatherElements.LEAF_WETNESS_DURATION,"SUM"},
        {"AIRTEMP",WeatherElements.TEMPERATURE_MEAN,"AVG"},
        {"AIRHUM",WeatherElements.RELATIVE_HUMIDITY_MEAN,"AVG"},
        {"SOLARRAD",WeatherElements.GLOBAL_RADIATION,"AVG"},
        {"WINDSPEED", WeatherElements.WIND_SPEED_2M,"AVG"}
    };
    
    
    
    /**
     * Using output designed for RIMPro, parsing into WeatherObservations
     * @param stationID the METOS station ID
     * @param timeZone
     * @param startDate
     * @return 
     */
    public List<WeatherObservation> getWeatherObservations(String stationID, TimeZone timeZone, Date startDate, Date endDate) throws ParseWeatherDataException 
    {
        List<WeatherObservation> retVal = new ArrayList<>();
        SimpleDateFormat urlDFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        urlDFormat.setTimeZone(timeZone);
        dFormat.setTimeZone(timeZone);
        // Assuming 1 hour resolution until we find a timestamp that says :30
        Integer logIntervalId = WeatherObservation.LOG_INTERVAL_ID_1H;
        List<String[]> data = new ArrayList<>();
        String[] headers;
        Map<Integer, Integer> elementOrdering = new HashMap<>();
        try {
            URL fruitwebDavisURL = new URL(MessageFormat.format(FruitWebDavisDataParser.FRUITWEB_URL_TEMPLATE, stationID,urlDFormat.format(startDate)));
            //System.out.println(fruitwebDavisURL.toString());
            BufferedReader in = new BufferedReader(
            new InputStreamReader(fruitwebDavisURL.openStream()));

            String inputLine;
            Date testTimestamp;
            // We need to collect all the lines first, because we need to analyze
            // If the resolution is 30 minutes or 1 hour
            while ((inputLine = in.readLine()) != null)
            {
                //System.out.println(inputLine);
                String[] lineData = inputLine.split(";");
                // Skip empty lines
                if(lineData.length <= 1)
                {
                    continue;
                }
                // Check for valid start of line
                try {
                    testTimestamp = dFormat.parse(lineData[0] + " " + lineData[1]);
                    data.add(lineData);
                    if(lineData[1].split(":")[1].equals("30"))
                    {
                        logIntervalId = WeatherObservation.LOG_INTERVAL_ID_30M;
                    }
                } catch (ParseException ex) {
                    
                    // Is this the heading line?
                    // Then we parse it to set the ordering of elements
                    if(lineData[0].equals("DATE"))
                    {
                        headers = lineData;
                        // DATE and TIME should always be the two first ones
                        for(int i=2;i<lineData.length;i++)
                        {
                            for(int j=0;j<ELEMENT_MEASUREMENT_TYPES.length;j++)
                            {
                                if(ELEMENT_MEASUREMENT_TYPES[j][0].equals(lineData[i]))
                                {
                                    elementOrdering.put(i,j);
                                }
                            }
                        }
                    }
                }
            }
            in.close();
        } catch (IOException ex) {
            throw new ParseWeatherDataException(ex.getMessage());
        }
        
        /*for(Integer i: elementOrdering.keySet())
        {
        	System.out.println(ELEMENT_MEASUREMENT_TYPES[elementOrdering.get(i)][0] + "=" + i);
        }*/
        
        // Data comes in half-hour chunks (resolution = 30 minutes)
        // Unlike Metos, the hour starts at :30
        if(logIntervalId.equals(WeatherObservation.LOG_INTERVAL_ID_30M))
        {
            Boolean shouldBe30Now = true;
            String[] data30 = null; 
            String[] data00 = null;
            Date timestamp = null;
            for(String[] lineData: data)
            {
                // Skip lines that are not exactly :00 or :30
                if(
                        !lineData[1].split(":")[1].equals("00")
                        && ! lineData[1].split(":")[1].equals("30")
                        )
                {
                    continue;
                }
                else if(lineData[1].split(":")[1].equals("30") && shouldBe30Now)
                {
                    data30 = lineData;
                    
                    shouldBe30Now = false;
                    continue; // So that we summarize only after :00 data has been set too
                }
                else if(lineData[1].split(":")[1].equals("00") && !shouldBe30Now)
                {
                    data00 = lineData;
                    try
                    {
                        timestamp = dFormat.parse(lineData[0] + " " + lineData[1]);
                    }
                    catch(ParseException ex)
                    {
                        throw new ParseWeatherDataException("Error with time stamp in weather data from Davis/FruitWeb station: " + ex.getMessage());
                    }
                    shouldBe30Now = true;
                }
                else
                {
                    throw new ParseWeatherDataException("Doesn't make sense at " + lineData[0] + " " + lineData[1] + "!");
                }

                for(Integer i=2;i<data30.length;i++)
                {
                    Double aggregateValue = null;
                    Double value00 = null;
                    Double value30 = null;
                    Boolean atLeastOneValueMissing = false;
                    Integer elementMeasurementTypeIndex = elementOrdering.get(i);
                    if(elementMeasurementTypeIndex == null)
                    {
                    	continue;
                    }
                    try{value00 = Double.valueOf(data00[i].replaceAll(",","."));}catch(NumberFormatException ex){atLeastOneValueMissing = Boolean.TRUE;}
                    try{value30 = Double.valueOf(data30[i].replaceAll(",","."));}catch(NumberFormatException ex){atLeastOneValueMissing = Boolean.TRUE;}
                    if(value00 == null && value30 == null)
                    {
                        aggregateValue = null;
                    }
                    else
                    {
                        value00 = value00 == null ? 0.0 : value00;
                        value30 = value30 == null ? 0.0 : value30;
                        //System.out.println("element " + i + "=" + MetosDataParser.elementMeasurementTypes[elementMeasurementTypeIndex][1]);
                        if(FruitWebDavisDataParser.ELEMENT_MEASUREMENT_TYPES[elementMeasurementTypeIndex][2].equals("AVG"))
                        {
                            aggregateValue = (value00 + value30) / (atLeastOneValueMissing ? 1 : 2);
                        }
                        else
                        {
                            aggregateValue = (value00 + value30);
                        }
                    }
                    WeatherObservation obs = new WeatherObservation();
                    obs.setTimeMeasured(timestamp);
                    obs.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
                    obs.setElementMeasurementTypeId(FruitWebDavisDataParser.ELEMENT_MEASUREMENT_TYPES[elementMeasurementTypeIndex][1]);
                    obs.setValue(aggregateValue);
                    retVal.add(obs);
                }
            }
        }
        // Data is hourly, easy to add
        else
        {
            Date timestamp = null;
            for(String[] lineData: data)
            {
                // If minute != 00 we skip the line
                if(!lineData[1].split(":")[1].equals("00"))
                {
                    continue;
                }
                try
                {
                    timestamp = dFormat.parse(lineData[0] + " " + lineData[1]);
                }
                catch(ParseException ex)
                {
                    throw new ParseWeatherDataException("Error with time stamp in weather data from Metos station: " + ex.getMessage());
                }
                for(Integer i=2;i<lineData.length;i++)
                {
                    Double value = Double.valueOf(lineData[i].replaceAll(",","."));
                    Integer elementMeasurementTypeIndex = elementOrdering.get(i);
                    // This means there is an element type we don't collect
                    if(elementMeasurementTypeIndex == null)
                    {
                        continue;
                    }
                    WeatherObservation obs = new WeatherObservation();
                    obs.setTimeMeasured(timestamp);
                    obs.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
                    obs.setElementMeasurementTypeId(FruitWebDavisDataParser.ELEMENT_MEASUREMENT_TYPES[elementMeasurementTypeIndex][1]);
                    obs.setValue(value);
                    retVal.add(obs);
                }
            }
        }
        
        return retVal.stream().filter(obs -> 
                (obs.getTimeMeasured().compareTo(startDate) >= 0 && (endDate == null || obs.getTimeMeasured().compareTo(endDate) <= 0))
        ).collect(Collectors.toList());
    }
}
