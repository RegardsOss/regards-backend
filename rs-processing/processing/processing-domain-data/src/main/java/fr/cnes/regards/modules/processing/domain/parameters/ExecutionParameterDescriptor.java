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
package fr.cnes.regards.modules.processing.domain.parameters;

import lombok.Value;
import lombok.With;

/**
 * This class defines an execution parameter: its name, type and description.
 *
 * @author gandrieu
 */
@Value
@With
public class ExecutionParameterDescriptor {

    /**
     * Name of the parameter.
     */
    String name;

    /**
     * Which type of parameter value is expected.
     */
    ExecutionParameterType type;

    /**
     * Short description of the parameter.
     */
    String desc;

    /**
     * True if the parameter can be ommitted.
     */
    boolean optional;

    /**
     * True if several values can be given for this parameter name
     */
    boolean repeatable;

    /**
     * True if the end-user must provide the value for this parameter,
     * meaning that the actual value has to be defined at the creation of the batch.
     * (False if it can be inferred from the context of execution.)
     */
    boolean userDefined;

}
