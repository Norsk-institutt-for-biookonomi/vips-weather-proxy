/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.nibio.vips.weather.proxy.parsers;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Calendar;
import no.nibio.vips.entity.WeatherObservation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class OpenMeteoParserTest {
    
    public OpenMeteoParserTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of getWeatherObservations method, of class ALabDataParser.
     */
    @Test
    public void testOpenMeteoParser() throws Exception {
        System.out.println("testOpenMeteo");
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Oslo");
        Double longitude = 10.7207;
        Double latitude = 59.9426;
        Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS);
        Date startDate = Date.from(twoDaysAgo);
        Date endDate = Date.from(tomorrow);
        Integer logIntervalId = 2;
        List <String> elementMeasurementTypes = Arrays.asList("TM", "RR", "BT", "Q0");
        
        OpenMeteoParser instance = new OpenMeteoParser();
        List<WeatherObservation> result = instance.getWeatherObservations(longitude, latitude, timeZone, startDate, endDate, elementMeasurementTypes, logIntervalId);
        //result.forEach(obs->System.out.println(obs));
        assertNotNull(result);
        
    }
    
}