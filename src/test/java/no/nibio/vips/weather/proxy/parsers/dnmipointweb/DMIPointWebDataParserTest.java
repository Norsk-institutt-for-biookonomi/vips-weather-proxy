/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.nibio.vips.weather.proxy.parsers.dnmipointweb;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import java.util.Date;
import java.util.List;
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
public class DMIPointWebDataParserTest {
    
    public DMIPointWebDataParserTest() {
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
     * Test of getData method, of class DMIPointWebDataParser.
     */
    @Test
    public void testGetData() {
        System.out.println("getData");
        Double longitude = 10.333252;
        Double latitude = 57.179002;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Oslo"));
        cal.set(2019, Calendar.JUNE, 6, 0, 0, 0);
        Date dateFrom = cal.getTime();
        cal.add(Calendar.MONTH, 3);
        Date dateTo = cal.getTime();
        DMIPointWebDataParser instance = new DMIPointWebDataParser();
        
        List<WeatherObservation> result = instance.getData(longitude, latitude, dateFrom, dateTo);
        //result.forEach(o->System.out.println(o.toString()));
        assertNotNull(result);
        
    }
    
}
