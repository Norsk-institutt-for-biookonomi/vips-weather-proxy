/*
 * Copyright (c) 2019 NIBIO <http://www.nibio.no/>.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * 
 *
 */

package no.nibio.vips.weather.proxy.resource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import no.nibio.vips.entity.WeatherObservation;
import no.nibio.vips.util.InvalidAggregationTypeException;
import no.nibio.vips.util.WeatherObservationListException;
import no.nibio.vips.util.XDate;
import no.nibio.vips.weather.proxy.parsers.ALabDataParser;
import no.nibio.vips.weather.proxy.parsers.FruitWebDavisDataParser;
import no.nibio.vips.weather.proxy.parsers.MetNoLocationForecastProvider;
import no.nibio.vips.weather.proxy.parsers.MetNoSubSeasonalForecastProvider;
import no.nibio.vips.weather.proxy.parsers.MeteobotAPIDataParser;
import no.nibio.vips.weather.proxy.parsers.MetosAPIDataParser;
import no.nibio.vips.weather.proxy.parsers.OpenMeteoParser;
import no.nibio.vips.weather.proxy.parsers.OutOfAreaException;
import no.nibio.vips.weather.proxy.parsers.ParseWeatherDataException;
import no.nibio.vips.weather.proxy.parsers.TahmoDataParser;
import no.nibio.vips.weather.proxy.parsers.USPestDataParser;
import no.nibio.vips.weather.proxy.parsers.VitiMeteoParser;
import no.nibio.vips.weather.proxy.parsers.dnmipointweb.DMIPointWebDataParser;
import no.nibio.vips.weather.proxy.parsers.fmi.FmiIntermediator;
import no.nibio.vips.weather.proxy.parsers.fmi.FmiOpenDataAccess;
import no.nibio.vips.weather.proxy.parsers.fmi.FmiOpenDataForecastParser;
import no.nibio.vips.weather.proxy.parsers.fmi.FmiOpenDataRadiationParser;


/**
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 * @author Brita Linnestad <brita.linnestad@nibio.no>
 * @copyright 2019 <a href="http://www.nibio.no/">NIBIO</a>
 */
@Path("rest/")
public class WeatherProxyResource {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(WeatherProxyResource.class);

    @Inject
    @ConfigProperty(name = "no.nibio.vips.logic.weather.TAHMOOPEN_CREDENTIALS")
    String tahmoOpenCredentials;

