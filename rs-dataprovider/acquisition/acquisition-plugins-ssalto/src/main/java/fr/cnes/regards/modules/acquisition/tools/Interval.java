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

/**
 * Manage an interval of value [minValue, maxValue].
 *  
 * @author Christophe Mertz
 *
 */
public class Interval {

    /**
     * The min value of the {@link Interval}
     */
    private long minValue = 0;

    /**
     * The max value of the {@link Interval}
     */
    private long maxValue = 0;

    /**
     * Default constructor
     */
    public Interval() {
        super();
    }

    /**
     * Update the interval value with a new value<br>
     * If the value is > maxValue then maxValue is set to value
     * If the value is < minValue then minValue is set to value
     * @param value the value to apply to the interval
     */
    public void update(long value) {
        if (minValue == 0) {
            minValue = value;
        }
        if (maxValue == 0) {
            maxValue = value;
        }

        if (value > maxValue) {
            maxValue = value;
        } else if (value < minValue) {
            minValue = value;
        }
    }

    public long getMaxValue() {
        return maxValue;
    }

    public long getMinValue() {
        return minValue;
    }

    public void setMaxValue(long maxVal) {
        maxValue = maxVal;
    }

    public void setMinValue(long minVal) {
        minValue = minVal;
    }
}
