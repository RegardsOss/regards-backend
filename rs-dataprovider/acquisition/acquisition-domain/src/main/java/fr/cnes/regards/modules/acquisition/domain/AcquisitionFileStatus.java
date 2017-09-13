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
package fr.cnes.regards.modules.acquisition.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class AcquisitionFileStatus implements Comparable<Object> {

    /**
     * valeur de l'etat, cette valeur correspond a la valeur en base
     */
    private final int value;

    private static Map<Integer, AcquisitionFileStatus> hashMap;

    /**
     * chaine de caractere explicitant la valeur
     */
    private final String meaning;

    public static final AcquisitionFileStatus INVALID = new AcquisitionFileStatus(1, "INVALID");

    public static final AcquisitionFileStatus DUPLICATE = new AcquisitionFileStatus(2, "DUPLICATE");

    public static final AcquisitionFileStatus IN_PROGRESS = new AcquisitionFileStatus(3, "IN_PROGRESS");

    public static final AcquisitionFileStatus DELETED = new AcquisitionFileStatus(4, "DELETED");

    public static final AcquisitionFileStatus VALID = new AcquisitionFileStatus(5, "VALID");

    public static final AcquisitionFileStatus TO_ARCHIVE = new AcquisitionFileStatus(6, "TO_ARCHIVE");

    public static final AcquisitionFileStatus ARCHIVED = new AcquisitionFileStatus(7, "ARCHIVED");

    public static final AcquisitionFileStatus IN_CATALOGUE = new AcquisitionFileStatus(8, "IN_CATALOGUE");

    public static final AcquisitionFileStatus TAR_CURRENT = new AcquisitionFileStatus(9, "TAR_CURRENT");

    public static final AcquisitionFileStatus ACQUIRED = new AcquisitionFileStatus(13, "ACQUIRED");

    /** Duplicate additional status */
    // Status of current copied file
    public static final AcquisitionFileStatus DUPLICATE_COPIED = new AcquisitionFileStatus(10, "DUPLICATE_COPIED");

    // Status of current file not take into account
    public static final AcquisitionFileStatus DUPLICATE_IGNORED = new AcquisitionFileStatus(11, "DUPLICATE_IGNORED");

    /** Manual additional status */
    public static final AcquisitionFileStatus DUPLICATE_WAIT_OPERATOR = new AcquisitionFileStatus(12, "DUPLICATE_WAIT");

    /**
     * constrcteur prive
     * 
     * @param val
     * @param mean
     * 
     */
    public AcquisitionFileStatus(int val, String mean) {
        value = val;
        meaning = mean;
        if (hashMap == null) {
            hashMap = new HashMap<>();
        }
        hashMap.put(new Integer(val), this);
    }

    /**
     * Parse
     * 
     * @param val
     * @return AcquisitionFileStatus
     */
    public static AcquisitionFileStatus parse(Integer val) {
        return hashMap.get(val);
    }

    public String getMeaning() {
        return meaning;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + value;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AcquisitionFileStatus other = (AcquisitionFileStatus) obj;
        if (value != other.value)
            return false;
        return true;
    }

    public int getValue() {
        return value;
    }

    /**
     * Methode surchargee permettant la comparaison de deux objects AcquisitionFileStatus
     */
    @Override
    public int compareTo(Object o) {
        int ret = 1;

        if (value == ((AcquisitionFileStatus) o).getValue()) {
            ret = 0;
        } else if (value < ((AcquisitionFileStatus) o).getValue()) {
            ret = -1;
        } else {
            ret = 1;
        }
        return ret;
    }
}
