/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.nibio.vips.weather.proxy.parsers;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import no.nibio.vips.entity.WeatherObservation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author treinar
 */
public class MetosAPIDataParserTest {
    
    public MetosAPIDataParserTest() {
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
     * Test of getAccessCode method, of class MetosAPIDataParser.
     */
    //@Test
    /*
    public void testGetAccessCode() throws Exception {
        System.out.println("getAccessCode");
        MetosAPIDataParser instance = new MetosAPIDataParser();
        String expResult = "";
        String result = instance.getAccessCode();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of getToken method, of class MetosAPIDataParser.
     */
    //@Test
    /*
    public void testGetToken() throws Exception {
        System.out.println("getToken");
        MetosAPIDataParser instance = new MetosAPIDataParser();
        String expResult = "";
        String result = instance.getToken();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of getParsedObservations method, of class MetosAPIDataParser.
     */
    //@Test
    /*
    public void testGetParsedObservations() throws Exception {
        System.out.println("getParsedObservations");
        String jsonTxt = "";
        TimeZone timeZone = null;
        MetosAPIDataParser instance = new MetosAPIDataParser();
        List<WeatherObservation> expResult = null;
        List<WeatherObservation> result = instance.getParsedObservations(jsonTxt, timeZone);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of getWeatherObservations method, of class MetosAPIDataParser.
     */
    //@Test
    /*
    public void testGetWeatherObservations() throws Exception {
        System.out.println("getWeatherObservations");
        String stationId = "";
        TimeZone timeZone = null;
        Date startDate = null;
        MetosAPIDataParser instance = new MetosAPIDataParser();
        List<WeatherObservation> expResult = null;
        List<WeatherObservation> result = instance.getWeatherObservations(stationId, timeZone, startDate);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of testAPI method, of class MetosAPIDataParser.
     *
    @Test
    public void testTestAPI() throws Exception {
        System.out.println("testAPI");
        MetosAPIDataParser instance = new MetosAPIDataParser();
        System.setProperty("no.nibio.vips.logic.weather.FIELDCLIMATE_API_CLIENT_ID","XXXXXX");
        System.setProperty("no.nibio.vips.logic.weather.FIELDCLIMATE_API_CLIENT_SECRET","XXXXXXXx");
        System.setProperty("no.nibio.vips.logic.weather.FIELDCLIMATE_API_USERNAME","XXXXXXxx");
        System.setProperty("no.nibio.vips.logic.weather.FIELDCLIMATE_API_PASSWORD","XXXXXXXX");
        instance.testAPI("01208704");
        // TODO review the generated test code and remove the default call to fail.
        
    }*/
    
}
