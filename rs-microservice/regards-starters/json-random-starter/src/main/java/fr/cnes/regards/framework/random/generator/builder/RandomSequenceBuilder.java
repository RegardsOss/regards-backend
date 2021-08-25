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
import fr.cnes.regards.framework.random.generator.AbstractRandomGenerator;
import org.springframework.stereotype.Component;

/**
 * Look at spring.factories
 */
@Component
public class RandomSequenceBuilder implements RandomGeneratorBuilder<RandomSequenceBuilder.SequenceGenerator> {

    @Override
    public String getFunctionName() {
        return "seq";
    }

    @Override
    public SequenceGenerator build(FunctionDescriptor fd) {
        return new SequenceGenerator(fd);
    }

    public static class SequenceGenerator extends AbstractRandomGenerator<String> {

        private static final String USAGE = "Function %s only support an optional format as for String.format";

        private Integer current = 0;

        private String format = "%d";

        public SequenceGenerator(FunctionDescriptor fd) {
            super(fd);
        }

        @Override
        public void parseParameters() {
            switch (fd.getParameterSize()) {
                case 0:
                    break;
                case 1:
                    format = fd.getParameter(0);
                    break;
                default:
                    throw new IllegalArgumentException(String.format(USAGE, fd.getFunctionName()));
            }
        }

        @Override
        public String random() {
            String s = String.format(format, current);
            current++;
            return s;
        }
    }
}
