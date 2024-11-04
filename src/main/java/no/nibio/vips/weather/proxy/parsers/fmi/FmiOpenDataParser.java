/*
 * Copyright (c) 2020 NIBIO <http://www.nibio.no/>. 
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
 * 
 */

package no.nibio.vips.weather.proxy.parsers.fmi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
public class FmiOpenDataParser {
    
    public FmiOpenDataParser() {}
    
    public String getAsJSON_prototype(String fmiDataAsXML, String temporalFactor) {
        
        //DateTimeFunctions myDateTime = new DateTimeFunctions();
        
        //System.out.println("PARSE START: " + myDateTime.getNowStandardWithMilliseconds());
        
        if (fmiDataAsXML.contains("numberReturned=\"0\"")) {
            return "";
        }
        
        String response = "[";
        
        String firstTemporalFactorAsString = temporalFactor;
        String secondTemporalFactorAsString = "";
        
        switch (firstTemporalFactorAsString) {
            case "00":  secondTemporalFactorAsString = "30";
                        break;
            case "10":  secondTemporalFactorAsString = "40";
                        break;
            case "20":  secondTemporalFactorAsString = "50";
                        break;
            case "30":  secondTemporalFactorAsString = "00";
                        break;
            case "40":  secondTemporalFactorAsString = "10";
                        break;
            case "50":  secondTemporalFactorAsString = "20";
                        break;
        }
        
        String dataPod;
        
        DateTimeFunctions dateTime = new DateTimeFunctions();
        
        int observationCount;
        int variableCount;
                
        String [] groupTokens;
        String [] variableTokens;
        
        String gmlCovPositions;
        List gmlCovPositionsAsList = new ArrayList();
        String gmlDateTime;
        
        String tupleList;
        List tupleListAsList = new ArrayList();
        
        String dataRecord;
        List dataRecordAsList = new ArrayList();
        String variableKey;
        //System.out.println(fmiDataAsXML);
        //Coverage points and timestamps
        gmlCovPositions = fmiDataAsXML.substring(fmiDataAsXML.indexOf("<gmlcov:positions>") + 18, fmiDataAsXML.indexOf("</gmlcov:positions>"));
        gmlCovPositions = gmlCovPositions.trim();
        groupTokens = gmlCovPositions.split("                ");
        observationCount = groupTokens.length;
        for (String groupToken: groupTokens) {
            variableTokens = groupToken.split("  ");
            gmlCovPositionsAsList.add(dateTime.getUnixTimeAsISO(Long.parseLong(variableTokens[1])));
        }
        
        //Variable values
        tupleList = fmiDataAsXML.substring(fmiDataAsXML.indexOf("<gml:doubleOrNilReasonTupleList>") + 32, fmiDataAsXML.indexOf("</gml:doubleOrNilReasonTupleList>"));
        tupleList = tupleList.trim();
        groupTokens = tupleList.split("                 ");
        variableCount = groupTokens.length;
        for (String groupToken: groupTokens) {
            tupleListAsList.add(groupToken);
        }
        
        //Variable Keys
        dataRecord = fmiDataAsXML.substring(fmiDataAsXML.indexOf("<swe:DataRecord>") + 16, fmiDataAsXML.indexOf("</swe:DataRecord>"));
        dataRecord = dataRecord.trim();
        groupTokens = dataRecord.split("              ");
        variableCount = groupTokens.length;
        for (String groupToken: groupTokens) {
            variableKey = groupToken.substring(groupToken.indexOf("<swe:field name=\"") + 17, groupToken.indexOf("\"  "));
            dataRecordAsList.add(variableKey);
        }
        
        //Combine
        Iterator valueIterator = tupleListAsList.iterator();
        Iterator keyIterator;
        Iterator dateIterator = gmlCovPositionsAsList.iterator();
        boolean skip = true;
        Set debugVariableKeys = new HashSet<>();
        while (valueIterator.hasNext()) {
            dataPod = "";
            variableTokens = valueIterator.next().toString().split(" ");
            keyIterator = dataRecordAsList.iterator();
            gmlDateTime = dateIterator.next().toString();
            //if (gmlDateTime.substring(14).equals(firstTemporalFactorAsString + ":00") || gmlDateTime.substring(14).equals(secondTemporalFactorAsString + ":00")) {
            if (gmlDateTime.substring(14).equals(firstTemporalFactorAsString + ":00")) {
                skip = false;
            }
            gmlDateTime = gmlDateTime.replace(" ", "T");
            gmlDateTime += "+00:00";
            
            for (String variableValue: variableTokens) {
                variableKey = keyIterator.next().toString();
                debugVariableKeys.add(variableKey);
                if (variableKey.equals("t2m") || variableKey.equals("rh") || variableKey.equals("r_1h") || variableKey.equals("ws_10min")) {
                    switch (variableKey) {
                        case "t2m": variableKey = "TM";
                                    break;
                        case "rh": variableKey = "UM";
                                    break;
                        case "r_1h": variableKey = "RR";
                                    break;
                        case "ws_10min": variableKey = "FF2";
                                    break;
                    }
                    dataPod = "{\"timeMeasured\":\"" + gmlDateTime + "\", \"elementMeasurementTypeId\":\"" + variableKey + "\", \"logIntervalId\":1, \"value\":" + variableValue + "}, ";
                    if (skip == false) {
                        response += dataPod;
                    }
                }
                //System.out.println(variableKey + ":" + variableValue);
            }
            skip = true;
            //System.out.println("UTC:" + gmlDateTime);
            dataPod = "";
        }
        //System.out.println("VariableKeys: ");
        //debugVariableKeys.forEach(str->System.out.println(str));
        response += "]";
        response = response.replace(", ]", "]");
        //NaN HACK
        response = response.replace("NaN", "null");
        //System.out.println(gmlCovPositions);
        //System.out.println(tupleList);
        //System.out.println(dataRecord);
        //System.out.println(response);
        //System.out.println("PARSE END: " + myDateTime.getNowStandardWithMilliseconds());
        return response;
    }
    
