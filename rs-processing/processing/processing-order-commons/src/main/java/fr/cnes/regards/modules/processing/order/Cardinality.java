/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.order;

/**
 * Tells how many outputs there are by execution.
 * <p>
 * Any process used by rs-order must provide this piece of information
 * as part of the OrderProcessInfo.
 * <p>
 * This allows to know in advance how many OrderDataFile to create so that
 * the order metalink can be generated as soon as the order is accepted.
 *
 * @author gandrieu
 */
public enum Cardinality {

    /**
     * Each execution produces exactly one output file.
     */
    ONE_PER_EXECUTION,

    /**
     * Each execution produces exactly one file per feature.
     */
    ONE_PER_FEATURE,

    /**
     * Each execution produces exactly one output file per input file.
     */
    ONE_PER_INPUT_FILE;

}
