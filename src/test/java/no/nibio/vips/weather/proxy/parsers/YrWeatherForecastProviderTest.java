/*
 * Copyright (c) 2015 NIBIO <http://www.nibio.no/>. 
 * 
 * This file is part of VIPSLogic.
 * VIPSLogic is free software: you can redistribute it and/or modify
 * it under the terms of the NIBIO Open Source License as published by 
 * NIBIO, either version 1 of the License, or (at your option) any
 * later version.
 * 
 * VIPSLogic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * NIBIO Open Source License for more details.
 * 
 * You should have received a copy of the NIBIO Open Source License
 * along with VIPSLogic.  If not, see <http://www.nibio.no/licenses/>.
 * 
 */
package no.nibio.vips.weather.proxy.parsers;

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
public class YrWeatherForecastProviderTest {
    
    public YrWeatherForecastProviderTest() {
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
     * Test of getWeatherForecasts method, of class YrWeatherForecastProvider.
     */
    @Test
    public void testGetWeatherForecasts() throws Exception {
        System.out.println("getWeatherForecasts");
        // Test Norwegian location
        Double longitude = 10.7207;
        Double latitude = 59.9426;
        Double altitude = 70.0;
        YrWeatherForecastProvider instance = new YrWeatherForecastProvider();
        List<WeatherObservation> result = instance.getWeatherForecastsXML(longitude, latitude, altitude);
        assertNotNull(result);
        longitude = 38.0866;
        latitude = -1.4718;
        result = instance.getWeatherForecastsXML(longitude, latitude, altitude);
        assertNotNull(result);
        //result.forEach(o->System.out.println(o.toString()));
    }
    
}
