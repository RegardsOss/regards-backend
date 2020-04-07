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

public enum FunctionDescriptorType {

    BOOLEAN("boolean"),
    LOCAL_DATE_TIME("ldt"),
    DOUBLE("double"),
    ENUM("enum"),
    FLOAT("float"),
    INTEGER("integer"),
    LONG("long"),
    STRING("string"),
    UUID("uuid");

    private String functionName;

    private FunctionDescriptorType(String functionName) {
        this.functionName = functionName;
    }

    public static FunctionDescriptorType of(String functionName) {
        for (FunctionDescriptorType ft : FunctionDescriptorType.values()) {
            if (ft.functionName.equals(functionName)) {
                return ft;
            }
        }
        throw new IllegalArgumentException(String.format("Unknow function %s", functionName));
    }
}