    /**
     * Get weather data from the Finnish Meteorological Institute's open data API Read the docs here:
     * https://en.ilmatieteenlaitos.fi/open-data
     * <p>
     * The parser is a modified version of Markku Koistinen's parser made in the SpotIT project
     *
     * @param stationId
     * @param timeZonePOST
     * @param timeZoneGET
     * @param startDatePOST
     * @param startDateGET
     * @param endDatePOST
     * @param endDateGET
     * @param elementMeasurementTypesPOST
     * @param elementMeasurementTypesGET
     * @return
     */
    @GET
    @Path("fmi/{stationId}")
    @Produces("application/json;charset=UTF-8")
    public Response getFMIVIPSWeatherData(
            @PathParam("stationId") String stationId,
            @FormParam("timeZone") String timeZonePOST,
            @QueryParam("timeZone") String timeZoneGET,
            @FormParam("startDate") String startDatePOST,
            @QueryParam("startDate") String startDateGET,
            @FormParam("endDate") String endDatePOST,
            @QueryParam("endDate") String endDateGET,
            @FormParam("elementMeasurementTypes[]") String[] elementMeasurementTypesPOST,
            @QueryParam("elementMeasurementTypes[]") String[] elementMeasurementTypesGET) {
        try {
            String timeZoneParam = timeZonePOST != null ? timeZonePOST : timeZoneGET != null ? timeZoneGET : "UTC";
            TimeZone timeZone = TimeZone.getTimeZone(timeZoneParam);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            format.setTimeZone(timeZone);
            String startDateParam = startDatePOST != null ? startDatePOST : startDateGET;
            XDate startDate1 = new XDate(format.parse(startDateParam));
            String endDateParam = endDatePOST != null ? endDatePOST : endDateGET;
            XDate endDate1 = new XDate(format.parse(endDateParam));
            List<String> elementMeasurementTypes =
                    (elementMeasurementTypesPOST != null && elementMeasurementTypesPOST.length > 0)
                            ? Arrays.asList(elementMeasurementTypesPOST)
                            : (elementMeasurementTypesGET != null && elementMeasurementTypesGET.length > 0)
                                    ? Arrays.asList(elementMeasurementTypesGET)
                                    : null;

            // The first set of observations, with parameters TM, FF2, UM and RR
            FmiIntermediator intermediator = new FmiIntermediator();
            String obsAsString = intermediator.getTemporalData_vips(stationId, startDate1.getISOUTCFormat(),
                    endDate1.getISOUTCFormat());

            ObjectMapper mapper = new ObjectMapper();
            List<WeatherObservation> observations =
                    mapper.readValue(obsAsString, new TypeReference<List<WeatherObservation>>() {});
            // Then, if Q0 has been requested, get those and supply
            if (elementMeasurementTypes != null && elementMeasurementTypes.contains("Q0")) {
                FmiOpenDataAccess dA = new FmiOpenDataAccess();
                FmiOpenDataRadiationParser rP = new FmiOpenDataRadiationParser();
                // Radiation data max period is 7 days (168 hours)
                Instant start7DayPeriod = startDate1.toInstant();
                Instant end7DayPeriod = start7DayPeriod.plus(7, ChronoUnit.DAYS);
                while (start7DayPeriod.isBefore(endDate1.toInstant())) {
                    String radiationDataFromFMIXML = dA.getRadiationData(stationId, start7DayPeriod, end7DayPeriod);
                    // System.out.println(radiationDataFromFMIXML);
                    if (!radiationDataFromFMIXML.isBlank()) {
                        observations.addAll(rP.getVIPSWeatherObservations(radiationDataFromFMIXML));
                    }
                    start7DayPeriod = start7DayPeriod.plus(7, ChronoUnit.DAYS);
                    end7DayPeriod = end7DayPeriod.plus(7, ChronoUnit.DAYS);
                }
            }
            Collections.sort(observations);
            return Response.ok().entity(observations).build();
        } catch (ParseException | IOException | NullPointerException ex) {
            ex.printStackTrace();
            return Response.serverError().entity(ex).build();
        }
    }

    /**
     * Data from Metos's FieldClimate API
     *
     * @param stationId
     * @param timeZonePOST
     * @param timeZoneGET
     * @param startDatePOST
     * @param startDateGET
     * @return
     */
    @GET
    @Path("metosapi/{stationId}")
    @Produces("application/json;charset=UTF-8")
    public Response getMetosAPIWeatherData(
            @PathParam("stationId") String stationId,
            @FormParam("timeZone") String timeZonePOST,
            @QueryParam("timeZone") String timeZoneGET,
            @FormParam("startDate") String startDatePOST,
            @QueryParam("startDate") String startDateGET) {
        List<WeatherObservation> observations;
        try {
            String timeZoneParam = timeZonePOST != null ? timeZonePOST : timeZoneGET != null ? timeZoneGET : "UTC";
            TimeZone timeZone = TimeZone.getTimeZone(timeZoneParam);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            format.setTimeZone(timeZone);
            String startDateParam = startDatePOST != null ? startDatePOST : startDateGET;
            Date startDate1 = format.parse(startDateParam);
            // System.out.println("WeatherProxyService: startDate1=" + startDate1);
            observations = new MetosAPIDataParser().getWeatherObservations(stationId, timeZone, startDate1);
        } catch (ParseException | ParseWeatherDataException | NullPointerException ex) {
            LOGGER.error("Exception while attempting to get MetosAPI weather data", ex);
            return Response.serverError().entity(ex).build();
        }
        return Response.ok().entity(observations).build();
    }

