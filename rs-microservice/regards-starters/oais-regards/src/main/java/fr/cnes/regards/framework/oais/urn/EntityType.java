/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.oais.urn;

/**
 *
 * List of available entity types
 *
 * @author msordi
 * @author Christophe Mertz
 *
 */
public enum EntityType {

    /**
     * Possible model type
     */
    COLLECTION("collection"),
    DOCUMENT("document"),
    DATA("data"),
    DATASET("dataset");

    private final String value;

    private EntityType(String value) {
        this.value = value;
    }

    public static EntityType fromString(String val) {
        for (EntityType type : values()) {
            if (type.value.equals(val)) {
                return type;
            }
        }
        throw new IllegalArgumentException(String.format("No enum constant for type %s", val));
    }

}
