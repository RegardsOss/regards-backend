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
package fr.cnes.regards.framework.modules.plugins.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * Annotate a plugin parameter. Following field types are supported for injection :
 * <ul>
 * <li>String</li>
 * <li>Byte</li>
 * <li>Short</li>
 * <li>Integer</li>
 * <li>Long</li>
 * <li>Float</li>
 * <li>Double</li>
 * <li>Boolean</li>
 * <li>Collection</li>
 * <li>Plugin interface</li>
 * <li>Map</li>
 * <li>POJO</li>
 * </ul>
 *
 * @author Christophe Mertz
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PluginParameter {

    /**
     * @return a required human readable label
     */
    String label();

    /**
     * @return an optional further human readable information if the label is not explicit enough!
     */
    String description() default "";

    /**
     * Plugin parameter default value.
     *
     * @return the default parameter value
     */
    String defaultValue() default "";

    /**
     * Is the Plugin parameter is mandatory ?
     *
     * @return true if the plugin parameter is mandatory.
     */
    boolean optional() default false;

    // FIXME run CI before remove
    // /**
    // * Raw type can be specified here to explicitly tell GSON which type to deserialize to! Otherwise, plugin engine
    // * will try to guess it.
    // * @return raw parameter type
    // */
    // Class<?> rawtype() default Void.class;
    //
    // /**
    // * Argument types can be explicitly specified here for parameterized raw types for GSON deserialization!
    // Otherwise,
    // * plugin engine will try to guess them.
    // * @return list of generic types affecting the raw parameter type
    // */
    // Class<?>[] argTypes() default {};
}
