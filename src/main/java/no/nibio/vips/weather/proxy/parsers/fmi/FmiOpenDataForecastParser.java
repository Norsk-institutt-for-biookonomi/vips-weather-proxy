/*
 * Copyright (c) 2020 NIBIO <https://www.nibio.no/>.
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

package no.nibio.vips.weather.proxy.parsers.fmi;

import no.nibio.vips.entity.WeatherObservation;
import no.nibio.vips.util.WeatherElements;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Converts FMI XML string to VIPS type Weather observations
 *
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 * @copyright 2020 <a href="http://www.nibio.no/">NIBIO</a>
 */
public class FmiOpenDataForecastParser {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FmiOpenDataForecastParser.class);

    public List<WeatherObservation> getVIPSWeatherObservations(String fmiXMLString) {
        List<WeatherObservation> retVal = new ArrayList<>();
        try {
            // Build the DOM structure from XML string
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(fmiXMLString));
            Document doc = dBuilder.parse(is);

            // The included parameters are given in correct order in these elements
            // As per the Java documentation the elements are arranged in the document's order
            // https://docs.oracle.com/en/java/javase/11/docs/api/java.xml/org/w3c/dom/Document.html#getElementsByTagName(java.lang.String)
            NodeList parameterSet = doc.getElementsByTagName("swe:field");
            // The epoch timestamps (and positions) are given as lines in this element
            String[] positionsAndTimeStamps =
                doc.getElementsByTagName("gmlcov:positions").item(0).getTextContent().split("\\r?\\n");
            // The corresponding values are given as columns in rows(lines) in this element
            String[] valueLines =
                doc.getElementsByTagName("gml:doubleOrNilReasonTupleList").item(0).getTextContent().split("\\r?\\n");

            // Run through each timestamp
            for (int i = 0; i < positionsAndTimeStamps.length; i++) {
                if (positionsAndTimeStamps[i].trim().isBlank() || valueLines[i].contains("NaN")) {
                    continue;
                }


                String[] rawValues = valueLines[i].trim().split("\\s+");
                // Iterate the parameters, create new WeatherObservation for each of them
                for (int j = 0; j < parameterSet.getLength(); j++) {
                    WeatherObservation o = new WeatherObservation();
                    Element e = (Element) parameterSet.item(j);
                    String paramName = e.getAttribute("name");
                    switch (paramName) {
                        case "Temperature":
                            o.setElementMeasurementTypeId(WeatherElements.TEMPERATURE_MEAN);
                            break;
                        case "Humidity":
                            o.setElementMeasurementTypeId(WeatherElements.RELATIVE_HUMIDITY_MEAN);
                            break;
                        case "WindSpeedMS":
                            o.setElementMeasurementTypeId(WeatherElements.WIND_SPEED_2M);
                            break;
                        case "DewPoint":
                            o.setElementMeasurementTypeId(WeatherElements.DEW_POINT_TEMPERATURE);
                            break;
                        case "Precipitation1h":
                            o.setElementMeasurementTypeId(WeatherElements.PRECIPITATION);
                            break;
                        case "radiationglobal":
                            o.setElementMeasurementTypeId(WeatherElements.GLOBAL_RADIATION);
                            break;
                        default:
                            continue;
                    }

                    o.setValue(Double.valueOf(rawValues[j]));
                    o.setLogIntervalId(WeatherObservation.LOG_INTERVAL_ID_1H);
                    o.setTimeMeasured(Date.from(
                        Instant.ofEpochSecond(Long.valueOf(positionsAndTimeStamps[i].trim().split("\\s+")[2]))));
                    retVal.add(o);
                }

            }

        } catch (SAXException | IOException | ParserConfigurationException ex) {
            LOGGER.error("Unable to get VIPSWeatherObservations from Finnish string", ex);
            return null;
        }
        return retVal;
    }
}
