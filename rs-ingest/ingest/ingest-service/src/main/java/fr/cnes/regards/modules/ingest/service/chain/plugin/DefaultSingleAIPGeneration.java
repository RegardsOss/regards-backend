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
package fr.cnes.regards.modules.ingest.service.chain.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.plugin.IAipGeneration;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;

/**
 * Default AIP generation plugin. The plugin automatically build a single AIP based on SIP information.
 *
 * @author Marc Sordi
 */
@Plugin(author = "REGARDS Team", description = "Default single AIP generation", id = "DefaultSingleAIPGeneration",
        version = "1.0.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class DefaultSingleAIPGeneration implements IAipGeneration {

    @Override
    public List<AIP> generate(SIPEntity sip, String tenant, EntityType entityType) {
        List<AIP> aips = new ArrayList<>();
        // in this case we just use SIP providerId as there is only one AIP generated, no need to tweak it
        String providerId = sip.getProviderId();
        Integer version = sip.getVersion();
        aips.add(AIP.build(sip.getSip(),
                           new OaisUniformResourceName(OAISIdentifier.AIP,
                                                       entityType,
                                                       tenant,
                                                       UUID.fromString(providerId),
                                                       version),
                           Optional.of(sip.getSipIdUrn()),
                           providerId,
                           version));
        return aips;
    }
}
