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
package fr.cnes.regards.framework.utils.cycle.detection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test class to test complex class plugin parameter type.
 *
 * @author sbinda
 *
 */
public class TestPojo {

    private String value;

    private List<String> values;

    private Set<Integer> intValues;

    private Set<Long> longValues;

    // format DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private String date;

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

    public String getDate() {
        return date;
    }

    public void setDate(String offsetDateTime) {
        date = offsetDateTime;
    }

    public Set<Integer> getIntValues() {
        return intValues;
    }

    public void setIntValues(Set<Integer> intValues) {
        this.intValues = intValues;
    }

    public void addIntValues(Integer n) {
        if (this.intValues == null) {
            this.intValues = new HashSet<>();
        }
        this.intValues.add(n);
    }

    public Set<Long> getLongValues() {
        return longValues;
    }

    public void setLongValues(Set<Long> longValues) {
        this.longValues = longValues;
    }

    public void addLongValues(Long l) {
        if (this.longValues == null) {
            this.longValues = new HashSet<>();
        }
        this.longValues.add(l);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((date == null) ? 0 : date.hashCode());
        result = prime * result + ((intValues == null) ? 0 : intValues.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + ((values == null) ? 0 : values.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TestPojo other = (TestPojo) obj;
        if (date == null) {
            if (other.date != null)
                return false;
        } else if (!date.equals(other.date))
            return false;
        if (intValues == null) {
            if (other.intValues != null)
                return false;
        } else if (!intValues.equals(other.intValues))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        if (values == null) {
            if (other.values != null)
                return false;
        } else if (!values.equals(other.values))
            return false;
        return true;
    }

}