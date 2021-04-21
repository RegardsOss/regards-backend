/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.dto.aip;

import java.util.Optional;

import fr.cnes.regards.framework.oais.builder.IPBuilder;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;

/**
 * AIP Builder. Used to create AIP.
 * @author Marc Sordi
 *
 * @deprecated {@link AIP} fluent API instead
 */
@Deprecated
public class AIPBuilder extends IPBuilder<AIP> {

    /**
     * Init AIP builder
     * @param aipId required archival information package identifier
     * @param sipId optional submission information package identifier (may be null!)
     * @param providerId required provider id
     * @param entityType entity type
     */
    public AIPBuilder(OaisUniformResourceName aipId, Optional<OaisUniformResourceName> sipId, String providerId,
            EntityType entityType) {
        super(AIP.class, entityType);
        ip.setId(aipId);
        ip.setSipId(sipId.orElse(null));
        ip.setProviderId(providerId);
    }

    /**
     * Constructor used to initialize the builder from an AIP
     * @param toBeUpdated
     */
    public AIPBuilder(AIP toBeUpdated) {
        super(toBeUpdated);
        ip.setId(toBeUpdated.getId());
        ip.setSipId(toBeUpdated.getSipId().orElse(null));
        ip.setProviderId(toBeUpdated.getProviderId());
    }
}
