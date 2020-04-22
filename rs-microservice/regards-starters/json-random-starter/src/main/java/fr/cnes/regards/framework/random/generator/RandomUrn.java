/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import fr.cnes.regards.framework.random.function.FunctionDescriptor;

public class RandomUrn extends AbstractRandomGenerator<String> {

    private static String USAGE = "Function {} only support 1 or 2 arguments";

    private String format;

    private String from;

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
                from = fd.getParameter(1);
                break;
            default:
                throw new IllegalArgumentException(String.format(USAGE, fd.getType()));
        }
    }

    @Override
    public Optional<List<String>> getDependentProperties() {
        if (from == null) {
            return Optional.empty();
        }
        return Optional.of(Arrays.asList(from));
    }

    @Override
    public String random() {
        return String.format(format, UUID.randomUUID());
    }

    @Override
    public String randomWithContext(Map<String, Object> context) {
        Object fromObject = findValue(context, from);
        if (UUID.class.isAssignableFrom(fromObject.getClass())) {
            return String.format(format, UUID.fromString(((UUID) fromObject).toString()));
        }
        if (String.class.isAssignableFrom(fromObject.getClass())) {
            return String.format(format, UUID.nameUUIDFromBytes(((String) fromObject).getBytes()));
        }
        throw new UnsupportedOperationException(String.format("%s does not support %s for dependent property value",
                                                              fd.getType(), fromObject.getClass()));

    }
}
