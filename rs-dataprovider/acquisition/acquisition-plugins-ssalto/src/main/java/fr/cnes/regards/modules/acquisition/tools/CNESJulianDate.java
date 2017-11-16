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
package fr.cnes.regards.modules.acquisition.tools;

import java.util.Calendar;
import java.util.Date;

/**
 * Classe permettant de manipuler des dates en jour Julien CNES, c'est a dire
 * des dates comptees en jour depuis le 1 janvier 1950.
 * 
 * @author Christophe Mertz
 *
 */
public class CNESJulianDate {

    /**
     * @return a Julian Calendar whose first day is 01/01/1950
     */
    private static Calendar getJulianCalendar() {
        // Init CNES Julian day : 1 jan 1950
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1950);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal;
    }

    /**
     * Transforme les jour Julien CNES en date.
     * 
     * @param pJulianDays
     * @param pSecondInDay
     * @return
     */
    public static Date toDate(Integer pJulianDays, Integer pSecondInDay) {

        Calendar cal = getJulianCalendar();

        // Add number of days
        if (pJulianDays != null) {
            cal.add(Calendar.DATE, pJulianDays.intValue());
        }
        // Add second in day
        if (pSecondInDay != null) {
            int seconds = pSecondInDay.intValue();
            // if seconds = 86400 then hour = 23:59:59 instead of 00:00:00 the
            // day after
            if (seconds == 86400) {
                cal.add(Calendar.SECOND, seconds - 1);
            } else {
                cal.add(Calendar.SECOND, seconds);
            }
        }

        return cal.getTime();
    }

    /**
     * Transforme les jour Julien CNES en date.
     * 
     * @param pJulianDays
     * @param pSecondInDay
     * @return
     */
    public static Date toDate(String pJulianDays, String pSecondInDay) {
        Integer julianDays = null;
        Integer secondInDay = null;
        if (pJulianDays != null) {
            julianDays = Integer.valueOf(pJulianDays);
        }
        if (pSecondInDay != null) {
            secondInDay = Integer.valueOf(pSecondInDay);
        }
        return CNESJulianDate.toDate(julianDays, secondInDay);
    }

    /**
     * Transforme les jour Julien CNES en date.
     * 
     * @param pJulianDays
     * @return
     */
    public static Date toDate(Integer pJulianDays) {
        return CNESJulianDate.toDate(pJulianDays, 0);
    }

    /**
     * 
     * @param pJulianDays
     * @param pHours
     * @param pMinutes
     * @param pSeconds
     * @return a Gregorian Date
     */
    public static Date toDate(String pJulianDays, String pHours, String pMinutes, String pSeconds) {

        Calendar cal = getJulianCalendar();
        // Add Julien days
        if (pJulianDays != null) {
            Integer julianDays = Integer.valueOf(pJulianDays);
            cal.add(Calendar.DATE, julianDays);
        }
        // Add hours
        if (pHours != null) {
            Integer hours = Integer.valueOf(pHours);
            cal.set(Calendar.HOUR_OF_DAY, hours);
        }
        // Add minutes
        if (pMinutes != null) {
            Integer minutes = Integer.valueOf(pMinutes);
            cal.set(Calendar.MINUTE, minutes);
        }
        // Add seconds
        if (pSeconds != null) {
            Integer seconds = Integer.valueOf(pSeconds);
            cal.set(Calendar.SECOND, seconds);
        }

        return cal.getTime();
    }
}
