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

import java.lang.annotation.*;

/**
 * This annotation can be used to initialize a plugin. It must either be used :
 * <ul>
 *  <li>On a no-arg method when {@link #hasConfiguration()} is false</ui>
 *  <li>On a method with an unique argument of type
 *  {@link fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration PluginConfiguration} when
 *  {@link #hasConfiguration()} is true </ul>
 * </ul>
 * The method is called after parameter injection.
 *
 * @author Christophe Mertz
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PluginInit {

    boolean hasConfiguration() default false;
}
