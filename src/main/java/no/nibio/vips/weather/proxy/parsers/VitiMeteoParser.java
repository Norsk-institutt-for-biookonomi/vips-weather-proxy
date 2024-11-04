/*
 * Copyright (c) 2019 NIBIO <https://www.nibio.no/>. 
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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import no.nibio.vips.entity.WeatherObservation;
 
 
 /**
  *
  * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
  */
 public class VitiMeteoParser {
     
     private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(VitiMeteoParser.class);

    @Inject
    @ConfigProperty(name = "no.nibio.vips.logic.weather.VITIMETEO_API_USERNAME")
    String vitiMeteoApiUserName;

    @Inject
    @ConfigProperty(name = "no.nibio.vips.logic.weather.VITIMETEO_API_PASSWORD")
    String vitiMeteoApiPassword;
     
     public final static String VITIMETEO_URL_TEMPLATE = "https://vitimeteo.ch/api/stations/{0}/weatherdata.json?startdate={1}&enddate={2}&groupby={3}&sen_list={4}";
     
     private final static String[][] elementMeasurementTypes = {
         {"series_1_avg","TM","AVG"},
         {"series_1_max","TX","MAX"},
         {"series_1_min","TN","MIN"},
         {"series_4_avg","UM", "AVG"}, 
         {"series_6_sum","RR","SUM"},
         {"series_7_sum","BT","AVG"},
         {"series_9_avg", "FM2", "AVG"},
         {"series_11_avg", "Q0", "AVG"},
     };
        
     
     
     public List<WeatherObservation> getWeatherObservations(String stationId, String startDate, String endDate, String timezone, String logIntervalId, List<String> VIPSWeatherParameters) throws IOException, URISyntaxException, ParseWeatherDataException, ParseException {
        
         List<WeatherObservation> retVal = new ArrayList<>();
         
         TimeZone timeZone = TimeZone.getTimeZone(timezone);
         DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         dFormat.setTimeZone(timeZone);
         
         // Create list of sensors to fetch from vitimeteo api
         String sensor_list = "";
         
         if(!VIPSWeatherParameters.isEmpty()) {
             sensor_list = "[";
             for(int i=0; i < VIPSWeatherParameters.size();i++){
             
                 for (String[] elementMeasurementType : elementMeasurementTypes) {
                     if(elementMeasurementType[1].equals(VIPSWeatherParameters.get(i))){
                         String aggregation = elementMeasurementType[0].split("_")[2];
                         sensor_list += (i > 0 ? "," : "") 
                                 + "[" + elementMeasurementType[0].replaceAll("[^0-9]", "") 
                                 + ",'" + aggregation + 
                                 "']";
                     }
                 }
             }
             sensor_list += "]";
         }
          
         try {
             
             String period = null;    
             Integer logInterval = null;
           
             if(logIntervalId.equals(WeatherObservation.LOG_INTERVAL_ID_1H.toString())){
                     period = "hour";
                     logInterval = WeatherObservation.LOG_INTERVAL_ID_1H;            
             } else if(logIntervalId.equals(WeatherObservation.LOG_INTERVAL_ID_1D.toString())){
                     period = "day";
                     logInterval = WeatherObservation.LOG_INTERVAL_ID_1D;
             } else {
                 throw new ParseWeatherDataException("LogIntervalId not valid");
             }
             
             if(sensor_list.length()==0){
                 throw new ParseWeatherDataException("Element measurement type is missing");
             }
             
             URL vitiURL = new URL(MessageFormat.format(VitiMeteoParser.VITIMETEO_URL_TEMPLATE, stationId, startDate, endDate,period, sensor_list));
             LOGGER.debug(vitiURL.toString());
             URLConnection connection = vitiURL.openConnection();
             String userpass= vitiMeteoApiUserName + ":" + vitiMeteoApiPassword; 
             String basicAuth = "Basic " + jakarta.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
             connection.setRequestProperty("Authorization",basicAuth);
            
             BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
             
             String content="";
             String line;
         
             while ((line = in.readLine()) != null) {
                 content += line;
             }
               
             ObjectMapper oMapper = new ObjectMapper();
             JsonNode jNode = oMapper.readTree(content);
             
             JsonNode dataSeries = jNode.findValue("chart");
         
             int completeSetOfObs = 2;
             
             if(dataSeries != null) {
                 for (String[] elementMeasurementType : elementMeasurementTypes) {
                     JsonNode type = jNode.findValue(elementMeasurementType[0]);
                     if (type != null) {    
                         JsonNode data = type.findValue("point");
                         if (data.isArray()) {
                             for (final JsonNode objNode : data) {
                                 String inputLine = (objNode .toString()).replaceAll("[\\[\\]^|\"$]","");
                                 String[] lineData = inputLine.split(",");
                                 if (lineData.length == completeSetOfObs) {
                                     WeatherObservation obs = new WeatherObservation();
                                     obs.setElementMeasurementTypeId(elementMeasurementType[1]);
                                     obs.setTimeMeasured(dFormat.parse(lineData[0]));
                                     obs.setValue(Double.valueOf(lineData[1]));
                                     obs.setLogIntervalId(logInterval);
                                     retVal.add(obs);
                                 }
                             }
                         }
                     }      
                 }
             }
         }
         catch (IOException | ArrayIndexOutOfBoundsException ex) {
             throw new ParseWeatherDataException(ex.getMessage());
         }
         
         return retVal;
     }
    
 }
 