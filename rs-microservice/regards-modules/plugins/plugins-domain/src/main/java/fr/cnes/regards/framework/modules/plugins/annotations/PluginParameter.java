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
package fr.cnes.regards.framework.modules.plugins.annotations;

import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.PluginOverridenParamType;

import java.lang.annotation.*;
import java.util.Map;

/**
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
     * @return the parameter's name used as a key for database registration. If not specified, falling back to class
     * field name.
     */
    String name() default "";

    /**
     * @return a human readable label for map key. This value is only required and useful for {@link Map} type
     * parameters. See {@link #label()} for map value label.
     */
    String keylabel() default "";

    /**
     * @return a required human readable label. For {@link Map} type parameters, this label is used for map value.
     * See {@link #keylabel()} for map key label.
     */
    String label();

    /**
     * Description may contain a <b>text</b> or a <b>markdown file reference (ending with .md extension)</b>. If so,
     * file must be
     * available in the same package as
     * the plugin.
     *
     * @return an optional further human readable information if the label is not explicit enough!
     */
    String description() default "";

    /**
     * Markdown file reference ending with .md extension. If not, information is skipped! If set, file must be available
     * in the same package as the plugin.<br/>
     * The system uses {@link Class#getResourceAsStream(String)} to load the file.
     * The markdown is written for REGARDS administrators.
     */
    String markdown() default "";

    /**
     * Complementary markdown file reference ending with .md extension. The same way as markdown, if the file is not
     * available information is skipped! If set, file must be available
     * in the same package as the plugin.<br/>
     * The system uses {@link Class#getResourceAsStream(String)} to load the file.
     * The markdown is written for REGARDS users.
     */
    String userMarkdown() default "";

    /**
     * Plugin parameter default value.
     *
     * @return the default parameter value
     */
    String defaultValue() default "";

    /**
     * Is the plugin parameter sensitive and should be encrypted into database?<br/>
     * BE AWARE: only plugin parameters of type {@link String} can be sensitive.
     *
     * @return true if the plugin parameter is sensitive.
     */
    boolean sensitive() default false;

    /**
     * Is the Plugin parameter is mandatory ?
     *
     * @return true if the plugin parameter is mandatory.
     */
    boolean optional() default false;

    /**
     * Is the plugin parameter configurable by the users?
     *
     * @return true if the plugin parameter is not configurable by users
     */
    boolean unconfigurable() default false;

    /**
     * Optional attribute overriding the type of the parameter
     * If equals to TO_BE_COMPUTED, the type of the parameter will be computed automatically
     *
     * @return the type of the parameter
     */
    PluginOverridenParamType type() default PluginOverridenParamType.TO_BE_COMPUTED;
}
