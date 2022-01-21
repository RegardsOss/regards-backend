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
package fr.cnes.regards.modules.ingest.service.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.exception.AIPGenerationException;
import fr.cnes.regards.modules.ingest.domain.plugin.IAipGeneration;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.service.chain.ProcessingChainTestErrorSimulator;

/**
 * Test plugin for the processing chains.
 * @author Sébastien Binda
 */
@Plugin(author = "REGARDS Team", description = "Test plugin for AIP generation", id = "TestAIPGenerator",
        version = "1.0.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class AIPGenerationTestPlugin implements IAipGeneration {

    @Autowired
    private ProcessingChainTestErrorSimulator errorSimulator;

    @Override
    public List<AIP> generate(SIPEntity sip, String tenant, EntityType entityType) throws AIPGenerationException {
        if (AIPGenerationTestPlugin.class.equals(errorSimulator.getSimulateErrorForStep())) {
            throw new AIPGenerationException("Simulated exception for step AIPGenerationTestPlugin");
        }

        List<AIP> aips = new ArrayList<>();
        OaisUniformResourceName sipIdUrn = sip.getSipIdUrn();
        Integer version = sip.getVersion();
        aips.add(AIP.build(sip.getSip(),
                           new OaisUniformResourceName(OAISIdentifier.AIP,
                                                       entityType,
                                                       tenant,
                                                       sipIdUrn.getEntityId(),
                                                       version, null, null),
                           Optional.of(sipIdUrn),
                           sip.getProviderId(),
                           version));
        return aips;
    }

}
