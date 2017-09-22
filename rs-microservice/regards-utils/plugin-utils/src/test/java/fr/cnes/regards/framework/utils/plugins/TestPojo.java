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
package fr.cnes.regards.framework.utils.plugins;

import java.util.Date;
import java.util.List;

/**
 * Test class to test complex class plugin parameter type.
 *
 * @author sbinda
 *
 */
public class TestPojo {

    private String value;

    private List<String> values;

    private Date date;

    public String getValue() {
        return value;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValue(String pValue) {
        value = pValue;
    }

    public void setValues(List<String> pValues) {
        values = pValues;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date pDate) {
        date = pDate;
    }

}