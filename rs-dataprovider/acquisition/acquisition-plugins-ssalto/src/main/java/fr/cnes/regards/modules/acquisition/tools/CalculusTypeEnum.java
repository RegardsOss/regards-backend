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
package fr.cnes.regards.modules.acquisition.tools;

/**
 * 
 * @author Christophe Mertz
 *
 */
public final class CalculusTypeEnum {

    private final String value;

    public static final CalculusTypeEnum MIN = new CalculusTypeEnum("MIN");

    public static final CalculusTypeEnum MAX = new CalculusTypeEnum("MAX");

    public static final CalculusTypeEnum AVG = new CalculusTypeEnum("AVG");

    public static final CalculusTypeEnum FIRST = new CalculusTypeEnum("FIRST");

    public static final CalculusTypeEnum LAST = new CalculusTypeEnum("LAST");

    private CalculusTypeEnum(String newVal) {
        value = newVal;
    }

    /**
     * Renvoie l'instance de {@link CalculusTypeEnum} dont la valeur est name, <code>null</code>, si aucune instance n'est trouvee,
     * name est passe en majuscule (ie : Min reverra CalculusTypeEnum.MIN)
     * @param name la valeur à parser
     * @return la valeur de {@link CalculusTypeEnum} correspondant à la valeur passée en paramètre
     */
    public static CalculusTypeEnum parse(String name) {
        CalculusTypeEnum myEnum = null;
        String calcValue = name.toUpperCase();
        if (calcValue.equals(MIN.value)) {
            myEnum = MIN;
        } else if (calcValue.equals(MAX.value)) {
            myEnum = MAX;
        } else if (calcValue.equals(AVG.value)) {
            myEnum = AVG;
        } else if (calcValue.equals(FIRST.value)) {
            myEnum = FIRST;
        } else if (calcValue.equals(LAST.value)) {
            myEnum = LAST;
        }
        return myEnum;
    }

    public String toString() {
        return value;
    }

}
