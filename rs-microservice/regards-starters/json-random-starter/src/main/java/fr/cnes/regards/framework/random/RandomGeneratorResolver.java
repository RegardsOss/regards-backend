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
package fr.cnes.regards.framework.random;

import fr.cnes.regards.framework.random.function.FunctionDescriptor;
import fr.cnes.regards.framework.random.function.FunctionDescriptorParser;
import fr.cnes.regards.framework.random.function.IPropertyGetter;
import fr.cnes.regards.framework.random.generator.NoopGenerator;
import fr.cnes.regards.framework.random.generator.RandomGenerator;
import fr.cnes.regards.framework.random.generator.builder.RandomGeneratorBuilder;

import java.util.List;

/**
 * Service for generator auto discovery
 */
public class RandomGeneratorResolver {

    private List<RandomGeneratorBuilder<?>> generatorBuilders;

    public RandomGeneratorResolver(List<RandomGeneratorBuilder<?>> generatorBuilders) {
        this.generatorBuilders = generatorBuilders;
    }

    public RandomGenerator<?> get(Object value, IPropertyGetter propertyGetter) {
        // Parse function
        FunctionDescriptor fd = FunctionDescriptorParser.parse(value);
        if (fd == null) {
            return new NoopGenerator(value);
        }

        // Find a generator builder
        RandomGeneratorBuilder<?> builder = generatorBuilders.stream()
                .filter(rgb -> rgb.getFunctionName().equals(fd.getFunctionName())).findFirst().orElseThrow(
                        () -> new IllegalArgumentException(
                                String.format("Unsupported function %s", fd.getFunctionName())));

        RandomGenerator<?> rg = builder.build(fd, propertyGetter);
        rg.parseParameters();
        return rg;
    }
}
