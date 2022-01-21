/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FunctionDescriptor {

    private final String functionName;

    private final Map<Integer, String> parameters = new ConcurrentHashMap<>();

    public FunctionDescriptor(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public Map<Integer, String> getParameters() {
        return parameters;
    }

    public void addParameter(Integer position, String value) {
        this.parameters.put(position, value);
    }

    public String getParameter(Integer position) {
        return this.parameters.get(position);
    }

    public int getParameterSize() {
        return this.parameters.size();
    }

    @Override
    public String toString() {
        return "FunctionDescriptor [name=" + functionName + ", parameters=" + parameters + "]";
    }
}
