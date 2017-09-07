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

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginConfigurationProperties;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.DateFormatter;

/**
 * Formate les dates recupererees dans le fichier cs_data([0-9]{3}).dat.Z
 * 
 * @author Christophe Mertz
 *
 */
public class SetCryosat2Doris1bPoeCddisDate implements ICalculationClass {

    public Object calculateValue(Object value, AttributeTypeEnum type, PluginConfigurationProperties properties) {

        Date date = calculate(value, type);

        return DateFormatter.getDateRepresentation(date, DateFormatter.XS_DATE_TIME_FORMAT);
    }

    /**
     * Calcule la date associee au parametre value
     * @param value
     * @param type
     * @return type Date
     */
    public Date calculate(Object value, AttributeTypeEnum type) {
        // Read line
        String line = (String) value;
        // Parameters for compute values
        String compute = null;
        int beginIndex = 0;
        int endIndex = 0;

        // Init calendar
        Calendar cal = Calendar.getInstance();

        // Compute year
        // 17-18 Time of observation (beginning of count)
        // Year minus 1900 if greater than 90
        // Year minus 2000 if less than or equal 90
        beginIndex = 0;
        endIndex = 2;
        compute = line.substring(beginIndex, endIndex).trim();
        int year = Integer.valueOf(compute).intValue();
        if (year > 90) {
            year = year + 1900;
        } else {
            year = year + 2000;
        }
        cal.set(Calendar.YEAR, year);

        // Set month
        cal.set(Calendar.MONTH, 0);

        // Compute day
        // 19-21 Day of year (January 1 = Day 1)
        beginIndex = 2;
        endIndex = 5;
        compute = line.substring(beginIndex, endIndex).trim();
        int day = Integer.valueOf(compute).intValue();
        cal.set(Calendar.DATE, day);

        // Set hour
        cal.set(Calendar.HOUR_OF_DAY, 0);

        // Set minute
        cal.set(Calendar.MINUTE, 0);

        // Compute second
        // 22-26 Seconds from midnight
        beginIndex = 5;
        endIndex = 10;
        compute = line.substring(beginIndex, endIndex).trim();
        int second = Integer.valueOf(compute).intValue();
        cal.set(Calendar.SECOND, second);

        // 27-32 Fractional part of seconds (microseconds)
        // Not used

        return cal.getTime();
    }
}
