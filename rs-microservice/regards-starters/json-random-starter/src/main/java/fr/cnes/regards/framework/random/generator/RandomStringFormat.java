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

import java.util.List;

import fr.cnes.regards.framework.random.function.FunctionDescriptor;

public class RandomStringFormat extends AbstractRandomGenerator<String> {

    private static String USAGE = "Function {} needs 2 paramters minimum format and parameters";

    private String format;

    private List<String> parameters;

    public RandomStringFormat(FunctionDescriptor fd) {
        super(fd);
    }

    @Override
    public void parseParameters() {
        if (fd.getParameterSize() < 2) {
            throw new IllegalArgumentException(String.format(USAGE, fd.getType()));
        } else {
            parameters.addAll(fd.getParameters().values());
        }
    }

    @Override
    public String random() {
        Object[] params = new String[parameters.size()];
        parameters.toArray(params);
        return String.format(format, params);
    }
}
