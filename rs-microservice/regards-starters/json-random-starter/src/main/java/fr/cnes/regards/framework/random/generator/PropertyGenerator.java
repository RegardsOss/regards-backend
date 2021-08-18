/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.random.generator;

import fr.cnes.regards.framework.random.function.FunctionDescriptor;
import fr.cnes.regards.framework.random.function.IPropertyGetter;

/**
 * @author sbinda
 *
 */
public class PropertyGenerator extends AbstractRandomGenerator<String> {

    private static String USAGE = "Function {} only support a property key as parameter like example.myproperty";

    private String springProperty = null;

    private IPropertyGetter propertyGetter = null;

    /**
     * @param fd
     */
    public PropertyGenerator(FunctionDescriptor fd) {
        super(fd);
    }

    /**
     * @param fd
     * @param env
     */
    public PropertyGenerator(FunctionDescriptor fd, IPropertyGetter propertyGetter) {
        super(fd);
        this.propertyGetter = propertyGetter;
    }

    @Override
    public void parseParameters() {
        switch (fd.getParameterSize()) {
            case 1:
                springProperty = fd.getParameter(0);
                break;
            default:
                throw new IllegalArgumentException(String.format(USAGE, fd.getType()));
        }
    }

    @Override
    public String random() {
        return propertyGetter.getProperty(springProperty).toString();
    }

}
