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
package fr.cnes.regards.modules.acquisition.domain.metamodel;

/**
 * Cette classe modelise les enumeres definissant les types d'entite
 * 
 * @author Christophe Mertz
 *
 */
public enum CalculationFunctionTypeEnum {

    /**
     * Enumere designant la fonction "somme"
     */
    SUM("SUM"),

    /**
     * Enumere designant la fonction "minimum"
     */
    MIN("MIN"),

    /**
     * Enumere designant la fonction "maximum"
     */
    MAX("MAX"),

    /**
     * Enumere designant la fonction "interval"
     */
    INTERVAL("INTERVAL"),

    /**
     * Enumere designant la fonction "nb"
     */
    COUNT("COUNT"),

    /**
     * Enumere designant la fonction "moyenne"
     */
    AVG("AVG"),

    /**
     * Enumere designant la fonction "sans effet"
     */
    NONE("NONE");

    private final String calculation;

    /**
     * Constructeur prive. Cette classe ne doit pas etre instanciee.
     * 
     * @param type
     *            le type sous forme de chaine.
     */
    private CalculationFunctionTypeEnum(String type) {
        calculation = type;
    }

    public static CalculationFunctionTypeEnum fromString(String strCalculation) {
        for (CalculationFunctionTypeEnum value : values()) {
            if (value.calculation.equals(strCalculation)) {
                return value;
            }
        }
        throw new IllegalArgumentException(String.format("No enum constant for calculation type %s", strCalculation));
    }

    @Override
    public String toString() {
        return this.name();
    }

}
