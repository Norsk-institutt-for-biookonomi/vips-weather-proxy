/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.nibio.vips.weather.proxy.parsers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import no.nibio.vips.entity.WeatherObservation;

/**
 *
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class ALabDataParserTest {
    
    public ALabDataParserTest() {
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
    @Disabled
    public void testGetWeatherObservations() throws Exception {
        System.out.println("getWeatherObservations");
        String stationID = "00000000000BAD92E601";
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Helsinki");
        Instant eightWeeksAgo = Instant.now().minus(56, ChronoUnit.DAYS);
        Date startDate = Date.from(eightWeeksAgo);
        String userName = "nsl";
        String password = "yE9H9reV";
        ALabDataParser instance = new ALabDataParser();
        List<WeatherObservation> result = instance.getWeatherObservations(stationID, timeZone, startDate, userName, password);
        //result.forEach(obs->System.out.println(obs));
        assertNotNull(result);
        
    }
    
}
