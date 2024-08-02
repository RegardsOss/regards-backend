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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.ingest.dto;

/**
 * @author mnguyen0
 * @author Sylvai VISSIERE-GUERINET
 */
public enum VersioningMode {

    /**
     * This new version is simply ignored, nothing more is being done.
     */
    IGNORE,
    /**
     * This new version is taken into consideration, old versions are still there.
     */
    INC_VERSION,
    /**
     * This new version is to be handled by a human who will decide what to do.
     */
    MANUAL,
    /**
     * This new version will replace the old one.
     * That means old versions will be marked as deleted once the new version has been successfully handled
     */
    REPLACE
}
