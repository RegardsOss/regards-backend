/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * Provides information on how many executions are needed.
 *
 * @author gandrieu
 */
public enum Scope {

    /**
     * One execution per suborder, the execution has all the files
     * in the suborder as input files
     */
    // TODO: add suborder constraints (max files, max size, etc.),
    // for now left to the rs-order suborder config (this could go in the OrderProcessInfo)
    SUBORDER,

    /**
     * One execution per feature, the execution has only the files
     * for the given feature.
     */
    FEATURE;

}
