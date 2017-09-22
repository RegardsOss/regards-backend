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

/**
 * 
 * @author Christophe Mertz
 * 
 * 
 */

public enum ErrorType {

    /**
     * process OK
     */
    OK,
    /**
     * process warning
     */
    WARNING,
    /**
     * process error
     */
    ERROR;

    @Override
    public String toString() {
        return this.name();
    }

    //    /**
    //     * valeur de l'erreur, cette valeur correspond a la valeur en base
    //     */
    //    private int value;
    //
    //    /**
    //     * chaine de caractere explicitant la valeur
    //     */
    //    private final String meaning;
    //
    //    private static Map<Integer, ErrorType> hashMap = new HashMap<Integer, ErrorType>();
    //
    //    public static final ErrorType OK = new ErrorType(1, "OK");
    //
    //    public static final ErrorType WARNING = new ErrorType(2, "WARNING");
    //
    //    public static final ErrorType ERROR = new ErrorType(3, "ERROR");
    //
    //    /**
    //     * constructeur prive
    //     * 
    //     * @param newValue
    //     * @param newMeaning
    //     * 
    //     */
    //    private ErrorType(int newValue, String newMeaning) {
    //        value = newValue;
    //        meaning = newMeaning;
    //        hashMap.put(new Integer(newValue), this);
    //    }
    //
    //    /**
    //     * Parse
    //     * 
    //     * @param newValue
    //     * @return ErrorType
    //     */
    //    public static ErrorType parse(Integer newValue) {
    //        return hashMap.get(newValue);
    //    }
    //
    //    /**
    //     * Constructeur public
    //     * 
    //     * @param newMeaning
    //     */
    //    public ErrorType(String newMeaning) {
    //        meaning = newMeaning;
    //        if ("OK".equals(meaning)) {
    //            value = OK.value;
    //        } else if ("WARNING".equals(meaning)) {
    //            value = WARNING.value;
    //        } else if ("ERROR".equals(meaning)) {
    //            value = ERROR.value;
    //        } else {
    //            value = 0;
    //        }
    //    }
    //
    //    @Override
    //    public int hashCode() {
    //        final int prime = 31;
    //        int result = 1;
    //        result = prime * result + ((meaning == null) ? 0 : meaning.hashCode());
    //        result = prime * result + value;
    //        return result;
    //    }
    //
    //    @Override
    //    public boolean equals(Object obj) {
    //        if (this == obj)
    //            return true;
    //        if (obj == null)
    //            return false;
    //        if (getClass() != obj.getClass())
    //            return false;
    //        ErrorType other = (ErrorType) obj;
    //        if (meaning == null) {
    //            if (other.meaning != null)
    //                return false;
    //        } else if (!meaning.equals(other.meaning))
    //            return false;
    //        if (value != other.value)
    //            return false;
    //        return true;
    //    }
    //
    //    public String getMeaning() {
    //        return meaning;
    //    }
    //
    //    public int getValue() {
    //        return value;
    //    }
}
