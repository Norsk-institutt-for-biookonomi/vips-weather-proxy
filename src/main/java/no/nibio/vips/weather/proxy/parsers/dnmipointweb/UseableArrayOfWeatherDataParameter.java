/*
 * Copyright (c) 2017 NIBIO <https://www.nibio.no/>. 
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

package no.nibio.vips.weather.proxy.parsers.dnmipointweb;

import org.datacontract.schemas._2004._07.dmiweatherservice.ArrayOfWeatherDataParameter;
import org.datacontract.schemas._2004._07.dmiweatherservice.WeatherDataParameter;

/**
 * @copyright 2017 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class UseableArrayOfWeatherDataParameter extends ArrayOfWeatherDataParameter{
    
    public void add(WeatherDataParameter wdp)
    {
        this.getWeatherDataParameter().add(wdp);
    }

}
