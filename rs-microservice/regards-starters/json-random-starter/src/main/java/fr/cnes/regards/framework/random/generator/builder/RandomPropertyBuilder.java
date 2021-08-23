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
package fr.cnes.regards.framework.random.generator.builder;

import fr.cnes.regards.framework.random.function.FunctionDescriptor;
import fr.cnes.regards.framework.random.function.IPropertyGetter;
import fr.cnes.regards.framework.random.generator.AbstractRandomGenerator;

public class RandomPropertyBuilder implements RandomGeneratorBuilder<RandomPropertyBuilder.PropertyGenerator> {

    @Override
    public String getFunctionName() {
        return "property";
    }

    @Override
    public PropertyGenerator build(FunctionDescriptor fd) {
        return new PropertyGenerator(fd);
    }

    @Override
    public PropertyGenerator build(FunctionDescriptor fd, IPropertyGetter propertyGetter) {
        return new PropertyGenerator(fd, propertyGetter);
    }

    /**
     * @author sbinda
     */
    public static class PropertyGenerator extends AbstractRandomGenerator<String> {

        private static final String USAGE = "Function %s only support a property key as parameter like example.myproperty";

        private String springProperty = null;

        private IPropertyGetter propertyGetter = null;

        public PropertyGenerator(FunctionDescriptor fd) {
            super(fd);
            throw new UnsupportedOperationException("Property getter is required:");
        }

        public PropertyGenerator(FunctionDescriptor fd, IPropertyGetter propertyGetter) {
            super(fd);
            this.propertyGetter = propertyGetter;
        }

        @Override
        public void parseParameters() {
            if (fd.getParameterSize() == 1) {
                springProperty = fd.getParameter(0);
            } else {
                throw new IllegalArgumentException(String.format(USAGE, fd.getFunctionName()));
            }
        }

        @Override
        public String random() {
            return propertyGetter.getProperty(springProperty);
        }
    }
}
