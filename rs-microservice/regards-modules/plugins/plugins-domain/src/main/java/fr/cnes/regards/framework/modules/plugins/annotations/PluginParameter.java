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

import fr.cnes.regards.framework.modules.plugins.domain.PluginParameterType;

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
 * </ul>
 *
 * @author Christophe Mertz
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PluginParameter {

    /**
     *
     * Plugin parameter name. The parameter name defined here are managed by the PluginManager service.
     *
     * @return the plugin parameter name
     */
    String name();

    /**
     * Plugin parameter default value.
     * 
     * @return the default parameter value
     */
    String defaultValue() default "";

    PluginParameterType.ParamType paramType() default PluginParameterType.ParamType.UNDEFINED;

    /**
     * Is the Plugin parameter is mandatory ?
     * 
     * @return true if the plugin parameter is mandatory. 
     */
    boolean optional() default false;

    /**
     *
     * Parameter description to explain the expected value if the name is not explicit enough.
     *
     * @return plugin parameter's description
     */
    String description() default "";

    Class type() default void.class;
}
