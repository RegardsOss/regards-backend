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
 * classe qui enumere les etat d'erreur pour les fichiers traites par les processus.
 * 
 * @author Christophe Mertz
 * 
 */

public class ErrorType {

    /**
     * valeur de l'erreur, cette valeur correspond a la valeur en base
     * 
     * @since 1.0
     */
    private int value_;

    /**
     * chaine de caractere explicitant la valeur
     * 
     * @since 1.0
     */
    private final String meaning_;

    private static Map<Integer, ErrorType> hashMap_;

    public static final ErrorType OK = new ErrorType(1, "OK");

    public static final ErrorType WARNING = new ErrorType(2, "WARNING");

    public static final ErrorType ERROR = new ErrorType(3, "ERROR");

    /**
     * constructeur prive
     * 
     * @param pValue
     * @param pMeaning
     * @since 1.0
     * 
     */
    private ErrorType(int pValue, String pMeaning) {
        value_ = pValue;
        meaning_ = pMeaning;
        if (hashMap_ == null) {
            hashMap_ = new HashMap<Integer, ErrorType>();
        }
        hashMap_.put(new Integer(pValue), this);
    }

    /**
     * Parse
     * 
     * @param pValue
     * @return ErrorType
     * @since 1.1
     */
    public static ErrorType parse(Integer pValue) {
        return hashMap_.get(pValue);
    }

    /**
     * Constructeur public
     * 
     * @param meaning_
     * @since 1.1
     */
    public ErrorType(String pMeaning) {
        meaning_ = pMeaning;
        if ("OK".equals(meaning_)) {
            value_ = OK.value_;
        } else if ("WARNING".equals(meaning_)) {
            value_ = WARNING.value_;
        } else if ("ERROR".equals(meaning_)) {
            value_ = ERROR.value_;
        } else {
            value_ = 0;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((meaning_ == null) ? 0 : meaning_.hashCode());
        result = prime * result + value_;
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
        ErrorType other = (ErrorType) obj;
        if (meaning_ == null) {
            if (other.meaning_ != null)
                return false;
        } else if (!meaning_.equals(other.meaning_))
            return false;
        if (value_ != other.value_)
            return false;
        return true;
    }

    public String getMeaning() {
        return meaning_;
    }

    public int getValue() {
        return value_;
    }
}
