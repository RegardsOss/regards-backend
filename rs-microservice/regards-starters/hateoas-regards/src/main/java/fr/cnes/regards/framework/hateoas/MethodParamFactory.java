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
package fr.cnes.regards.framework.hateoas;

/**
 * Method parameter factory
 * @author msordi
 */
public final class MethodParamFactory {

    private MethodParamFactory() {
    }

    public static <T> MethodParam<T> build(Class<T> pParameterType, T pValue) {
        return new MethodParam<>(pParameterType, pValue);
    }

    public static <T> MethodParam<T> build(Class<T> pParameterType) {
        return new MethodParam<>(pParameterType, null);
    }

}
