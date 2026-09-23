/*
 * Copyright (c) 2026 NIBIO <https://www.nibio.no/>. 
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
package no.nibio.vips.weather.proxy.parsers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.BufferedInputStream;
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
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class MetNoLocationForecastProviderTest {
    
    public MetNoLocationForecastProviderTest() {
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
     * Test of getWeatherForecastsHourlyFromMetnoData method, of class MetNoLocationForecastProvider.
     */
    @Test
    public void testGetWeatherForecastsHourlyFromMetnoData() throws Exception {
        System.out.println("getWeatherForecastsHourlyFromMetnoData");
        // Read the timeseries from a file, and pass it to the method to test
        BufferedInputStream inputStream = new BufferedInputStream(this.getClass().getResourceAsStream("/metnolocationforecastresult.json"));
        JsonFactory f = new MappingJsonFactory();
        JsonParser jp = f.createParser(inputStream);
        JsonNode all = jp.readValueAsTree();
        // The file is located in src/test/resources/metnolocationforecastresult.json, and contains a sample response from the Met.no API for a location in Norway
        JsonNode timeseries = all.get("properties").get("timeseries");
        MetNoLocationForecastProvider instance = new MetNoLocationForecastProvider();
        List<WeatherObservation> result = instance.getWeatherForecastsHourlyFromMetnoData(timeseries);
        assertNotNull(result);
        assertEquals(318, result.size());
        WeatherObservation lastObs = result.get(result.size() - 1);
        assertEquals(lastObs.getElementMeasurementTypeId(), "FF");
        assertEquals(lastObs.getValue(), 2.9);
        
    }


    /**
     * Test of getWeatherForecastsDailyFromMetnoData method, of class MetNoLocationForecastProvider.
     */
    @Test
    public void testGetWeatherForecastsDailyFromMetnoData() throws Exception {
        System.out.println("getWeatherForecastsDailyFromMetnoData");
        // Read the timeseries from a file, and pass it to the method to test
        BufferedInputStream inputStream = new BufferedInputStream(this.getClass().getResourceAsStream("/metnolocationforecastresult.json"));
        JsonFactory f = new MappingJsonFactory();
        JsonParser jp = f.createParser(inputStream);
        JsonNode all = jp.readValueAsTree();
        // The file is located in src/test/resources/metnolocationforecastresult.json, and contains a sample response from the Met.no API for a location in Norway
        JsonNode timeseries = all.get("properties").get("timeseries");
        MetNoLocationForecastProvider instance = new MetNoLocationForecastProvider();
        List<WeatherObservation> result = instance.getWeatherForecastsDailyFromMetnoData(timeseries);
        assertNotNull(result);
        //System.out.println(result);
        assertEquals(54, result.size());
        WeatherObservation lastObs = result.get(result.size() - 1);
        assertEquals(lastObs.getElementMeasurementTypeId(), "FM2");
        assertEquals(lastObs.getValue(), 1.75);
        
    }
}
