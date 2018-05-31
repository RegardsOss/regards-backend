/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.exception.AIPGenerationException;
import fr.cnes.regards.modules.ingest.domain.plugin.IAipGeneration;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * Fake plugin for the processing chains.
 * 
 * @author Christophe Mertz
 */
@Plugin(author = "REGARDS Team", description = "Fake plugin for AIP generation", id = "FakeAIPGenerationTestPlugin",
        version = "1.0.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class FakeAIPGenerationTestPlugin implements IAipGeneration {

    @Override
    public List<AIP> generate(SIP sip, UniformResourceName ipId, String sipId) throws AIPGenerationException {

        return null;
    }

}
