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
package fr.cnes.regards.modules.acquisition.domain.model;

/**
 *
 * Cette classe modelise le type de critere : date, chaine de caractere, entier ou reel.
 *
 * @author Christophe Mertz
 */
public enum AttributeTypeEnum {

    TYPE_UNKNOWN("UNKNOWN"),
    TYPE_REAL("REAL"),
    TYPE_INTEGER("INTEGER"),
    TYPE_STRING("STRING"),
    TYPE_DATE_TIME("DATE_TIME"),
    TYPE_DATE("DATE"),
    TYPE_CLOB("CLOB"),
    TYPE_URL("URL"),
    TYPE_LONG_STRING("LONG_STRING"),
    TYPE_GEO_LOCATION("GEO_LOCATION");

    /**
     * Type de flux
     */
    private String typeName;

    /**
     *
     * Constructeur
     *
     * @param pTypeName
     *            le nom du type
     */
    private AttributeTypeEnum(String pTypeName) {
        typeName = pTypeName;
    }

    public static AttributeTypeEnum parse(String pTypeName) {
        AttributeTypeEnum type = TYPE_UNKNOWN;
        AttributeTypeEnum[] vals = AttributeTypeEnum.values();
        for (int i = 0; i < vals.length && type.equals(TYPE_UNKNOWN); i++) {
            if (pTypeName.equals(vals[i].getTypeName())) {
                type = vals[i];
            }
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    public String getTypeName() {
        return typeName;
    }
}