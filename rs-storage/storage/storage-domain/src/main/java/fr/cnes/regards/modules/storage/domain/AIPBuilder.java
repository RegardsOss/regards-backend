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
package fr.cnes.regards.modules.storage.domain;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Collection;

import org.springframework.util.Assert;

import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.builder.IPBuilder;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;

/**
 *
 * AIP Builder. Used to create AIP.
 * @author Marc Sordi
 *
 */
public class AIPBuilder extends IPBuilder<AIP> {

    /**
     * Init AIP builder
     * @param ipId required information package identifier
     * @param sipId SIP identifier (may be null)
     * @param entityType
     */
    public AIPBuilder(UniformResourceName ipId, @Nullable String sipId, EntityType entityType) {
        super(AIP.class, entityType);
        ip.setId(ipId);
        ip.setSipId(sipId);
    }

    public AIPBuilder(AIP oldAip) {
        super(oldAip);
    }
}
