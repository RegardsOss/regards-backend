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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.tools;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class Interval {

    private long minValue = 0;

    private long maxValue = 0;

    public Interval() {

    }

    public void update(long pValue) {
        if (minValue == 0) {
            minValue = pValue;
        }
        if (maxValue == 0) {
            maxValue = pValue;
        }

        if (pValue > maxValue) {
            maxValue = pValue;
        }
        else
            if (pValue < minValue) {
                minValue = pValue;
            }
    }

    public long getMaxValue() {
        return maxValue;
    }

    public long getMinValue() {
        return minValue;
    }

    public void setMaxValue(long pMaxValue) {
        maxValue = pMaxValue;
    }

    public void setMinValue(long pMinValue) {
        minValue = pMinValue;
    }
}
