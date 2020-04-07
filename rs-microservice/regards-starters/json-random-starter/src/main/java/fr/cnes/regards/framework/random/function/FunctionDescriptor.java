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

import java.util.HashMap;
import java.util.Map;

public class FunctionDescriptor {

    private FunctionDescriptorType type;

    private Map<Integer, Object> parameters = new HashMap<>();

    public FunctionDescriptor(FunctionDescriptorType type) {
        this.type = type;
    }

    public FunctionDescriptorType getType() {
        return type;
    }

    public void setType(FunctionDescriptorType type) {
        this.type = type;
    }

    public Map<Integer, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<Integer, Object> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(Integer position, Object value) {
        this.parameters.put(position, value);
    }

    public Object getParameter(Integer position) {
        return this.parameters.get(position);
    }

    @Override
    public String toString() {
        return "FunctionDescriptor [type=" + type + ", parameters=" + parameters + "]";
    }
}
