/*
 * (C) 2019 NIBIO <https://www.nibio.no/>
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
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nibio.vips.entity.WeatherObservation;
import no.nibio.vips.util.InvalidAggregationTypeException;
import no.nibio.vips.util.WeatherObservationListException;
import no.nibio.vips.util.WeatherUtil;

/**
 * Gets data from the Tahmo API.
 * Read about the API here: 
 * 
 * @copyright 2017 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Brita Linnestad <brita.linnestad@nibio.no>
 */
public class TahmoDataParser {
    
    public final static String TAHMOS_URL_TEMPLATE = "https://datahub.tahmo.org/services/measurements/v2/stations/{0}/measurements/{1}?start={2}&end={3}&variable={4}";
    
    
    private final static String[][] elementMeasurementTypes = {
        {"te","TM","AVG"},
        {"ap","POM","AVG"},      
        {"pr","RR","SUM"},
        {"wd","DM2","AVG"},
        {"ws","FM2","AVG"},
        {"wg","FG2","MAX"},
        {"ra", "Q0", "AVG"},
        {"sm", "VAN20M", "AVG"},
        {"st", "TJM10", "AVG"},
        {"rh", "UM", "AVG"} 
    };
    
    public List<WeatherObservation> getWeatherObservations(String stationID, TimeZone timeZone, String start, String end, String variable) throws ParseWeatherDataException, ParseException, WeatherObservationListException, InvalidAggregationTypeException 
    {  
         List<WeatherObservation> retVal = new ArrayList<>();
         Map<String, List<WeatherObservation>> serialized = new HashMap<>();
         List<String> data = new ArrayList<>();
         
         // Assuming 1 hour resolution until we find a timestamp that says :05
         Integer logIntervalId = WeatherObservation.LOG_INTERVAL_ID_1H;
         String[] authStr = stationID.split(";;");     

         if(authStr.length != 3)
         {
            throw new ParseWeatherDataException("No or invalid credentials provided");
         }
         String stationCode = authStr[0];
   
         String[] headers;
         WeatherUtil wUtil = new WeatherUtil();
           
         DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
         dFormat.setTimeZone(timeZone);
      
         // Set to controlled values by default
         String collection = "controlled";
        
         try {  
            var tahmoURL = new URL(MessageFormat.format(TahmoDataParser.TAHMOS_URL_TEMPLATE, stationCode, collection, start, end, variable));
            URLConnection connection = tahmoURL.openConnection();
            String userpass=authStr[1] + ":" + authStr[2]; 
            String basicAuth = "Basic " + jakarta.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
            connection.setRequestProperty("Authorization",basicAuth);
                 
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            
            String all = "";
            String line;
            
            while((line = in.readLine()) != null)
            {   
                all += line;
            } 
            
            ObjectMapper oMapper = new ObjectMapper();
            JsonNode jNode = oMapper.readTree(all);
            
            JsonNode series = jNode.findValue("series");
          
            if(series != null) {
               
                JsonNode type = jNode.findValue("name");
                JsonNode values = jNode.findValue("values");  
                
                String name = type .toString().replace("\"","");
            
                var valueIndex = 0;
                var elementIndex = 0;
            
                if(name.equals("raw")) {
                    valueIndex = 4;
                    elementIndex = 5;
                } 
                else if(name.equals("controlled"))
                {
                    valueIndex = 10;
                    elementIndex = 11;
                }         
            
                // Check if we have 5 minutes intervall or 1 hour 
                for(int i=0;i< 50;i++)
                {
                    String inputLine = (values.get(i) .toString()).replaceAll("[\\[\\]^\"|\"$]","");
                    String[] lineData = inputLine.split(",");
                        
                    if(lineData[0].split(":")[1].equals("05"))
                    {
                        logIntervalId = WeatherObservation.LOG_INTERVAL_ID_5M;
                    }
                }
            
                if(logIntervalId.equals(WeatherObservation.LOG_INTERVAL_ID_5M))
                {
                    for(int i=0;i< values.size();i++)
                    {
                        String inputLine = (values.get(i) .toString()).replaceAll("[\\[\\]^\"|\"$]","");
                        String[] lineData = inputLine.split(",");
                        
                        for(int j=0;j<elementMeasurementTypes.length;j++)
                        {                                
                            if(elementMeasurementTypes[j][0].equals(lineData[elementIndex]) && !lineData[valueIndex].equals("null"))
                            {
                                // RH values from Tahmo parsers are between 0-1
                                int a=1;
                                if(elementMeasurementTypes[j][0].equals("rh")) { a=100; }
                                
                                Double value = a*Double.parseDouble(lineData[valueIndex]);
                                
                                WeatherObservation obs = new WeatherObservation();
                               
                                obs.setElementMeasurementTypeId(TahmoDataParser.elementMeasurementTypes[j][1]);
                                obs.setTimeMeasured(dFormat.parse(lineData[0]));   
                                obs.setValue(value);
                                obs.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_5M);
                       
                                if(!serialized.containsKey(obs.getElementMeasurementTypeId()))
                                {
                                    serialized.put(obs.getElementMeasurementTypeId(), new ArrayList<WeatherObservation>());
                                }
                                serialized.get(obs.getElementMeasurementTypeId()).add(obs);
                            }   
                       
                        }                        
                    } 
                 
                }
            
                else
                {
                    for(int i=0;i< values.size();i++)
                    {
                        String inputLine = (values.get(i) .toString()).replaceAll("[\\[\\]^\"|\"$]","");
                        String[] lineData = inputLine.split(",");
                
                        if(lineData.length <= 1)
                        {
                            continue;
                        }
                   
                        for(int j=0;j<elementMeasurementTypes.length-1;j++)
                        {         
                            if(elementMeasurementTypes[j][0].equals(lineData[elementIndex]) && !lineData[valueIndex].equals("null"))
                            {
                                // RH values from Tahmo parsers are between 0-1
                                int a=1;
                                if(elementMeasurementTypes[j][0].equals("rh")) { a=100; }
                                
                                Double value = a*Double.parseDouble(lineData[valueIndex]);
                               
                                WeatherObservation obs = new WeatherObservation();
              
                                obs.setElementMeasurementTypeId(TahmoDataParser.elementMeasurementTypes[j][1]);
                                obs.setTimeMeasured(dFormat.parse(lineData[0]));
                                obs.setValue(value);
                                obs.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
                       
                                retVal.add(obs);
                            }   
                       
                        }
                    }  
                }
            }
         }
         catch (IOException | ParseException ex) {
            throw new ParseWeatherDataException(ex.getMessage());
         }
        
