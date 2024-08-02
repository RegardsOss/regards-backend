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
package fr.cnes.regards.modules.model.dto.properties;

/**
 * List of available attribute types
 *
 * @author msordi
 */
public enum PropertyType {

    /**
     * Possible attribute type
     */
    STRING,
    JSON,
    INTEGER,
    DOUBLE,
    DATE_ISO8601,
    URL,
    BOOLEAN,
    STRING_ARRAY {
        @Override
        public boolean isArray() {
            return true;
        }
    },
    INTEGER_ARRAY {
        @Override
        public boolean isArray() {
            return true;
        }
    },
    DOUBLE_ARRAY {
        @Override
        public boolean isArray() {
            return true;
        }
    },
    DATE_ARRAY {
        @Override
        public boolean isArray() {
            return true;
        }
    },
    // Equivalent to the related Elasticsearch type integer_range
    INTEGER_RANGE {
        @Override
        public boolean isInterval() {
            return true;
        }
    },
    /**
     * @deprecated Use {@link #INTEGER_RANGE} instead
     */
    @Deprecated INTEGER_INTERVAL {
        @Override
        public boolean isInterval() {
            return true;
        }
    },
    // Equivalent to the related Elasticsearch type double_range
    DOUBLE_RANGE {
        @Override
        public boolean isInterval() {
            return true;
        }
    },
    /**
     * @deprecated Use {@link #DOUBLE_RANGE} instead
     */
    @Deprecated DOUBLE_INTERVAL {
        @Override
        public boolean isInterval() {
            return true;
        }
    },
    // Equivalent to the related Elasticsearch type date_range
    DATE_RANGE {
        @Override
        public boolean isInterval() {
            return true;
        }
    },
    /**
     * @deprecated Use {@link #DATE_RANGE} instead
     */
    @Deprecated DATE_INTERVAL {
        @Override
        public boolean isInterval() {
            return true;
        }
    },
    LONG,
    // Equivalent to the related Elasticsearch type long_range
    LONG_RANGE {
        @Override
        public boolean isInterval() {
            return true;
        }
    },
    /**
     * @deprecated Use {@link #LONG_RANGE} instead
     */
    @Deprecated LONG_INTERVAL {
        @Override
        public boolean isInterval() {
            return true;
        }
    },
    LONG_ARRAY {
        @Override
        public boolean isArray() {
            return true;
        }
    },
    OBJECT;

    /**
     * Tell whether or not specified type correspond to an interval
     *
     * @return {@link Boolean}
     */
    public boolean isInterval() {
        return false;
    }

    /**
     * Tell whether or not specified type correspond to an array
     *
     * @return {@link Boolean}
     */
    public boolean isArray() {
        return false;
    }
}
