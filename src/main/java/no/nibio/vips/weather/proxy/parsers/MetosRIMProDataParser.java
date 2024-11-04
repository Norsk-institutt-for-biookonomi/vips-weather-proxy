/*
 * Copyright (c) 2015 NIBIO <http://www.nibio.no/>. 
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
import no.nibio.vips.entity.WeatherObservation;

/**
 * Reads/parses data from a Metos weather station
 * For future versions (need authentication per station): http://www.fieldclimate.com/json_manual/ 
 * @copyright 2015 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class MetosRIMProDataParser {
    public final static String METOS_URL_TEMPLATE = "http://www.fieldclimate.com/rimpro/all_sensors_csv.php?s={0}";
    // Metos parameters, including name and aggregation type
    private final static String[][] elementMeasurementTypes = {
        {"Solar radiation Dgt","Q0","AVG"},
        {"Precipitation","RR","SUM"},
        {"Wind speed","FM2","AVG"},
        {"Battery voltage","BATTERY","AVG"},
        {"Leaf Wetness","BT","SUM"},
        {"HC Air temperature","TM","AVG"},
        {"Air temperature","TM","AVG"},
        {"HC Relative humidity","UM","AVG"},
        {"Relative humidity","UM","AVG"}
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
        List<WeatherObservation> allObservations = this.getWeatherObservations(stationID, timeZone);
        for(WeatherObservation obs: allObservations)
        {
            if(obs.getTimeMeasured().compareTo(startDate) >= 0)
            {
                retVal.add(obs);
            }
        }
        return retVal;
    }
    
    /**
     * Using output designed for RIMPro, parsing into WeatherObservations
     * @param stationID the METOS station ID
     * @param timeZone
     * @return 
     */
    public List<WeatherObservation> getWeatherObservations(String stationID, TimeZone timeZone) throws ParseWeatherDataException 
    {
        List<WeatherObservation> retVal = new ArrayList<>();
        SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        dFormat.setTimeZone(timeZone);
        // Assuming 1 hour resolution until we find a timestamp that says :30
        Integer logIntervalId = WeatherObservation.LOG_INTERVAL_ID_1H;
        List<String[]> data = new ArrayList<>();
        String[] headers;
        Map<Integer, Integer> elementOrdering = new HashMap<>();
        try {
            URL metosURL = new URL(MessageFormat.format(MetosRIMProDataParser.METOS_URL_TEMPLATE, stationID));
            BufferedReader in = new BufferedReader(
            new InputStreamReader(metosURL.openStream()));

            String inputLine;
            Date testTimestamp = null;

            // We need to collect all the lines first, because we need to analyze
            // If the resolution is 30 minutes or 1 hour
            while ((inputLine = in.readLine()) != null)
            {
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
                    if(lineData[0].equals("Datum"))
                    {
                        headers = lineData;
                        // Datum and Zeit should always be the two first ones
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
            in.close();
        } catch (IOException ex) {
            throw new ParseWeatherDataException(ex.getMessage());
        }
        

        // Data comes in half-hour chunks (resolution = 30 minutes)
        if(logIntervalId.equals(WeatherObservation.LOG_INTERVAL_ID_30M))
        {
            Boolean shouldBe00Now = true;
            String[] data00 = null;
            String[] data30 = null; 
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
                else if(lineData[1].split(":")[1].equals("00") && shouldBe00Now)
                {
                    data00 = lineData;
                    try
                    {
                        timestamp = dFormat.parse(lineData[0] + " " + lineData[1]);
                    }
                    catch(ParseException ex)
                    {
                        throw new ParseWeatherDataException("Error with time stamp in weather data from Metos station: " + ex.getMessage());
                    }
                    shouldBe00Now = false;
                    continue; // So that we summarize only after :30 data has been set too
                }
                else if(lineData[1].split(":")[1].equals("30") && !shouldBe00Now)
                {
                    data30 = lineData;
                    shouldBe00Now = true;
                }
                else
                {
                    throw new ParseWeatherDataException("Doesn't make sense!");
                }

                for(Integer i=2;i<data00.length;i++)
                {
                    Double aggregateValue = null;
                    Double value00 = Double.valueOf(data00[i].replaceAll(",","."));
                    Double value30 = Double.valueOf(data30[i].replaceAll(",","."));
                    Integer elementMeasurementTypeIndex = elementOrdering.get(i);
                    // This means there is an element type we don't collect
                    if(elementMeasurementTypeIndex == null)
                    {
                        continue;
                    }
                    //System.out.println("element " + i + "=" + MetosRIMProDataParser.elementMeasurementTypes[elementMeasurementTypeIndex][1]);
                    if(MetosRIMProDataParser.elementMeasurementTypes[elementMeasurementTypeIndex][2].equals("AVG"))
                    {
                        aggregateValue = (value00 + value30) / 2;
                    }
                    else
                    {
                        aggregateValue = (value00 + value30);
                    }

                    WeatherObservation obs = new WeatherObservation();
                    obs.setTimeMeasured(timestamp);
                    obs.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
                    obs.setElementMeasurementTypeId(MetosRIMProDataParser.elementMeasurementTypes[elementMeasurementTypeIndex][1]);
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
                    obs.setElementMeasurementTypeId(MetosRIMProDataParser.elementMeasurementTypes[elementMeasurementTypeIndex][1]);
                    obs.setValue(value);
                    retVal.add(obs);
                }
            }
        }
                
            
        
        return retVal;
    }
}
