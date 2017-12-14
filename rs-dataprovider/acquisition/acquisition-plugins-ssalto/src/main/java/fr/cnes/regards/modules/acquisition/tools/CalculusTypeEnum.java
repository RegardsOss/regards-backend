/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

    private final String name;

    public static final CalculusTypeEnum MIN = new CalculusTypeEnum("MIN");

    public static final CalculusTypeEnum MAX = new CalculusTypeEnum("MAX");

    public static final CalculusTypeEnum AVG = new CalculusTypeEnum("AVG");

    public static final CalculusTypeEnum FIRST = new CalculusTypeEnum("FIRST");

    public static final CalculusTypeEnum LAST = new CalculusTypeEnum("LAST");

    private CalculusTypeEnum(String pName) {
        name = pName;
    }

    /**
     * renvoie l'instance de CalculusTypeEnum dont la valeur est pValue,
     * null, si aucune instance n'est trouvee.
     * pValue est passe en majuscule. (ie : Min reverra CalculusTypeEnum.MIN)
     * @param pName
     * @return
     */
    public static CalculusTypeEnum parse(String pName) {
        CalculusTypeEnum myEnum = null;
        pName = pName.toUpperCase();
        if (pName.equals(MIN.name)) {
            myEnum = MIN;
        } else if (pName.equals(MAX.name)) {
            myEnum = MAX;
        } else if (pName.equals(AVG.name)) {
            myEnum = AVG;
        } else if (pName.equals(FIRST.name)) {
            myEnum = FIRST;
        } else if (pName.equals(LAST.name)) {
            myEnum = LAST;
        }
        return myEnum;
    }

    public String toString() {
        return name;
    }

}
