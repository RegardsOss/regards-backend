/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.validation.utils;

/**
 * Utils class for checksum validation
 *
 * @author Thibaud Michaudel
 **/
public class ChecksumValidationUtils {

    private final static String MD5_PATTERN = "^[0-9a-fA-F]{32}$";

    /**
     * Validate that the checksum is well formed for the given algorithm.
     * For MD5, a checksum need to be exactly 32 hexadecimals characters to be valid
     */
    public static boolean isValidChecksum(String checksum, String algorithm) {
        if (algorithm == null || checksum == null) {
            return false;
        }
        if (algorithm.equals("MD5")) {
            return checksum.matches(MD5_PATTERN);
        }
        // Only MD5 checksum need to be validated in regards
        return true;
    }
}
