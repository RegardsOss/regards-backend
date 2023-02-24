package fr.cnes.regards.modules.search.domain;

/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

/**
 * POJO to handle boundaries of an attribute.
 *
 * @author Sébastien Binda
 */
public class PropertyBound<T> {

    /**
     * Json path of the attribute
     */
    private String propertyName;

    /**
     * Lower bound value
     */
    private T lowerBound;

    /**
     * Upper bound value
     */
    private T upperBound;

    /**
     * @param propertyName Jsonpath of the attribute
     * @param lowerBound   Lower bound value
     * @param upperBound   Upper bound value
     */
    public PropertyBound(String propertyName, T lowerBound, T upperBound) {
        super();
        this.propertyName = propertyName;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public T getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(T lowerBound) {
        this.lowerBound = lowerBound;
    }

    public T getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(T upperBound) {
        this.upperBound = upperBound;
    }

}