    /**
     * Data from the MeteoBot API
     *
     * @param method
     * @param stationId
     * @param startDatePOST
     * @param startDateGET
     * @param endDatePOST
     * @param endDateGET
     * @return
     */
    @GET
    @Path("meteobotapi/Index")
    @Produces("application/json;charset=UTF-8")
    public Response getMeteobotAPIWeatherData(
            @QueryParam("stationId") String stationId,
            @FormParam("startDate") String startDatePOST,
            @QueryParam("startDate") String startDateGET,
            @FormParam("endDate") String endDatePOST,
            @QueryParam("endDate") String endDateGET) {
        List<WeatherObservation> observations;
        try {
            String startDateParam = startDatePOST != null ? startDatePOST : startDateGET;
            String endDateParam = endDatePOST != null ? endDatePOST : endDateGET;

            observations = new MeteobotAPIDataParser().getWeatherObservations(stationId, startDateParam, endDateParam);

        } catch (ParseWeatherDataException ex) {
            return Response.serverError().entity(ex).build();
        }
        return Response.ok().entity(observations).build();
    }

    @GET
    @Path("tahmo")
    @Produces("application/json;charset=UTF-8")
    public Response getTahmoAPIWeatherData(
            @QueryParam("stationId") String stationId,
            @FormParam("timeZone") String timeZonePOST,
            @QueryParam("timeZone") String timeZoneGET,
            @QueryParam("startDate") String startDateStr,
            @QueryParam("startTime") String startTimeStr,
            @QueryParam("endDate") String endDateStr,
            @QueryParam("endTime") String endTimeStr,
            @QueryParam("variable") String variable)
            throws ParseException, WeatherObservationListException, InvalidAggregationTypeException {
        List<WeatherObservation> observations;
        try {
            String timeZoneParam = timeZonePOST != null ? timeZonePOST : timeZoneGET != null ? timeZoneGET : "UTC";
            TimeZone timeZone = TimeZone.getTimeZone(timeZoneParam);

            startTimeStr = startTimeStr != null ? startTimeStr : "00";
            endTimeStr = endTimeStr != null ? endTimeStr : "23";

            String startDate = startDateStr + "T" + startTimeStr + ":00:00Z";
            String endDate = endDateStr + "T" + endTimeStr + ":00:00Z";

            observations =
                    new TahmoDataParser().getWeatherObservations(stationId, timeZone, startDate, endDate, variable);
        } catch (ParseWeatherDataException ex) {
            return Response.serverError().entity(ex).build();
        }

        return Response.ok().entity(observations).build();
    }

    @GET
    @Path("tahmoopen")
    @Produces("application/json;charset=UTF-8")
    public Response getTahmoAPIWeatherDataOpen(
            @QueryParam("stationId") String stationId,
            @FormParam("timeZone") String timeZonePOST,
            @QueryParam("timeZone") String timeZoneGET,
            @QueryParam("startDate") String startDateStr,
            @QueryParam("startTime") String startTimeStr,
            @QueryParam("endDate") String endDateStr,
            @QueryParam("endTime") String endTimeStr,
            @QueryParam("variable") String variable)
            throws ParseException, WeatherObservationListException, InvalidAggregationTypeException {
        stationId = stationId + tahmoOpenCredentials;
        return this.getTahmoAPIWeatherData(stationId, timeZonePOST, timeZoneGET, startDateStr, startTimeStr, endDateStr,
                endTimeStr, variable);

    }


