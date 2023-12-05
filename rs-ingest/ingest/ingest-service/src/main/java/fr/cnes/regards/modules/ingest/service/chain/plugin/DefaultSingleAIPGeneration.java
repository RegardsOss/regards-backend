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
package fr.cnes.regards.modules.ingest.service.chain.plugin;

import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.plugin.IAipGeneration;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default AIP generation plugin. The plugin automatically build a single AIP based on SIP information.
 *
 * @author Marc Sordi
 */
@Plugin(author = "REGARDS Team",
        description = "Default single AIP generation",
        id = "DefaultSingleAIPGeneration",
        version = "1.0.0",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CNES",
        url = "https://regardsoss.github.io/")
public class DefaultSingleAIPGeneration implements IAipGeneration {

    @Override
    public List<AIPDto> generate(SIPEntity sip, String tenant, EntityType entityType) {
        List<AIPDto> aips = new ArrayList<>();
        // in this case we just use SIP providerId as there is only one AIP generated, no need to tweak it
        Integer version = sip.getVersion();
        OaisUniformResourceName sipIdUrn = sip.getSipIdUrn();
        aips.add(AIPDto.build(sip.getSip(),
                              new OaisUniformResourceName(OAISIdentifier.AIP,
                                                          entityType,
                                                          tenant,
                                                          sipIdUrn.getEntityId(),
                                                          version,
                                                          null,
                                                          null),
                              Optional.of(sipIdUrn),
                              sip.getProviderId(),
                              version));
        return aips;
    }
}
