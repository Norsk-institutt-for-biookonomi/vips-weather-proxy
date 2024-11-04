/*
 * Copyright (c) 2016 NIBIO <https://www.nibio.no/>. 
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
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import no.nibio.vips.entity.WeatherObservation;
import no.nibio.vips.util.WeatherUtil;
import no.nibio.vips.util.WebUtil;

/**
 * @copyright 2016 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class USPestDataParser {
    public final static String USPEST_URL_TEMPLATE = "http://uspest.org/risk/models";
    // Parameters, including name and aggregation type
    private final static String[][] elementMeasurementTypes = {
        {"rain","RR","SUM"},
        {"temp","TM","AVG"},
        {"dewpt","DEWPOINT","AVG"},
        {"rel_hum","UM","AVG"},
        {"windspeed","FM2","AVG"},
        {"leafwet","BT","AVG"}
    };
    
    /**
     * Makes sure only data after the requested date is returned
     * @param stationID
     * @param timeZone
     * @param startDate
     * @return
     * @throws ParseWeatherDataException 
     */
    public List<WeatherObservation> getWeatherObservations(String stationID, TimeZone timeZone, Date startDate) throws ParseWeatherDataException 
    {
        List<WeatherObservation> retVal = new ArrayList<>();
        Map<Integer, Integer> elementOrdering = new HashMap<>();
        WeatherUtil wUtil = new WeatherUtil();
        SimpleDateFormat usPestDFormat = new SimpleDateFormat("MMM dd yyyy HH");
        usPestDFormat.setTimeZone(timeZone);
        SimpleDateFormat postFormDFormat = new SimpleDateFormat("yyyyMMdd");
        //Date today = SystemTime.getSystemTime();
        Date today = new Date();
        /*try
        {
            today = SystemTime.getSystemTime();
        }
        catch(NumberFormatException ex)
        {
            
        }*/
        
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("station", stationID);
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTime(startDate);
        int startDOY = cal.get(Calendar.DAY_OF_YEAR);
        queryParams.put("stm",String.valueOf(cal.get(Calendar.MONTH)+1)); // Start month
        queryParams.put("std",String.valueOf(cal.get(Calendar.DATE))); // Start dayOfMonth
        queryParams.put("sty",String.valueOf(cal.get(Calendar.YEAR))); // Start year
        cal.setTime(today);
        int todayDOY = cal.get(Calendar.DAY_OF_YEAR);
        queryParams.put("span",String.valueOf(todayDOY + 10 - startDOY)); // Number of days from start // Seems as it needs to be not to far in the future
        queryParams.put("fsm",String.valueOf(cal.get(Calendar.MONTH)+1)); // Forecast start month
        queryParams.put("fsd",String.valueOf(cal.get(Calendar.DATE))); // Forecast start dayOfMonth
        // fcast, Type of forecast
        // 1 = after 7day use 10 year averages (default)
        // 2 = after 7day use 30 year averages
        // 3 = after 7day use data from 1 year ago (if available)
        // 4  = after 7day use data from 2 years ago (if available)
        // 5 = after 7day use NMME extended seasonal forecast
        // 6 = after 7day use the CFSv2 extended seasonal forecast
        queryParams.put("fcast","1");
        // Bad weather data detection and replacement config
        queryParams.put("en_v2_tdp_missing", "on"); // Replace missing Temperature and Dew Point
        queryParams.put("en_v2_tdp_flatline", "on"); // Replace flat line Temperature and Dew Point
        queryParams.put("en_v2_tdp_range", "on"); // Replace range exceed Temperature and Dew Point
        //queryParams.put("en_v2_tdp_extrap", "on"); // NOT RECOMMENDED
        queryParams.put("en_v2_tdp_interp", "on"); // Replace data out of bounds of an interpolation
        //queryParams.put("en_v2_tdp_elevreg", "on"); // NOT RECOMMENDED
        queryParams.put("en_v2_windspeed_missing", "on"); // Replace missing wind speed
        queryParams.put("en_v2_rain_missing", "on"); // Replance missing precipitation
        queryParams.put("download_data","Download Data");
        queryParams.put("stdt",postFormDFormat.format(startDate)); // Probably start date, but why the double info (see stm/std)
        queryParams.put("date",postFormDFormat.format(today)); // Should be "today"
        
        String[] weatherParams = {"temp", "dewpt","rel_hum","windspeed","rain","leafwet"};
        Map<String, String[]> arrP = new HashMap<>();
        arrP.put("wp", weatherParams);
        // Keep track of which timestamps we already have,
        // to make sure overlapping forecast values don't overwrite measured values
        Set<Date> measuredDataTimestamps = new HashSet<>();
        //queryParams.keySet().stream().forEach(key -> System.out.println(key + "=" + queryParams.get(key)) );
        try
        {
            String output = WebUtil.getOutputFromPostRequest(USPestDataParser.USPEST_URL_TEMPLATE, queryParams, arrP);
            try (BufferedReader reader = new BufferedReader(new StringReader(output))) {
                reader.lines().forEach(
                        inputLine -> {
                            String[] lineData = inputLine.split(",");
                            //System.out.println(inputLine);
                            
                            // Skip empty lines
                            if(lineData.length <= 1)
                            {
                                return;
                            }
                            // Check for valid start of line
                            try {
                                
                                String dateAndTimeStr = lineData[0] + " " + lineData[1];
                                Date timeMeasured = usPestDFormat.parse(dateAndTimeStr);
                                // If this time stamp already has been processed, this must be
                                // an overlapping forecast value. We skip it.
                                if(measuredDataTimestamps.contains(timeMeasured))
                                {
                                    return;
                                }
                                else
                                {
                                    measuredDataTimestamps.add(timeMeasured);
                                }
                                // Do stuff
                                for(Integer i=2;i<lineData.length;i++)
                                {
                                    // Skip empty columns
                                    Double value = 0.0;
                                    try
                                    {
                                        value = Double.valueOf(lineData[i].replaceAll(",","."));
                                    }
                                    catch(NumberFormatException ex) {/*System.out.println("Error formatting \"" + lineData[i] + "\" lineData[" + i + "]");*/}
                                    
                                    Integer elementMeasurementTypeIndex = elementOrdering.get(i);
                                    // This means there is an element type we don't collect
                                    if(elementMeasurementTypeIndex == null)
                                    {
                                        continue;
                                    }
                                    WeatherObservation obs = new WeatherObservation();
                                    obs.setTimeMeasured(timeMeasured);
                                    obs.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
                                    obs.setElementMeasurementTypeId(USPestDataParser.elementMeasurementTypes[elementMeasurementTypeIndex][1]);
                                    // Converting from US system to VIPS
                                    // Temps measured in F -> C
                                    if(obs.getElementMeasurementTypeId().equals("TM"))
                                    {
                                        obs.setValue(wUtil.getCelciusFromFahrenheit(value));
                                    }
                                    // Rain measured in inches -> mm
                                    else if(obs.getElementMeasurementTypeId().equals("RR"))
                                    {
                                        obs.setValue(wUtil.getMillimetersFromInches(value));
                                    }
                                    // Leaf wetness [0...10] -> [0...60]
                                    else if(obs.getElementMeasurementTypeId().equals("BT"))
                                    {
                                        obs.setValue(wUtil.getLeafWetnessValueFromUSPest(value));
                                    }
                                    else
                                    {
                                        obs.setValue(value);
                                    }
                                    retVal.add(obs);
                                }
                            } catch (ParseException ex) {
                                
                                // Is this the heading line?
                                // Then we parse it to set the ordering of elements
                                if(lineData[0].equals("Date"))
                                {
                                    // Date and hour should always be the two first ones
                                    for(int i=2;i<lineData.length;i++)
                                    {
                                        for(int j=0;j<elementMeasurementTypes.length;j++)
                                        {
                                            if(elementMeasurementTypes[j][0].equals(lineData[i]))
                                            {
                                                elementOrdering.put(i,j);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                );
            }
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
        return retVal;
    }
}