    /**
     * Data from Davis stations subscribing to the FruitWeb service
     *
     * @param stationId
     * @param timeZonePOST
     * @param timeZoneGET
     * @param startDatePOST
     * @param startDateGET
     * @param endDatePOST
     * @param endDateGET
     * @return
     */
    @GET
    @Path("fruitwebdavis/{stationId}")
    @Produces("application/json;charset=UTF-8")
    public Response getFruitWebDavisWeatherData(
            @PathParam("stationId") String stationId,
            @FormParam("timeZone") String timeZonePOST,
            @QueryParam("timeZone") String timeZoneGET,
            @FormParam("startDate") String startDatePOST,
            @QueryParam("startDate") String startDateGET,
            @FormParam("endDate") String endDatePOST,
            @QueryParam("endDate") String endDateGET) {
        List<WeatherObservation> observations;
        try {
            String timeZoneParam = timeZonePOST != null ? timeZonePOST : timeZoneGET != null ? timeZoneGET : "UTC";
            TimeZone timeZone = TimeZone.getTimeZone(timeZoneParam);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            format.setTimeZone(timeZone);
            String startDateParam = startDatePOST != null ? startDatePOST : startDateGET;
            Date startDate1 = format.parse(startDateParam);
            String endDateParam = endDatePOST != null ? endDatePOST : endDateGET != null ? endDateGET : null;
            Date endDate1 = endDateParam != null ? format.parse(endDateParam) : null;
            observations = new FruitWebDavisDataParser().getWeatherObservations(URLDecoder.decode(stationId, "UTF-8"),
                    timeZone, startDate1, endDate1);
        } catch (ParseException | ParseWeatherDataException | NullPointerException | UnsupportedEncodingException ex) {
            return Response.serverError().entity(ex).build();
        }
        return Response.ok().entity(observations).build();
    }

    /**
     * Get data from A-lab stations (Finland based company: http://www.a-lab.fi/)
     *
     * @param stationId
     * @param timeZonePOST
     * @param timeZoneGET
     * @param startDatePOST
     * @param startDateGET
     * @param userNamePOST
     * @param userNameGET
     * @param passwordPOST
     * @param passwordGET
     * @return
     */
    @GET
    @Path("alab/{stationId}")
    @Produces("application/json;charset=UTF-8")
    public Response getALabWeatherData(
            @PathParam("stationId") String stationId,
            @FormParam("timeZone") String timeZonePOST,
            @QueryParam("timeZone") String timeZoneGET,
            @FormParam("startDate") String startDatePOST,
            @QueryParam("startDate") String startDateGET,
            @FormParam("userName") String userNamePOST,
            @QueryParam("userName") String userNameGET,
            @FormParam("password") String passwordPOST,
            @QueryParam("password") String passwordGET) {
        List<WeatherObservation> observations;
        try {
            String timeZoneParam = timeZonePOST != null ? timeZonePOST : timeZoneGET != null ? timeZoneGET : "UTC";
            TimeZone timeZone = TimeZone.getTimeZone(timeZoneParam);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            format.setTimeZone(timeZone);
            String startDateParam = startDatePOST != null ? startDatePOST : startDateGET;
            Date startDate1 = format.parse(startDateParam);
            String userName = userNamePOST != null ? userNamePOST : userNameGET;
            String password = passwordPOST != null ? passwordPOST : passwordGET;
            if (userName != null && password != null) {
                observations = new ALabDataParser().getWeatherObservations(stationId, timeZone, startDate1, userName,
                        password);
            } else {
                observations = new ALabDataParser().getWeatherObservations(stationId, timeZone, startDate1);
            }
        } catch (ParseException | ParseWeatherDataException | NullPointerException ex) {
            return Response.serverError().entity(ex).build();
        }
        return Response.ok().entity(observations).build();
    }

    /**
     * Data from Oregon State University/IPPC's wide range of US weather stations http://uspest.org/
     *
     * @param stationId
     * @param timeZonePOST
     * @param timeZoneGET
     * @param startDatePOST
     * @param startDateGET
     * @return
     */
    @GET
    @Path("uspest/{stationId}")
    @Produces("application/json;charset=UTF-8")
    public Response getUSPestWeatherData(
            @PathParam("stationId") String stationId,
            @FormParam("timeZone") String timeZonePOST,
            @QueryParam("timeZone") String timeZoneGET,
            @FormParam("startDate") String startDatePOST,
            @QueryParam("startDate") String startDateGET) {
        List<WeatherObservation> observations;
        try {
            String timeZoneParam = timeZonePOST != null ? timeZonePOST : timeZoneGET != null ? timeZoneGET : "UTC";
            TimeZone timeZone = TimeZone.getTimeZone(timeZoneParam);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            format.setTimeZone(timeZone);
            String startDateParam = startDatePOST != null ? startDatePOST : startDateGET;
            Date startDate1 = format.parse(startDateParam);
            observations = new USPestDataParser().getWeatherObservations(stationId, timeZone, startDate1);
        } catch (ParseException | ParseWeatherDataException | NullPointerException ex) {
            return Response.serverError().entity(ex).build();
        }
        return Response.ok().entity(observations).build();
    }

