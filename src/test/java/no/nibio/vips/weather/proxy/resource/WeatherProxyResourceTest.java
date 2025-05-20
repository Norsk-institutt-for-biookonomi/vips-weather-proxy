package no.nibio.vips.weather.proxy.resource;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(WeatherProxyResource.class)

public class WeatherProxyResourceTest {

    @Test
    public void testFMIResource()
    {
        // Find a reasonable time to get data from
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusDays(3);
        
        given().when()
        .get("fmi/101104?timeZone=Europe/Helsinki&startDate=" + start + "&endDate=" + now + "&elementMeasurementTypes[]=Q0")
        .then().statusCode(OK.getStatusCode())
        .body("$.size()", not(0));
    }

    @Test
    public void testMetNoLocationForecastResource()
    {
        given().when()
        .get("forecasts/yr?longitude=10.961&latitude=50.109&altitude=100")
        .then().statusCode(OK.getStatusCode())
        .body("$.size()", not(0));
    }

    @Test
    public void testDMIPointWeb()
    {
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusDays(3);
        given().when()
        .get("dmipointweb/9.583,56.488?startDate=" + start + "&endDate=" + now)
        .then().statusCode(OK.getStatusCode())
        .body("$.size()", not(0));
    }

    @Test
    public void testTahmoOpen()
    {
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusDays(3);
        given().when()
        .get("tahmoopen?stationId=TA00390&startDate=" + start + "&endDate=" + now + "&variable=te")
        .then().statusCode(OK.getStatusCode())
        .body("$.size()", not(0));
    }

    /* 
    @Test
    public void testFMIForecasts()
    {
        given().when()
        .get("forecasts/fmi?longitude=22.35&latitude=61.25&altitude=0")
        .then().statusCode(OK.getStatusCode())
        .body("$.size()", not(0));
    }*/

    @Test
    public void testOpenMeteo()
    {
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusDays(2);
        LocalDate end = now.plusDays(2);
        given().when()
        .get("grid/openmeteo/?timeZone=Europe%2FOslo&startDate=" + start + "&endDate=" + end + "&elementMeasurementTypes[]=TM&logIntervalId=1&latitude=59.660468&longitude=10.781989")
        .then().statusCode(OK.getStatusCode())
        .body("$.size()", not(0));
    }

    @Test public void testMetNoSubSeasonalForecast(){
        given().when()
        .get("forecasts/metnosubseasonal/?latitude=59.660468&longitude=10.781989")
        .then().statusCode(OK.getStatusCode())
        .body("$.size()", is(104));

        given().when()
        .get("forecasts/metnosubseasonal/?latitude=39.660468&longitude=10.781989")
        .then().statusCode(422);
    }


}
