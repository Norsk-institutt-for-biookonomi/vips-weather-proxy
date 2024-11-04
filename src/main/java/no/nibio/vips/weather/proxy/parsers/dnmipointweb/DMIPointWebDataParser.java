/*
 * Copyright (c) 2017 NIBIO <https://www.nibio.no/>. 
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

package no.nibio.vips.weather.proxy.parsers.dnmipointweb;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.datacontract.schemas._2004._07.dmiweatherservice.ArrayOfWeatherDataModel;
import org.datacontract.schemas._2004._07.dmiweatherservice.WeatherDataModel;
import org.datacontract.schemas._2004._07.dmiweatherservice.WeatherDataParameter;
import org.datacontract.schemas._2004._07.dmiweatherservice.WeatherDataSource;
import org.datacontract.schemas._2004._07.dmiweatherservice.WeatherInterval;
import org.datacontract.schemas._2004._07.dmiweatherservice.WeatherResponse;
import org.tempuri.IWeatherService;
import org.tempuri.WeatherService;
import no.nibio.vips.entity.WeatherObservation;
import no.nibio.vips.util.WeatherElements;
import no.nibio.vips.util.WeatherUtil;

/**
 * Gets data from the Danish Meteorological Institute's Point Web service
 * @copyright 2017-2024 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class DMIPointWebDataParser {

    private static final Map<String, WeatherDataParameter> PARAM_MAP;
    static
    {
        PARAM_MAP = new HashMap<>();
        PARAM_MAP.put(WeatherElements.TEMPERATURE_MEAN, WeatherDataParameter.AIRTEMP);
        PARAM_MAP.put(WeatherElements.PRECIPITATION, WeatherDataParameter.PREC);
        PARAM_MAP.put(WeatherElements.RELATIVE_HUMIDITY_MEAN, WeatherDataParameter.AIRRH);
        PARAM_MAP.put(WeatherElements.WIND_SPEED_2M, WeatherDataParameter.WINDSPEED);
        PARAM_MAP.put(WeatherElements.SOIL_TEMPERATURE_10CM_MEAN, WeatherDataParameter.SOILTEMP);
        PARAM_MAP.put(WeatherElements.LEAF_WETNESS_DURATION, WeatherDataParameter.LEAFWET);
    }
    //private final ZoneId danishZ = ZoneId.of("Europe/Copenhagen");
    private final ZoneId danishZ = ZoneId.of("GMT+1");
    //private final TimeZone danishTZ = TimeZone.getTimeZone("Europe/Copenhagen");
    private final TimeZone danishTZ = TimeZone.getTimeZone("GMT+1");
    
    public List<WeatherObservation> getData(Double longitude, Double latitude, Date dateFrom, Date dateTo)
    {
        List<WeatherObservation> rawObservations = new ArrayList<>();
        try {
            IWeatherService proxy = new WeatherService().getSslOffloadedBasicHttpBindingIWeatherService();
            UseableArrayOfWeatherDataSource wdsource = new UseableArrayOfWeatherDataSource();
            wdsource.add(WeatherDataSource.OBS);
            wdsource.add(WeatherDataSource.FORECAST);
            //wdsource.add(WeatherDataSource.NORMAL);
            
            GregorianCalendar gc = new GregorianCalendar(danishTZ);
            gc.setTime(dateFrom);
            XMLGregorianCalendar calFrom = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
            XMLGregorianCalendar calTo = null;
            if(dateTo != null)
            {
               gc.setTime(dateTo);
               calTo = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
            }
            
            
            UseableArrayOfWeatherDataParameter wdparam = new UseableArrayOfWeatherDataParameter();
            DMIPointWebDataParser.PARAM_MAP.values().stream().forEach(
                    listedParam -> wdparam.add(listedParam)
            );
            
            //WeatherResponse result = proxy.getWeatherData(
            
            WeatherResponse result = proxy.getWeatherDataExtended(
                String.valueOf(latitude).replace(".", ","), // Latitude ("X") Decimal separator is comma!
                String.valueOf(longitude).replace(".", ","), // Longitude ("Y") Decimal separator is comma!
                false, // true if UTM, false if decimal degrees
                null, // UTM Zone if using UTM
                WeatherInterval.HOUR, // Hourly or Daily data
                wdsource, // Set of data sources
                calFrom, // Start of period with data
                calTo, // End of period with data
                wdparam, // Set of requested parameters
                0.0, // Base temperature value
                false, // Use base temperature? (t/f)
                true, // Get 6 days of forecasts (only for the proxy.getWeatherDataExtended method)
                "VIPS", // Client info for the service
                "SpotIT" // Identification
            );
            
            ArrayOfWeatherDataModel value = result.getWeahterDataList().getValue();
            value.getWeatherDataModel().stream().forEach(weatherDataModel -> {
                    DMIPointWebDataParser.PARAM_MAP.keySet().stream().forEach(
                            VIPSParam->{
                                WeatherObservation obs = this.getWeatherObservation(VIPSParam, weatherDataModel);
                                if(obs != null)
                                {
                                    rawObservations.add(obs);
                                }
                            }
                    );
                }
            );
             
        } catch (Exception ex) {
            Logger.getLogger(DMIPointWebDataParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        //System.out.println("Number of extracted weather data = " + retVal.size());
        // After "now", the DMI service provides forecast values seamlessly. After approx 48 hours these
        // values turn into 6 hour intervals
        // 2019-01-18: We keep all observations, leave it up to the client to decide what to do with the data.
        // But we mark them with the correct measuring interval
        
        ZonedDateTime now = ZonedDateTime.now();
        //List<WeatherObservation> filteredObservations = new ArrayList<>();
        WeatherUtil wUtil = new WeatherUtil();
        List<WeatherObservation> retVal = new ArrayList<>();
        Set<String> parametersInResult = rawObservations.stream().map(obs->obs.getElementMeasurementTypeId()).collect(Collectors.toSet());
        parametersInResult.forEach((param) -> {
            ZonedDateTime lastObsTime = null;
            List<WeatherObservation> paramObs = wUtil.filterWeatherObservationsByParameter(rawObservations, param);
            Collections.sort(paramObs);
            for(WeatherObservation obs:paramObs)
            {
                ZonedDateTime obsTime = ZonedDateTime.ofInstant(obs.getTimeMeasured().toInstant(),danishZ);

                if(lastObsTime != null && obsTime.isAfter(now) && !obsTime.minusHours(6).isBefore(lastObsTime))
                {
                    obs.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_6H);
                }
                retVal.add(obs);
                lastObsTime = obsTime;         
            }
        });
        Collections.sort(retVal);
        return retVal;
    }

    private WeatherObservation getWeatherObservation(String VIPSParam, WeatherDataModel wDataModel) {
        WeatherObservation obs = new WeatherObservation();
        /*if(VIPSParam.equals(WeatherElements.RELATIVE_HUMIDITY_MEAN))
        {
            System.out.println(wDataModel.getDateDay().toString());
        }*/
        // When the DMI weather service shifts from historic to forecast data, timezone information
        // is added to the timestamp string. The consequence of this is that in a system
        // where default timezone is something else than CET (UTC+1/2), there will be an 
        // overlap of values, meaning that to values, e.g
        // 2019-01-27T22:00:00
        // 2019-01-27T23:00:00+01:00
        // Will both be translated to 2019-01-27T22:00:00 UTC if the default 
        // timezone on that system is UTC.
        // So we add danishTZ here to avoid this. It only tok 8-10 hours to debug....
        //System.out.println("DMIPointWebDataParser, timeMeasured=" + wDataModel.getDateDay());
        obs.setTimeMeasured(wDataModel.getDateDay().toGregorianCalendar(danishTZ, null, null).getTime());
        obs.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
        obs.setElementMeasurementTypeId(VIPSParam);
        
        try
        {
            switch(VIPSParam)
            {
                case WeatherElements.TEMPERATURE_MEAN:
                    obs.setValue(wDataModel.getAirtemp().getValue());      break;
                case WeatherElements.PRECIPITATION:
                    obs.setValue(wDataModel.getPrec().getValue());         break;
                case WeatherElements.RELATIVE_HUMIDITY_MEAN:
                    obs.setValue(wDataModel.getAirrh().getValue());        break;
                case WeatherElements.WIND_SPEED_2M:
                    obs.setValue(wDataModel.getWindspeed().getValue());    break;
                case WeatherElements.SOIL_TEMPERATURE_10CM_MEAN:
                    obs.setValue(wDataModel.getSoiltemp().getValue());     break;// Not sure if it's 10 or 5 cm depth yet
                case WeatherElements.SOIL_TEMPERATURE_5CM_MEAN:
                    obs.setValue(wDataModel.getSoiltemp().getValue());     break;
                case WeatherElements.LEAF_WETNESS_DURATION:
                    obs.setValue(wDataModel.getLeafwet().getValue());      break;
                default: return null;
            }
        }
        catch(NullPointerException ex) // The requested parameter didn't exist
        {
            return null;
        }
        
        return obs;
    }
}
