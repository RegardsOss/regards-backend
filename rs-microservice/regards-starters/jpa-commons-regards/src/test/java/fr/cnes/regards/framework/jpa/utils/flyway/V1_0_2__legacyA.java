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
package fr.cnes.regards.framework.jpa.utils.flyway;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

/**
 * This test migration class does not implement {@link fr.cnes.regards.framework.jpa.utils.RegardsJavaMigration},
 * so it is processed as a "legacy" java migration, and as such it is run after all .sql scripts, even if they have a
 * higher version.
 *
 * @author Julien Canches
 */
@Component
public class V1_0_2__legacyA extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        // Nothing
    }

}