        try
        {
            // Convert from 5 min intervals to hourly intervals
                   
            for(String param:serialized.keySet())
            {
                
                List<WeatherObservation> paramObs = serialized.get(param);
                paramObs = wUtil.removeDuplicateWeatherObservations(paramObs, 0.05);
                    
                switch (param) {
                    case "RR":
                        retVal.addAll(wUtil.getAggregateHourlyValues(paramObs, timeZone, WeatherObservation.LOG_INTERVAL_ID_5M, WeatherUtil.AGGREGATION_TYPE_SUM));
                        break;
                    case "FG2":
                        retVal.addAll(wUtil.getAggregateHourlyValues(paramObs, timeZone, WeatherObservation.LOG_INTERVAL_ID_5M, WeatherUtil.AGGREGATION_TYPE_MAXIMUM));
                        break;
                    default:
                        retVal.addAll(wUtil.getAggregateHourlyValues(paramObs, timeZone, WeatherObservation.LOG_INTERVAL_ID_5M, WeatherUtil.AGGREGATION_TYPE_AVERAGE));
                        break;
                }
            }
        }catch (WeatherObservationListException | InvalidAggregationTypeException ex)
        {
            throw new ParseWeatherDataException(ex.getMessage());
        }
        Collections.sort(retVal);
        
        return retVal;
        
    }
}
