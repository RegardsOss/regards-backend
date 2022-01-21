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
package fr.cnes.regards.modules.ingest.domain.plugin;

import java.util.List;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.exception.AIPGenerationException;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;

/**
 * Third <b>required</b> step of the SIP processing chain.
 *
 * @author Marc Sordi
 *
 */
@FunctionalInterface
@PluginInterface(description = "AIP generation plugin contract")
public interface IAipGeneration {

    /**
     * Generate one or more {@link AIP} from given {@link SIPEntity}.<br>
     *     Know there are some rules about generating AIPs:
     *     <ol>
     *         <li>There can be no aip of same version using the same providerId.
     *         We advice you to take providerId from sip and tweak it</li>
     *         <li>AIP id must be form using tenant and entityType parameters</li>
     *     </ol>
     * @param sip {@link SIPEntity}
     * @param tenant tenant to use to create AIP id
     * @param entityType {@link EntityType} to use to create AIP id
     * @return generated {@link AIP}s
     */
    List<AIP> generate(SIPEntity sip, String tenant, EntityType entityType) throws AIPGenerationException;
}
