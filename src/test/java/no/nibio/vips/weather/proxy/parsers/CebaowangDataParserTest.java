/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.nibio.vips.weather.proxy.parsers;

import java.text.SimpleDateFormat;
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
public class CebaowangDataParserTest {
    
    public CebaowangDataParserTest() {
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
     * Test of getWeatherObservations method, of class CebaowangDataParser.
     */
    //@Test
    /*
    public void testGetWeatherObservations() throws Exception {
        System.out.println("getWeatherObservations");
        String stationID = "token=0819208daf3684b6e1b2f7d9b0e1bc64&deviceId=YBQ0000053";
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Shanghai");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(timeZone);
        Date startDate = format.parse("2019-05-25");
        Date endDate = format.parse("2019-05-26");
        CebaowangDataParser instance = new CebaowangDataParser();
        List<WeatherObservation> expResult = null;
        List<WeatherObservation> result = instance.getWeatherObservations(stationID, timeZone, startDate, endDate);
        /*if(result != null)
        {
            result.forEach(o->System.out.println(o));
        }
        assertNotNull(result);
        
    }*/

    /**
     * Test of getParsedObservations method, of class CebaowangDataParser.
     */
    //@Test
    /*public void testGetParsedObservations() throws Exception {
        System.out.println("getParsedObservations");
        String jsonTxt = "";
        TimeZone timeZone = null;
        CebaowangDataParser instance = new CebaowangDataParser();
        List<WeatherObservation> expResult = null;
        List<WeatherObservation> result = instance.getParsedObservations(jsonTxt, timeZone);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/
    
}
