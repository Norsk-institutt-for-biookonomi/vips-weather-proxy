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

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import no.nibio.vips.entity.WeatherObservation;
import no.nibio.vips.util.InvalidAggregationTypeException;
import no.nibio.vips.util.WeatherObservationListException;
import no.nibio.vips.util.WeatherUtil;

/**
 * Reads/parses data from A-Lab weather stations
 * @copyright 2015-2019 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class ALabDataParser {
    
    private static final Map<String, String> parameters;
    static {
        Map<String, String> staticMap = new HashMap<>();
        staticMap.put("119", "TM");
        staticMap.put("120", "UM");
        staticMap.put("121", "RRACC"); // Accumulated rain over an unkown amount of time!
        staticMap.put("122", "DD2"); 
        staticMap.put("123", "FF2"); 
        staticMap.put("124", "PPP"); 
        staticMap.put("125", "TJM10"); 
        staticMap.put("126", "VAN10p"); 
        parameters = Collections.unmodifiableMap(staticMap);
    }
    // 
    public final static String ALAB_URL_TEMPLATE = "http://xml.a-log.net/xml-export.cgi?devid={0}&username={1}&password={2}&measurements=all&from={3}&to={4}";
    
    public List<WeatherObservation> getWeatherObservations(String stationID, TimeZone timeZone, Date startDate, String userName, String password) throws ParseWeatherDataException 
    {
        List<WeatherObservation> retVal = new ArrayList<>();
        DateFormat timeParseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        timeParseFormat.setTimeZone(timeZone);
        
        WeatherUtil wUtil = new WeatherUtil();
        startDate = wUtil.normalizeToExactDate(startDate, timeZone);
        Date endDate = wUtil.normalizeToExactDate(new Date(), timeZone);
        DateFormat urlDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        urlDateFormat.setTimeZone(timeZone);
        String from = urlDateFormat.format(startDate);
        String to = urlDateFormat.format(endDate);
        
        // Get the raw data from service, serialize into WeatherObservations
        Map<String, List<WeatherObservation>> serialized = new HashMap<>();
        try {
            URL aLabURL = new URL(MessageFormat.format(ALabDataParser.ALAB_URL_TEMPLATE, stationID,userName,password,from,to));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(aLabURL.openStream());


            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            
            // Get all measurements at 15 min intervals
            // Must aggregate afterwards
            //List<Node> list = doc.selectNodes("//Measurement");
            NodeList measurements = (NodeList) xpath.evaluate("//Measurement", doc, XPathConstants.NODESET);

            for (int i = 0; i < measurements.getLength(); i++) {
                Element measurement = (Element) measurements.item(i);
                Date timeMeasured = timeParseFormat.parse(measurement.getAttribute("Time"));

                NodeList valueNodes = measurement.getElementsByTagName("Value");
                for(int j = 0; j < valueNodes.getLength(); j++) 
                {
                    Element valueNode = (Element) valueNodes.item(j);
                    WeatherObservation obs = new WeatherObservation();
                    obs.setTimeMeasured(timeMeasured);
                    obs.setElementMeasurementTypeId(ALabDataParser.parameters.get(valueNode.getAttribute("Type")));
                    // Set logInterval to 15min
                    obs.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_15M);
                    obs.setValue(Double.valueOf(valueNode.getTextContent()));
                    
                    if(!serialized.containsKey(obs.getElementMeasurementTypeId()))
                    {
                        serialized.put(obs.getElementMeasurementTypeId(), new ArrayList<WeatherObservation>());
                    }
                    serialized.get(obs.getElementMeasurementTypeId()).add(obs);
                    
                }
                //System.out.println(timeMeasured);
            }
        }
        catch(IOException | XPathExpressionException | ParserConfigurationException | ParseException | SAXException ex)
        {
            throw new ParseWeatherDataException(ex.getMessage());
        }
        
        try
        {
            // Convert from 15 min intervals to hourly intervals
            // Bear in mind that RRACC must be converted to RR
            for(String param:serialized.keySet())
            {
                List<WeatherObservation> paramObs = serialized.get(param);
                paramObs = wUtil.removeDuplicateWeatherObservations(paramObs, 0.05);
                if(!param.equals("RRACC"))
                {
                    retVal.addAll(wUtil.getAggregateHourlyValues(paramObs, timeZone, WeatherObservation.LOG_INTERVAL_ID_15M, WeatherUtil.AGGREGATION_TYPE_AVERAGE));
                }
                else
                {
                    //System.out.println("RRACC being calculated");
                    retVal.addAll(wUtil.getIncrementalValuesFromAccumulated(paramObs, timeZone, WeatherObservation.LOG_INTERVAL_ID_1H));
                }
            }
        }catch (WeatherObservationListException | InvalidAggregationTypeException ex)
        {
            throw new ParseWeatherDataException(ex.getMessage());
        }
        Collections.sort(retVal);
        
        
        return retVal;
    }
    
    /**
     * 
     * @param stationID Must be like this example: 000000000013A24CEB01&username=TESTXX&password=TESTYY
     * @param timeZone
     * @param startDate
     * @return
     * @throws ParseWeatherDataException 
     */
    public List<WeatherObservation> getWeatherObservations(String stationID, TimeZone timeZone, Date startDate) throws ParseWeatherDataException 
    {
        // StationID must contain username and password
        String[] idParts = stationID.split("&");
        String userName = null;
        String password = null;
        for (String idPart : idParts) {
            String[] param = idPart.split("=");
            if(param.length == 2)
            {
                switch(param[0])
                {
                    case "username":
                        userName = param[1];
                        break;
                    case "password":
                        password = param[1];
                        break;
                    default:
                        break;
                }
            }
        }
        return this.getWeatherObservations(stationID, timeZone, startDate, userName, password);

    }
    
}
