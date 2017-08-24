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
public class SsaltoFileStatus implements Comparable<Object> {

    /**
     * valeur de l'etat, cette valeur correspond a la valeur en base
     */
    private final int value_;

    private static Map<Integer, SsaltoFileStatus> hashMap_;

    /**
     * chaine de caractere explicitant la valeur
     * 
     * @since 1.0
     */
    private final String meaning_;

    public static final SsaltoFileStatus INVALID = new SsaltoFileStatus(1, "INVALID");

    public static final SsaltoFileStatus DUPLICATE = new SsaltoFileStatus(2, "DUPLICATE");

    public static final SsaltoFileStatus IN_PROGRESS = new SsaltoFileStatus(3, "IN_PROGRESS");

    public static final SsaltoFileStatus DELETED = new SsaltoFileStatus(4, "DELETED");

    public static final SsaltoFileStatus VALID = new SsaltoFileStatus(5, "VALID");

    public static final SsaltoFileStatus TO_ARCHIVE = new SsaltoFileStatus(6, "TO_ARCHIVE");

    public static final SsaltoFileStatus ARCHIVED = new SsaltoFileStatus(7, "ARCHIVED");

    public static final SsaltoFileStatus IN_CATALOGUE = new SsaltoFileStatus(8, "IN_CATALOGUE");

    public static final SsaltoFileStatus TAR_CURRENT = new SsaltoFileStatus(9, "TAR_CURRENT");

    public static final SsaltoFileStatus ACQUIRED = new SsaltoFileStatus(13, "ACQUIRED");

    /** Duplicate additional status */
    // Status of current copied file
    public static final SsaltoFileStatus DUPLICATE_COPIED = new SsaltoFileStatus(10, "DUPLICATE_COPIED");

    // Status of current file not take into account
    public static final SsaltoFileStatus DUPLICATE_IGNORED = new SsaltoFileStatus(11, "DUPLICATE_IGNORED");

    /** Manual additional status */
    public static final SsaltoFileStatus DUPLICATE_WAIT_OPERATOR = new SsaltoFileStatus(12, "DUPLICATE_WAIT");

    /**
     * constrcteur prive
     * 
     * @param pValue
     * @param pMeaning
     * @since 1.0
     * 
     */
    public SsaltoFileStatus(int pValue, String pMeaning) {
        value_ = pValue;
        meaning_ = pMeaning;
        if (hashMap_ == null) {
            hashMap_ = new HashMap<>();
        }
        hashMap_.put(new Integer(pValue), this);
    }

    /**
     * teste les champs value_ Methode surchargee
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     * @since 1.0
     */
    @Override
    public boolean equals(Object pArg0) {
        boolean result = false;
        SsaltoFileStatus status = (SsaltoFileStatus) pArg0;
        if (status.value_ == value_) {
            result = true;
        }
        return result;
    }

    /**
     * Parse
     * 
     * @param pValue
     * @return SsaltoFileStatus
     * @since 1.0
     */
    public static SsaltoFileStatus parse(Integer pValue) {
        return hashMap_.get(pValue);
    }

    // GETTERS AND SETTERS

    public String getMeaning() {
        return meaning_;
    }

    public int getValue() {
        return value_;
    }

    /**
     * Methode surchargee permettant la comparaison de deux objects SsaltoFileStatus
     */
    @Override
    public int compareTo(Object o) {
        int ret = 1;

        if (value_ == ((SsaltoFileStatus) o).getValue()) {
            ret = 0;
        } else if (value_ < ((SsaltoFileStatus) o).getValue()) {
            ret = -1;
        } else {
            ret = 1;
        }
        return ret;
    }
}
