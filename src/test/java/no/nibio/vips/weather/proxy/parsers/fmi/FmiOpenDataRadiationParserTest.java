/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.nibio.vips.weather.proxy.parsers.fmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import no.nibio.vips.entity.WeatherObservation;


/**
 *
 * @author treinar
 */
public class FmiOpenDataRadiationParserTest {
    
    public FmiOpenDataRadiationParserTest() {
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
     * Test of getVIPSWeatherObservations method, of class FmiOpenDataRadiationParser.
     */
    @Test
    @Disabled
    public void testGetAsJson() {
        System.out.println("getAsJson");
        String FMIXMLString = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/fmi_response_radiation.xml"), StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining("\n"));
        //System.out.println("FMIXMLString=" +FMIXMLString);
        FmiOpenDataRadiationParser instance = new FmiOpenDataRadiationParser();
        List<WeatherObservation> result = instance.getVIPSWeatherObservations(FMIXMLString);
        assertNotNull(result);
        assertEquals(96, result.size());

    }
    
}
