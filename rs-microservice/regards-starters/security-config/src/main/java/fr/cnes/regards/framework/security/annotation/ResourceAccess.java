/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

import com.google.gson.annotations.JsonAdapter;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Security hook to identify and secured REST endpoint accesses.
 * @author msordi
 */
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("VOID")
@JsonAdapter(ResourceAccessAdapter.class)
public @interface ResourceAccess {

    /**
     * Describe the current feature should start with an action verb
     * @return feature description
     */
    String description();

    /**
     * If the resource access is a plugin implementation, this parameter allow to identify the plugin interface
     * @return Plugin interface class
     */
    Class<?> plugin() default Void.class;

    /**
     * Allows to configure sensible default accesses
     * @return default resource role
     */
    DefaultRole role() default DefaultRole.PROJECT_ADMIN;

}