    /**
     * Getting data from the Danish Meteorological Institute's PointWeb weather service
     *
     * @param stationId
     * @param timeZonePOST
     * @param timeZoneGET
     * @param startDatePOST
     * @param startDateGET
     * @param endDatePOST
     * @param endDateGET
     * @return
     */
    @GET
    @Path("dmipointweb/{stationId}")
    @Produces("application/json;charset=UTF-8")
    public Response getDMIPointWebWeatherData(
            @PathParam("stationId") String stationId,
            @FormParam("timeZone") String timeZonePOST,
            @QueryParam("timeZone") String timeZoneGET,
            @FormParam("startDate") String startDatePOST,
            @QueryParam("startDate") String startDateGET,
            @FormParam("endDate") String endDatePOST,
            @QueryParam("endDate") String endDateGET) {
        // Station is id'ed by decimal degrees position
        // Format is &stationId=long,lat. E.g. &stationId=9.583,56.488 (sample is from Follum, Denmark)
        List<WeatherObservation> observations;
        try {
            String timeZoneParam = timeZonePOST != null ? timeZonePOST : timeZoneGET != null ? timeZoneGET : "UTC";
            TimeZone timeZone = TimeZone.getTimeZone(timeZoneParam);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            format.setTimeZone(timeZone);
            String startDateParam = startDatePOST != null ? startDatePOST : startDateGET;
            Date startDate1 = format.parse(startDateParam);
            String endDateParam = endDatePOST != null ? endDatePOST : endDateGET;
            Date endDate1;
            if (endDateParam != null) {
                endDate1 = format.parse(endDateParam);
            } else {
                // Expecting endDate to be 6 days after today if not specified
                Calendar cal = Calendar.getInstance();
                // cal.setTime(SystemTime.getSystemTime());
                cal.add(Calendar.DATE, 6);
                endDate1 = cal.getTime();
            }
            String[] coordinates = stationId.split(",");
            Double longitude = Double.parseDouble(coordinates[0]);
            Double latitude = Double.parseDouble(coordinates[1]);
            observations = new DMIPointWebDataParser().getData(longitude, latitude, startDate1, endDate1);
            // observations.forEach(o->System.out.println(o));
        } catch (ParseException | NullPointerException ex) {
            return Response.serverError().entity(ex).build();
        }
        return Response.ok().entity(observations).build();
    }

    /**
     * 9-day forecasts from The Norwegian Met Office, using their locationforecast API
     * https://api.met.no/weatherapi/locationforecast/2.0/documentation
     *
     * @param longitude
     * @param latitude
     * @param altitude
     * @return
     */
    @GET
    @Path("forecasts/yr/")
    @Produces("application/json;charset=UTF-8")
    public Response getMetNoLocationForecastYrPath(
            @QueryParam("longitude") Double longitude,
            @QueryParam("latitude") Double latitude,
            @QueryParam("altitude") Double altitude) {
        return this.getMetNoLocationForecast(longitude, latitude, altitude);
    }

