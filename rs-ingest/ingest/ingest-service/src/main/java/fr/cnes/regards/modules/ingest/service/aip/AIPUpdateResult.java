/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.aip;

import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;

/**
 * Result object to inform changes in an {@link AIPEntity}.
 *
 * @author Sébastien Binda
 */
public class AIPUpdateResult {

    /**
     * {@link AIPEntity} has been updated
     */
    private boolean aipEntityUpdated = false;

    /**
     * {@link AIPDto} has been updated
     */
    private boolean aipUpdated = false;

    public static AIPUpdateResult build(boolean aipEntityUpdated, boolean aipUpdated) {
        AIPUpdateResult r = new AIPUpdateResult();
        r.aipEntityUpdated = aipEntityUpdated;
        r.aipUpdated = aipUpdated;
        return r;
    }

    public boolean isAipEntityUpdated() {
        return aipEntityUpdated;
    }

    public boolean isAipUpdated() {
        return aipUpdated;
    }

}
