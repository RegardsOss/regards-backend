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
package fr.cnes.regards.framework.random.function;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunctionDescriptorParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionDescriptorParser.class);

    private static final String FUNCTION_REGEXP = "\\{\\{(\\w*)\\((.*)\\)\\}\\}";

    private static final Pattern FUNCTION_PATTERN = Pattern.compile(FUNCTION_REGEXP);

    private static final Pattern PARAM_ESCAPE_PATTERN = Pattern.compile("'(.*)'");

    private static final String SKIPPING = "Skipping static value : {}";

    /**
     * Split on all comma characters unless it's in between single quotes
     */
    private static final String PARAM_SPLIT_REGEXP = ",(?=([^']*'[^']*')*[^']*$)";

    private FunctionDescriptorParser() {}

    public static FunctionDescriptor parse(Object value) {
        if (value == null) {
            LOGGER.info(SKIPPING, value);
            return null;
        }
        if (String.class.isAssignableFrom(value.getClass())) {

            String function = (String) value;
            Matcher matcher = FUNCTION_PATTERN.matcher(function);
            if (matcher.matches()) {
                FunctionDescriptor fd = new FunctionDescriptor(FunctionDescriptorType.of(matcher.group(1)));
                parseParameters(fd, matcher.group(2));
                LOGGER.debug("{}", fd);
                return fd;
            } else {
                LOGGER.info(SKIPPING, function);
            }
        } else {
            LOGGER.info(SKIPPING, value);
        }
        return null;
    }

    private static void parseParameters(FunctionDescriptor descriptor, String parameters) {
        if (!parameters.isEmpty()) {
            String[] params = parameters.split(PARAM_SPLIT_REGEXP);
            for (int i = 0; i < params.length; i++) {
                String value = params[i].trim();
                Matcher matcher = PARAM_ESCAPE_PATTERN.matcher(value);
                if (matcher.matches()) {
                    value = matcher.group(1);
                }
                descriptor.addParameter(i, value);
            }
        }
    }
}