    public String getAsJSON(String fmiDataAsXML, String temporalFactor) {
        
        if (fmiDataAsXML.contains("numberReturned=\"0\"")) {
            return "";
        }
        
        String response = "[";
        
        String firstTemporalFactorAsString = temporalFactor;
        String secondTemporalFactorAsString = "";
        
        switch (firstTemporalFactorAsString) {
            case "00":  secondTemporalFactorAsString = "30";
                        break;
            case "10":  secondTemporalFactorAsString = "40";
                        break;
            case "20":  secondTemporalFactorAsString = "50";
                        break;
            case "30":  secondTemporalFactorAsString = "00";
                        break;
            case "40":  secondTemporalFactorAsString = "10";
                        break;
            case "50":  secondTemporalFactorAsString = "20";
                        break;
        }
        
        String dataPod;
        
        DateTimeFunctions dateTime = new DateTimeFunctions();
        
        int observationCount;
        int variableCount;
                
        String [] groupTokens;
        String [] variableTokens;
        
        String gmlCovPositions;
        List gmlCovPositionsAsList = new ArrayList();
        String gmlDateTime;
        
        String tupleList;
        List tupleListAsList = new ArrayList();
        
        String dataRecord;
        List dataRecordAsList = new ArrayList();
        String variableKey;
        
        //Coverage points and timestamps
        gmlCovPositions = fmiDataAsXML.substring(fmiDataAsXML.indexOf("<gmlcov:positions>") + 18, fmiDataAsXML.indexOf("</gmlcov:positions>"));
        gmlCovPositions = gmlCovPositions.trim();
        groupTokens = gmlCovPositions.split("                ");
        observationCount = groupTokens.length;
        for (String groupToken: groupTokens) {
            variableTokens = groupToken.split("  ");
            gmlCovPositionsAsList.add(dateTime.getUnixTimeAsISO(Long.parseLong(variableTokens[1])));
        }
        
        //Variable values
        tupleList = fmiDataAsXML.substring(fmiDataAsXML.indexOf("<gml:doubleOrNilReasonTupleList>") + 32, fmiDataAsXML.indexOf("</gml:doubleOrNilReasonTupleList>"));
        tupleList = tupleList.trim();
        groupTokens = tupleList.split("                 ");
        variableCount = groupTokens.length;
        for (String groupToken: groupTokens) {
            tupleListAsList.add(groupToken);
        }
        
        //Variable Keys
        dataRecord = fmiDataAsXML.substring(fmiDataAsXML.indexOf("<swe:DataRecord>") + 16, fmiDataAsXML.indexOf("</swe:DataRecord>"));
        dataRecord = dataRecord.trim();
        groupTokens = dataRecord.split("              ");
        variableCount = groupTokens.length;
        for (String groupToken: groupTokens) {
            variableKey = groupToken.substring(groupToken.indexOf("<swe:field name=\"") + 17, groupToken.indexOf("\"  "));
            dataRecordAsList.add(variableKey);
        }
        
        //Combine
        Iterator valueIterator = tupleListAsList.iterator();
        Iterator keyIterator;
        Iterator dateIterator = gmlCovPositionsAsList.iterator();
        boolean skip = true;
        while (valueIterator.hasNext()) {
            dataPod = "{";
            variableTokens = valueIterator.next().toString().split(" ");
            keyIterator = dataRecordAsList.iterator();
            for (String variableValue: variableTokens) {
                variableKey = keyIterator.next().toString();
                //if (variableKey.equals("t2m") || variableKey.equals("r_1h") || variableKey.equals("ws_10min")) {
                    dataPod += "\"" + variableKey + "\":" + variableValue + ", ";
                //}
                //System.out.println(variableKey + ":" + variableValue);
            }
            gmlDateTime = dateIterator.next().toString();
            if (gmlDateTime.substring(14).equals(firstTemporalFactorAsString + ":00") || gmlDateTime.substring(14).equals(secondTemporalFactorAsString + ":00")) {
                skip = false;
            }
            gmlDateTime = gmlDateTime.replace(" ", "T");
            gmlDateTime += "Z";
            dataPod += "\"UTC\":\"" + gmlDateTime + "\"},";
            if (skip == false) {
                response += dataPod;
            }
            skip = true;
            //System.out.println("UTC:" + gmlDateTime);
            dataPod = "";
        }
        response += "]";
        response = response.replace(",]", "]");
        //NaN HACK
        response = response.replace("NaN", "null");
        //System.out.println(gmlCovPositions);
        //System.out.println(tupleList);
        //System.out.println(dataRecord);
        //System.out.println(response);
        return response;
    }
    
}
