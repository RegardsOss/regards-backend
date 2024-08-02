/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.random.generator.AbstractNoParameterRandomGenerator;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Look at spring.factories
 */
@Component
public class RandomNowBuilder implements RandomGeneratorBuilder<RandomNowBuilder.NowGenerator> {

    @Override
    public String getFunctionName() {
        return "now";
    }

    @Override
    public NowGenerator build(FunctionDescriptor fd) {
        return new NowGenerator(fd);
    }

    static class NowGenerator extends AbstractNoParameterRandomGenerator<OffsetDateTime> {

        public NowGenerator(FunctionDescriptor fd) {
            super(fd);
        }

        @Override
        public OffsetDateTime random() {
            return OffsetDateTime.now();
        }
    }
}
