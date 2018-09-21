/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.module.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.IModuleManager;

/**
 * This annotation is deprecated. You have to create a file "module.properties" in association with your
 * {@link IModuleManager}. You can see {@link AbstractModuleManager} for required content.
 *
 * @author Marc Sordi
 *
 */
@Deprecated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleInfo {

    /**
     *
     * @return name of the module
     */
    String name();

    /**
     *
     * @return description of the module
     */
    String description() default "";

    /**
     *
     * @return version of the module
     */
    String version();

    /**
     *
     * @return author of the module
     */
    String author();

    /**
     *
     * @return legal owner of the module
     */
    String legalOwner();

    /**
     *
     * @return link to the documentation of the module
     */
    String documentation();

}
