/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.jpa.utils;

import org.flywaydb.core.api.migration.JavaMigration;

import java.util.List;

/**
 * A flyway java migration that is module-aware. Such a migration declares a module that it belongs to, and is run in
 * the same batch as other .sql scripts and java migrations that belong to the same module.
 * <p>
 * Implementation example:
 * <pre>
 * {@code
 *     @Component
 *     public class V2_0_1__MyMigration extends BaseJavaMigration implements RegardsJavaMigration {
 *
 *         @Override
 *         public String getModuleName() {
 *             return "myModule";
 *         }
 *
 *     }
 * }
 * </pre>
 *
 * @author Julien Canches
 */
public interface RegardsJavaMigration extends JavaMigration {

    /**
     * The module name. Other .sql scripts and java migrations that belong to the same module will be run in the same
     * batch as this migration (in the order of the versions).
     */
    String getModuleName();

    /**
     * The names of the modules that this migration depends on. The module that this migration belongs to will be set
     * as depending of the modules returned by this method, in additional to dependencies declared in the module's
     * file
     */
    default List<String> getDependencies() {
        return List.of();
    }

}