    @GET
    @Path("forecasts/metnolocationforecast/")
    @Produces("application/json;charset=UTF-8")
    public Response getMetNoLocationForecast(@QueryParam("longitude") Double longitude,
            @QueryParam("latitude") Double latitude,
            @QueryParam("altitude") Double altitude) {
        try {
            return Response.ok()
                    .entity(new MetNoLocationForecastProvider().getWeatherForecastsJSON(longitude, latitude, altitude))
                    .build();
        } catch (ParseException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    /**
     * 
     * @param longitude
     * @param latitude
     * @param transformTimeZone if set: the UTC midnight timestamps are transformed to midnight in the local timezone.
     *        E.g. Europe/Oslo
     * @return daily forecasts for TM, TN, TX and RR with UTC midnight timestamps (unless transformTimeZone is set)
     */
    @GET
    @Path("forecasts/metnosubseasonal/")
    @Produces("application/json;charset=UTF-8")
    @Operation(
            summary = "Get Met Norway's subseasonal forecast in the VIPS Json format",
            description = "The subseasonal forecast is described here: https://api.met.no/weatherapi/subseasonal/1.0/documentation")
    @APIResponse(
            responseCode = "200",
            description = "",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = WeatherObservation[].class)))
    @APIResponse(
            responseCode = "422",
            description = "The requested location is outside of the service's area")
    @APIResponse(
            responseCode = "500",
            description = "A server error occured.")
    public Response getMetNoSubSeasonalForecast(
            @QueryParam("longitude") Double longitude,
            @QueryParam("latitude") Double latitude,
            @Parameter(description = "Get the data with midnight set in the requested timezone (E.g. Europe/Oslo). Default is UTC",
                    required = false) @QueryParam("transformTimeZone") String transformTimeZone) {
        try {
            List<WeatherObservation> observations =
                    new MetNoSubSeasonalForecastProvider().getSubSeasonalForecasts(longitude, latitude);
            if (transformTimeZone != null) {
                observations.forEach(obs -> {
                    Instant t = obs.getTimeMeasured().toInstant();
                    ZonedDateTime zt = t.atZone(ZoneId.of(transformTimeZone));
                    ZonedDateTime ztAtMidnight = zt.with(LocalTime.MIDNIGHT);
                    t = ztAtMidnight.toInstant();
                    obs.setTimeMeasured(Date.from(t));
                });
            }
            return Response.ok().entity(observations).build();
        } catch (ParseWeatherDataException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } catch (OutOfAreaException ex) {
            return Response.status(422).entity(ex.getMessage()).build();
        }
    }

    /**
     * Get 36 hour forecasts from FMI (The Finnish Meteorological Institute), using their OpenData services at
     * https://en.ilmatieteenlaitos.fi/open-data
     *
     * @param longitude
     * @param latitude
     * @param altitude
     * @return
     */
    @GET
    @Path("forecasts/fmi/")
    @Produces("application/json;charset=UTF-8")
    public Response getFMIForecasts(
            @QueryParam("longitude") Double longitude,
            @QueryParam("latitude") Double latitude,
            @QueryParam("altitude") Double altitude) {
        FmiOpenDataAccess dA = new FmiOpenDataAccess();
        FmiOpenDataForecastParser fP = new FmiOpenDataForecastParser();
        String forecastXML = dA.getForecastData(longitude, latitude);
        List<WeatherObservation> forecastObs = fP.getVIPSWeatherObservations(forecastXML);
        return Response.ok().entity(forecastObs).build();
    }

