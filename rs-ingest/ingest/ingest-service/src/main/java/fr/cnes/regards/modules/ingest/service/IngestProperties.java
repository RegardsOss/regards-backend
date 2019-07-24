/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service;

/**
 * Global ingest properties
 *
 * @author Marc SORDI
 *
 */
public final class IngestProperties {

    /**
     * All transactions only manage at most {@link #WORKING_UNIT} entities at a time
     * in order to take care of the memory consumption and potential tenant starvation.
     */
    public static final Integer WORKING_UNIT = 100;

    private IngestProperties() {
        // Nothing to do
    }
}
