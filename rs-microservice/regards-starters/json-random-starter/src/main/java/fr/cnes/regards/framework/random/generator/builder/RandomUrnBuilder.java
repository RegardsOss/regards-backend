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
import fr.cnes.regards.framework.random.generator.AbstractRandomGenerator;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Look at spring.factories
 */
@Component
public class RandomUrnBuilder implements RandomGeneratorBuilder<RandomUrnBuilder.RandomUrn> {

    @Override
    public String getFunctionName() {
        return "urn";
    }

    @Override
    public RandomUrn build(FunctionDescriptor fd) {
        return new RandomUrn(fd);
    }

    static class RandomUrn extends AbstractRandomGenerator<String> {

        private static final String USAGE = "Function %s only support 1 or 2 arguments";

        private String format;

        private String projetProperty;

        private String idProperty;

        public RandomUrn(FunctionDescriptor fd) {
            super(fd);
        }

        @Override
        public void parseParameters() {
            switch (fd.getParameterSize()) {
                case 1:
                    format = fd.getParameter(0);
                    break;
                case 2:
                    format = fd.getParameter(0);
                    idProperty = fd.getParameter(1);
                    break;
                case 3:
                    format = fd.getParameter(0);
                    projetProperty = fd.getParameter(1);
                    idProperty = fd.getParameter(2);
                    break;
                default:
                    throw new IllegalArgumentException(String.format(USAGE, fd.getFunctionName()));
            }
        }

        @Override
        public Optional<List<String>> getDependentProperties() {
            if ((idProperty == null) && (projetProperty == null)) {
                return Optional.empty();
            } else if (projetProperty == null) {
                return Optional.of(Arrays.asList(idProperty));
            } else {
                return Optional.of(Arrays.asList(idProperty, projetProperty));
            }
        }

        @Override
        public String random() {
            return String.format(format, UUID.randomUUID());
        }

        @Override
        public String randomWithContext(Map<String, Object> context) {
            Object idProObject = findValue(context, idProperty);
            String uuid;
            if (UUID.class.isAssignableFrom(idProObject.getClass())) {
                uuid = UUID.fromString(((UUID) idProObject).toString()).toString();
            } else if (String.class.isAssignableFrom(idProObject.getClass())) {
                uuid = UUID.nameUUIDFromBytes(((String) idProObject).getBytes()).toString();
            } else {
                throw new UnsupportedOperationException(String.format(
                    "%s does not support %s for dependent property value",
                    fd.getFunctionName(),
                    idProObject.getClass()));
            }

            if (projetProperty != null) {
                return String.format(format, findValue(context, projetProperty).toString(), uuid);
            } else {
                return String.format(format, uuid);
            }

        }
    }
}
