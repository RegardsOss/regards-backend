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
package fr.cnes.regards.framework.hateoas;

import org.springframework.util.Assert;

/**
 *
 * Method parameter definition
 *
 * @param <T>
 *            parameter type
 * @author msordi
 *
 */
public class MethodParam<T> {

    /**
     * Parameter type
     */
    private final Class<T> parameterType;

    /**
     * Parameter value
     */
    private final T value;

    public MethodParam(final Class<T> pParameterType, final T pValue) {
        Assert.notNull(pParameterType);
        this.parameterType = pParameterType;
        this.value = pValue;
    }

    public T getValue() {
        return value;
    }

    public Class<T> getParameterType() {
        return parameterType;
    }
}
