/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

/**
 * This class parses a {@link String} to extract a date and format this date with the format "yyyy-MM-dd'T'HH:mm:ss".<br>
 * If the incoming date does not contain hour, minute or second information,<br>
 * <li>the hour is set to 23,<br>
 * <li>the minute is set 59,<br>
 * <li>the seconds is set to 59.
 *   
 * @author Christophe Mertz
 */
public class SetStopDateCommun extends AbstractSetDateCommon {

    protected int getDefaultHour() {
        return 23;
    }

    protected int getDefaultMinute() {
        return 59;
    }

    protected int getDefaultSecond() {
        return 59;
    }
}