    @GET
    @Path("grid/openmeteo/{lon_lat}")
    @Produces("application/json;charset=UTF-8")
    public Response getOpenMeteoWeatherDataByPath(
            @PathParam("lon_lat") String lonLatStr,
            @FormParam("timeZone") String timeZonePOST,
            @QueryParam("timeZone") String timeZoneGET,
            @FormParam("startDate") String startDatePOST,
            @QueryParam("startDate") String startDateGET,
            @FormParam("endDate") String endDatePOST,
            @QueryParam("endDate") String endDateGET,
            @FormParam("elementMeasurementTypes[]") String[] elementMeasurementTypesPOST,
            @QueryParam("elementMeasurementTypes[]") String[] elementMeasurementTypesGET,
            @FormParam("logIntervalId") Integer logIntervalIdPOST,
            @QueryParam("logIntervalId") Integer logIntervalIdGET) {
        try {
            String[] lonLat = lonLatStr.split("_");
            Double longitude = Double.parseDouble(lonLat[0]);
            Double latitude = Double.parseDouble(lonLat[1]);
            String timeZoneParam = timeZonePOST != null ? timeZonePOST : timeZoneGET != null ? timeZoneGET : "UTC";
            String startDateParam = startDatePOST != null ? startDatePOST : startDateGET;
            String endDateParam = endDatePOST != null ? endDatePOST : endDateGET != null ? endDateGET : null;
            String[] elementMeasurementTypes =
                    (elementMeasurementTypesPOST != null && elementMeasurementTypesPOST.length > 0)
                            ? elementMeasurementTypesPOST
                            : elementMeasurementTypesGET;
            Integer logIntervalId =
                    logIntervalIdPOST != null ? logIntervalIdPOST : logIntervalIdGET != null ? logIntervalIdGET : null;
            return this.getOpenMeteoWeatherData(longitude, latitude, timeZoneParam, startDateParam, endDateParam,
                    elementMeasurementTypes, logIntervalId);

        } catch (NumberFormatException ex) {
            return Response.serverError().entity(ex).build();
        }
    }

    @GET
    @Path("grid/openmeteo/")
    @Produces("application/json;charset=UTF-8")
    public Response getOpenMeteoWeatherData(
            @QueryParam("longitude") Double longitude,
            @QueryParam("latitude") Double latitude,
            @QueryParam("timeZone") String timeZoneStr,
            @QueryParam("startDate") String startDateStr,
            @QueryParam("endDate") String endDateStr,
            @QueryParam("elementMeasurementTypes[]") String[] elementMeasurementTypesInput,
            @QueryParam("logIntervalId") Integer logIntervalId) {

        List<WeatherObservation> observations;

        try {
            TimeZone timeZone = TimeZone.getTimeZone(timeZoneStr != null ? timeZoneStr : "UTC");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            format.setTimeZone(timeZone);
            Date startDate = format.parse(startDateStr);
            Date endDate = endDateStr != null ? format.parse(endDateStr) : null;
            List<String> elementMeasurementTypes = Arrays.asList(elementMeasurementTypesInput);

            observations = new OpenMeteoParser().getWeatherObservations(longitude, latitude, timeZone, startDate,
                    endDate, elementMeasurementTypes, logIntervalId);

        } catch (ParseException | ParseWeatherDataException | WeatherObservationListException
                | InvalidAggregationTypeException ex) {
            return Response.serverError().entity(ex).build();
        }

        return Response.ok().entity(observations).build();

    }

    /**
     * Data from Agrometeo, FIDL Switzerland https://vitimeteo.ch/
     * 
     * @param stationId
     * @param startDate
     * @param endDate
     * @param timezone
     * @param logIntervalId,
     * @param elementMeasurementTypes
     * @return
     * @throws java.text.ParseException
     * @throws java.io.IOException
     * @throws java.net.URISyntaxException
     * @throws no.nibio.vips.weather.proxy.parsers.ParseWeatherDataException
     */
    @GET
    @Path("vitimeteo/{stationId}")

    @Produces("application/json;charset=UTF-8")
    public Response getFIBLWeatherData(
            @PathParam("stationId") String stationId,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("timeZone") String timezone,
            @QueryParam("elementMeasurementTypes[]") String[] elementMeasurementTypes,
            @QueryParam("logIntervalId") String logIntervalId)
            throws ParseException, IOException, URISyntaxException, ParseWeatherDataException {
        List<WeatherObservation> observations;

        try {

            String timeZoneParam = timezone != null ? timezone : "UTC";
            String startDateParam =
                    startDate != null ? startDate : new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String endDateParam = endDate != null ? endDate : new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            observations = new VitiMeteoParser().getWeatherObservations(stationId, startDateParam, endDateParam,
                    timeZoneParam, logIntervalId, Arrays.asList(elementMeasurementTypes));

        } catch (NullPointerException ex) {
            return Response.serverError().entity(ex).build();
        }
        return Response.ok().entity(observations).build();
    }
}
