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
 * Class RegardsPlugin Main representation of plugin meta-data.
 *
 * @author Christophe Mertz
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Plugin {

    /**
     * Unique Id of the plugin. If the given id is not unique, an error is thrown during plugin loading. Class canonical
     * name is retain as a default value.
     *
     * @return the plugin's id
     */
    String id();

    /**
     * Description of the plugin. Simple user information.
     *
     * @return the plugin's description
     */
    String description();

    /**
     * Version of the plugin. Use to check if the plugin changed
     *
     * @return the plugin's version
     */
    String version();

    /**
     * An URL link to the web site of the plugin.
     *
     * @return the plugin's url
     */
    String url();

    /**
     * Author of the plugin. Simple user information.
     *
     * @return the plugin's author
     */
    String author();

    /**
     * An email to contact the plugin's author.
     *
     * @return the email of the plugin's author
     */
    String contact();

    /**
     * The legal owner of the plugin.
     *
     * @return the plugin's legal owner
     */
    String owner();

    /**
     * Licence of the plugin.
     *
     * @return the plugin's licence
     */
    String licence();

}
