/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.acquisition.plugins.ssalto.calc;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginConfigurationProperties;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.CNESJulianDate;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.DateFormatter;

/**
 * This classe calculates a date according to the input value.<br>
 * The required value format is "jjjjjhhmm" in CNES Julian Date
 * <ul>
 * <li>Julian day = jjjjj</li>
 * <li>Hours = hh</li>
 * <li>Minutes = mm</li>
 * <li>Second = 0</li>
 * </ul>
 * 
 * @author Christophe Mertz
 *
 */
public class SetDateCciArchive implements ICalculationClass {

    @Override
    public Object calculateValue(Object pValue, AttributeTypeEnum pType, PluginConfigurationProperties properties) {

        String days = null;
        String hours = null;
        String minutes = null;
        String seconds = String.valueOf(0);

        // Required filePattern
        String requiredPattern = "([0-9]{5})([0-9]{2})([0-9]{2})";
        Pattern pattern = Pattern.compile(requiredPattern);
        Matcher matcher = pattern.matcher((String) pValue);
        if (matcher.matches()) {
            // Days = group 1
            days = matcher.group(1);
            // Hours = group 2
            hours = matcher.group(2);
            // Minutes = group 3
            minutes = matcher.group(3);
        }

        Date tmp = CNESJulianDate.toDate(days, hours, minutes, seconds);
        String tmp2String = DateFormatter.getDateRepresentation(tmp, DateFormatter.XS_DATE_TIME_FORMAT);
        return tmp2String;
    }

}
