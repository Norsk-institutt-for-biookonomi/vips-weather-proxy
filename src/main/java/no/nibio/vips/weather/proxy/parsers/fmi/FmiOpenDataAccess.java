/*
 * Copyright (c) 2020 NIBIO <https://www.nibio.no/>.
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
package no.nibio.vips.weather.proxy.parsers.fmi;

import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * Get weather data from the Finnish Meteorological Institute's open data API Read the docs here:
 * https://en.ilmatieteenlaitos.fi/open-data
 *
 * The parser is a modified version of Markku Koistinen's parser made in the SpotIT project
 *
 * @author Markku Koistinen <markku.koistinen@luke.fi>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class FmiOpenDataAccess {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FmiOpenDataAccess.class);

    public FmiOpenDataAccess() {

    }

    //"http://data.fmi.fi/fmi-apikey/insert-your-apikey-here/wfs?request=getFeature&storedquery_id=fmi::forecast::hirlam::surface::point::multipointcoverage&place=[PLACE_NAME]&endtime=yyyy-MM-ddTHH:mm:ssZ"
    public String get24hrsForecast() {
        String response = "";

        DateTimeFunctions dateTime = new DateTimeFunctions();
        String fmiDateTime = dateTime.getNowPlusDays("yyyy-MM-dd HH:mm:ss", "UTC", 1);
        fmiDateTime = fmiDateTime.replace(" ", "T");
        fmiDateTime += "Z";
        //System.out.println(fmiDateTime);
        //String url = "http://data.fmi.fi/fmi-apikey/49804794-1f38-46f0-adad-bb3d1aae5ffa/wfs?request=getFeature&storedquery_id=fmi::forecast::hirlam::surface::point::multipointcoverage&place=Vihti&endtime" + fmiDateTime;
        String url =
            "http://opendata.fmi.fi/wfs?request=getFeature&storedquery_id=fmi::forecast::hirlam::surface::point::multipointcoverage&place=Vihti&endtime" + fmiDateTime;
        response = restGet(url);
        return response;
    }

    public String getTemporalData_prototype(String siteID, String startDateTime, String endDateTime) {
        String response = "";
        String url;
        DateTimeFunctions dateTimeFunctions = new DateTimeFunctions();
        startDateTime = dateTimeFunctions.getPreviousOrNextFullTenMinutes(startDateTime);
        endDateTime = dateTimeFunctions.getPreviousOrNextFullTenMinutes(endDateTime);
        String temporalFactor = startDateTime.substring(14, 16);
        //DateTime difference in days
        long dateTimeDiffInDays;
        dateTimeDiffInDays = dateTimeFunctions.getDateTimeDiffInDays_UTC(startDateTime, endDateTime);
        //DateTime difference in minutes
        long dateTimeDiffInMinutes;
        dateTimeDiffInMinutes = dateTimeFunctions.getDateTimeDiffInMinutes_UTC(startDateTime, endDateTime);

        //System.out.println("Days: " + dateTimeDiffInDays);
        //System.out.println("Minutes: " + dateTimeDiffInMinutes);

        //Iterate if time difference is more than 7 days
        String newEndDateTime;
        double differenceFactor;
        int iterationCount;
        String startDateISO;
        String endDateISO;
        String fmiResponse;
        FmiOpenDataParser fmiParser = new FmiOpenDataParser();
        if (dateTimeDiffInMinutes > 10080) {
            differenceFactor = (double) dateTimeDiffInMinutes / 10080;
            //System.out.println(differenceFactor);
            iterationCount = (int) Math.ceil(differenceFactor);
            //System.out.println(iterationCount + " iterations");
            for (int i = 0; i < iterationCount - 1; i++) {
                startDateISO = startDateTime.replace(" ", "T");
                startDateISO += "Z";
                newEndDateTime = dateTimeFunctions.getDatePlusDays_UTC("yyyy-MM-dd HH:mm:ss", "UTC", startDateTime, 7);
                endDateISO = newEndDateTime.replace(" ", "T");
                endDateISO += "Z";
                //url = "http://data.fmi.fi/fmi-apikey/49804794-1f38-46f0-adad-bb3d1aae5ffa/wfs?request=getFeature&storedquery_id=fmi::observations::weather::multipointcoverage&fmisid=" + siteID + "&maxlocations=1&starttime=" + startDateISO + "&endtime=" + endDateISO;
                url = "http://opendata.fmi.fi/wfs?request=getFeature&storedquery_id=fmi::observations::weather::multipointcoverage&fmisid=" + siteID + "&maxlocations=1&starttime=" + startDateISO + "&endtime=" + endDateISO;
                //System.out.println("URL=" + url);
                startDateTime = newEndDateTime;
                fmiResponse = restGet(url);
                response += fmiParser.getAsJSON_prototype(fmiResponse, temporalFactor);
            }
        }

        //System.out.println("Iteration done. Last query initialized...");
        startDateISO = startDateTime.replace(" ", "T");
        startDateISO += "Z";
        endDateISO = endDateTime.replace(" ", "T");
        endDateISO += "Z";
        //url = "http://data.fmi.fi/fmi-apikey/49804794-1f38-46f0-adad-bb3d1aae5ffa/wfs?request=getFeature&storedquery_id=fmi::observations::weather::multipointcoverage&fmisid=" + siteID + "&maxlocations=1&starttime=" + startDateISO + "&endtime=" + endDateISO;
        url = "http://opendata.fmi.fi/wfs?request=getFeature&storedquery_id=fmi::observations::weather::multipointcoverage&fmisid=" + siteID + "&maxlocations=1&starttime=" + startDateISO + "&endtime=" + endDateISO;
        //System.out.println("URL=" + url);
        fmiResponse = restGet(url);
        response += fmiParser.getAsJSON_prototype(fmiResponse, temporalFactor);
        response = response.replace("][", ", ");
        return response;
    }

    /**
     * 36 hour forecast data for given position
     *
     * @param longitude
     * @param latitude
     * @return
     */
    public String getForecastData(Double longitude, Double latitude) {
        try {
            //String URLTemplate = "http://opendata.fmi.fi/wfs?storedquery_id=fmi::forecast::hirlam::surface::point::multipointcoverage&latlon={0},{1}&request=getFeature&starttime={2}&parameters=Temperature,Humidity,WindSpeedMS,DewPoint,Precipitation1h,radiationglobal";
            String URLTemplate = "http://opendata.fmi.fi/wfs?storedquery_id=fmi::forecast::edited::weather::scandinavia::point::multipointcoverage&latlon={0},{1}&request=getFeature&starttime={2}&parameters=Temperature,Humidity,WindSpeedMS,DewPoint,Precipitation1h,radiationglobal";
            // Get today at midnight, GMT time zone
            LocalDateTime todayAtMidnight = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            ZoneId UTCId = ZoneId.of("UTC");
            ZonedDateTime UTCTodayAtMidnight = ZonedDateTime.of(todayAtMidnight, UTCId);

            String formattedUrl = MessageFormat.format(URLTemplate,
                latitude, longitude,
                UTCTodayAtMidnight.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX"))
            );
            return new Scanner(new URL(formattedUrl).openStream(), "UTF-8").useDelimiter("\\A").next();

        } catch (IOException ex) {
            LOGGER.error("Exception occurred when attempting to get forecast data", ex);
        }
        return null;
    }

    /**
     * Global radiation data from the given FMI station id
     *
     * @param fmiSid
     * @param startDateTime
     * @param endDateTime
     * @return
     */
    public String getRadiationData(String fmiSid, Instant startDateTime, Instant endDateTime) {
        try {
            String URLTemplate =
                "http://opendata.fmi.fi/wfs?request=getFeature&storedquery_id=fmi::observations::radiation::multipointcoverage&fmisid={0}&parameters=GLOB_1MIN&starttime={1}&endtime={2}";
            /*System.out.println(MessageFormat.format(URLTemplate, 
                    fmiSid, 
                    DateTimeFormatter.ISO_INSTANT.format(startDateTime), 
                    DateTimeFormatter.ISO_INSTANT.format(endDateTime)
            ));*/
            return new Scanner(new URL(MessageFormat.format(URLTemplate,
                fmiSid,
                DateTimeFormatter.ISO_INSTANT.format(startDateTime),
                DateTimeFormatter.ISO_INSTANT.format(endDateTime)
            )).openStream(), "UTF-8").useDelimiter("\\A").next();

        } catch (IOException ex) {
            LOGGER.error("Exception occurred when attempting to get radiation data", ex);
        }
        return null;
    }

    public String getTemporalData(String siteID, String startDateTime, String endDateTime) {
        String response = "";
        String url;
        DateTimeFunctions dateTimeFunctions = new DateTimeFunctions();
        startDateTime = dateTimeFunctions.getPreviousOrNextFullTenMinutes(startDateTime);
        endDateTime = dateTimeFunctions.getPreviousOrNextFullTenMinutes(endDateTime);
        String temporalFactor = startDateTime.substring(14, 16);
        //DateTime difference in days
        //long dateTimeDiffInDays = dateTimeFunctions.getDateTimeDiffInDays_UTC(startDateTime, endDateTime);
        //DateTime difference in minutes
        long dateTimeDiffInMinutes;
        dateTimeDiffInMinutes = dateTimeFunctions.getDateTimeDiffInMinutes_UTC(startDateTime, endDateTime);

        //System.out.println("Days: " + dateTimeDiffInDays);
        //System.out.println("Minutes: " + dateTimeDiffInMinutes);

        //Iterate if time difference is more than 7 days
        String newEndDateTime;
        double differenceFactor;
        int iterationCount;
        String startDateISO;
        String endDateISO;
        String fmiResponse;
        FmiOpenDataParser fmiParser = new FmiOpenDataParser();
        if (dateTimeDiffInMinutes > 10080) {
            differenceFactor = (double) dateTimeDiffInMinutes / 10080;
            //System.out.println(differenceFactor);
            iterationCount = (int) Math.ceil(differenceFactor);
            //System.out.println(iterationCount + " iterations");
            for (int i = 0; i < iterationCount - 1; i++) {
                //System.out.println("Start: " + startDateTime);
                startDateISO = startDateTime.replace(" ", "T");
                startDateISO += "Z";
                newEndDateTime = dateTimeFunctions.getDatePlusDays_UTC("yyyy-MM-dd HH:mm:ss", "UTC", startDateTime, 7);
                endDateISO = newEndDateTime.replace(" ", "T");
                endDateISO += "Z";
                //System.out.println("End: " + newEndDateTime);
                //url = "http://data.fmi.fi/fmi-apikey/49804794-1f38-46f0-adad-bb3d1aae5ffa/wfs?request=getFeature&storedquery_id=fmi::observations::weather::multipointcoverage&fmisid=" + siteID + "&maxlocations=1&starttime=" + startDateISO + "&endtime=" + endDateISO;
                url =
                    "http://opendata.fmi.fi/wfs?request=getFeature&storedquery_id=fmi::observations::weather::multipointcoverage&fmisid=" + siteID + "&maxlocations=1&starttime=" + startDateISO + "&endtime=" + endDateISO;
                startDateTime = newEndDateTime;
                fmiResponse = restGet(url);
                response += fmiParser.getAsJSON(fmiResponse, temporalFactor);
            }
        }

        startDateISO = startDateTime.replace(" ", "T");
        startDateISO += "Z";
        endDateISO = endDateTime.replace(" ", "T");
        endDateISO += "Z";
        //url = "http://data.fmi.fi/fmi-apikey/49804794-1f38-46f0-adad-bb3d1aae5ffa/wfs?request=getFeature&storedquery_id=fmi::observations::weather::multipointcoverage&fmisid=" + siteID + "&maxlocations=1&starttime=" + startDateISO + "&endtime=" + endDateISO;
        url =
            "http://opendata.fmi.fi/wfs?request=getFeature&storedquery_id=fmi::observations::weather::multipointcoverage&fmisid=" + siteID + "&maxlocations=1&starttime=" + startDateISO + "&endtime=" + endDateISO;
        fmiResponse = restGet(url);
        response += fmiParser.getAsJSON(fmiResponse, temporalFactor);
        response = response.replace("][", ", ");
        return response;
    }

    private String restGet(String url) {
        String response = "";

        String responseLine = "";

        try {
            URL urli = new URL(url);
            URLConnection connection = urli.openConnection();
            HttpURLConnection httpConn = (HttpURLConnection) connection;

            //byte[] b = soapRequest.getBytes();

            //httpConn.setRequestProperty( "Content-Length", String.valueOf( b.length ) );
            //httpConn.setRequestProperty("Accept","application/json");
            //httpConn.setRequestProperty("Content-Type","text/plain; charset=UTF-8");
            httpConn.setRequestMethod("GET");
            //httpConn.setDoOutput(true);
            httpConn.setDoInput(true);

            //OutputStream out = httpConn.getOutputStream();
            //out.write( b );
            //out.close();

            InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());

            BufferedReader in = new BufferedReader(isr);

            while ((responseLine = in.readLine()) != null) {
                response = response + responseLine;
            }
            in.close();

        } catch (Exception ex) {
            LOGGER.error("Connection failed", ex);
        }

        return response;
    }

}
