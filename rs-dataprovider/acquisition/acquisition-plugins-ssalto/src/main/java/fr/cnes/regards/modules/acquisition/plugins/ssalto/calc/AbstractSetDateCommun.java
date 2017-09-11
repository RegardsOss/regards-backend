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

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginConfigurationProperties;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.DateFormatter;

/**
 * 
 * @author Christophe Mertz
 *
 */
public abstract class AbstractSetDateCommun implements ICalculationClass {
    public Object calculateValue(Object pValue, AttributeTypeEnum pType, PluginConfigurationProperties properties) {

        boolean status = false;
        String result = null;

        String year = null;
        String month = null;
        String day = null;
        String hours = null;
        String minutes = null;
        String seconds = null;

        // Required filePattern
        String requiredPattern = "([0-9]{4})\\s+([0-9]{1,2})\\s+([0-9]{1,2})\\s+([0-9]{1,2})\\s+([0-9]{1,2})\\s+([0-9]{1,2})";
        Pattern pattern = Pattern.compile(requiredPattern);
        Matcher matcher = pattern.matcher((String) pValue);
        if (!matcher.matches()) {
            // Verify without the hour indication
            String altRequiredPattern = "([0-9]{4})\\s+([0-9]{1,2})\\s+([0-9]{1,2})";
            Pattern altPattern = Pattern.compile(altRequiredPattern);
            matcher = altPattern.matcher((String) pValue);

            if (matcher.matches()) {
                status = true;
                // Year = group 1
                year = matcher.group(1);
                // Month = group 2
                month = matcher.group(2);
                // Day = group 3
                day = matcher.group(3);
            }
        } else {
            status = true;
            // Year = group 1
            year = matcher.group(1);
            // Month = group 2
            month = matcher.group(2);
            // Day = group 3
            day = matcher.group(3);
            // Hour = group 4
            hours = matcher.group(4);
            // Minutes = group 5
            minutes = matcher.group(5);
            // Secondes = group 6
            seconds = matcher.group(6);
        }

        Calendar cal = Calendar.getInstance();
        if (year != null && Integer.valueOf(year) != null) {
            cal.set(Calendar.YEAR, Integer.valueOf(year).intValue());
        }
        if (month != null && Integer.valueOf(month) != null && Integer.valueOf(month).intValue() > 0) {
            cal.set(Calendar.MONTH, (Integer.valueOf(month).intValue() - 1));
        }
        if (day != null && Integer.valueOf(day) != null) {
            cal.set(Calendar.DATE, Integer.valueOf(day).intValue());
        }
        if (hours != null && Integer.valueOf(hours) != null) {
            cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(hours).intValue());
        } else {
            cal.set(Calendar.HOUR_OF_DAY, getDefaultHour());
        }
        if (minutes != null && Integer.valueOf(minutes) != null) {
            cal.set(Calendar.MINUTE, Integer.valueOf(minutes).intValue());
        } else {
            cal.set(Calendar.MINUTE, getDefaultMinute());
        }
        if (seconds != null && Integer.valueOf(seconds) != null) {
            cal.set(Calendar.SECOND, Integer.valueOf(seconds).intValue());
        } else {
            cal.set(Calendar.SECOND, getDefaultSecond());
        }

        if (status) {
            Date tmp = cal.getTime();
            result = DateFormatter.getDateRepresentation(tmp, DateFormatter.XS_DATE_TIME_FORMAT);
        }

        return result;
    }

    abstract int getDefaultHour();

    abstract int getDefaultMinute();

    abstract int getDefaultSecond();

}
