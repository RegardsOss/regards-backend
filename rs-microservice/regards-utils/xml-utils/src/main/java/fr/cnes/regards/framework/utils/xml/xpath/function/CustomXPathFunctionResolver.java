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
package fr.cnes.regards.framework.utils.xml.xpath.function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;
import java.util.HashMap;
import java.util.Map;

public class CustomXPathFunctionResolver implements XPathFunctionResolver {

    public static final String EXTENDED_FUNCTION_PREFIX = "h2";

    public static final String EXTENDED_FUNCTION_URI = "http://h2.cnes.fr";

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomXPathFunctionResolver.class);

    private static final Map<String, XPathFunction> functionByName = new HashMap<>();

    public CustomXPathFunctionResolver(XPath xPath) {
        registerFunction(new BboxXPathFunction(xPath));
        registerFunction(new DateTimeXPathFunction());
        registerFunction(new DateTimeXPathFunction());
        registerFunction(new IntegerXPathFunction());
        registerFunction(new StringXPathFunction());
    }

    private static void registerFunction(NamedXPathFunction function) {
        functionByName.put(function.getFunctionName(), function);
    }

    @Override
    public XPathFunction resolveFunction(QName functionName, int arity) {
        LOGGER.debug("Resolver : {} with arity {}", functionName, arity);

        return functionByName.get(functionName.getLocalPart());
    }

}
