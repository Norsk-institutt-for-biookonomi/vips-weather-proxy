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

/**
 * Get weather data from the Finnish Meteorological Institute's open data API
 * Read the docs here: https://en.ilmatieteenlaitos.fi/open-data 
 * 
 * The parser is a modified version of Markku Koistinen's parser made in the
 * SpotIT project
 * 
 * @author Markku Koistinen <markku.koistinen@luke.fi>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class FmiIntermediator {
    
    public FmiIntermediator() {}
    
    public String getTemporalData_fmi(String fmiSid, String startDateTime, String endDateTime) {
        String response = "";
        FmiOpenDataAccess fmiAccess = new FmiOpenDataAccess();
        startDateTime = startDateTime.replace("T", " ");
        startDateTime = startDateTime.replace("Z", "");
        endDateTime = endDateTime.replace("T", " ");
        endDateTime = endDateTime.replace("Z", "");
        response = fmiAccess.getTemporalData(fmiSid, startDateTime, endDateTime);
        //System.out.println(fmiSid);
        //System.out.println(startDateTime);
        //System.out.println(endDateTime);
        return response;
    }
    
    public String getTemporalData_vips(String fmiSid, String startDateTime, String endDateTime) {
        String response = "";
        FmiOpenDataAccess fmiAccess = new FmiOpenDataAccess();
        startDateTime = startDateTime.replace("T", " ");
        startDateTime = startDateTime.replace("Z", "");
        endDateTime = endDateTime.replace("T", " ");
        endDateTime = endDateTime.replace("Z", "");
        response = fmiAccess.getTemporalData_prototype(fmiSid, startDateTime, endDateTime);
        //System.out.println(fmiSid);
        //System.out.println(startDateTime);
        //System.out.println(endDateTime);
        return response;
    }
    
}
