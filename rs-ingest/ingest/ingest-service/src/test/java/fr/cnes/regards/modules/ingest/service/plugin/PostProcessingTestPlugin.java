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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipPostprocessing;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.PostProcessResult;
import fr.cnes.regards.modules.ingest.service.chain.ProcessingChainTestErrorSimulator;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

/**
 * @author Marc SORDI
 */
@Plugin(author = "REGARDS Team", description = "Test plugin for SIP postprocessing", id = "PostProcessingTestPlugin",
    version = "1.0.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
    url = "https://regardsoss.github.io/")
public class PostProcessingTestPlugin implements ISipPostprocessing {

    @Autowired
    private ProcessingChainTestErrorSimulator errorSimulator;

    @Override
    public PostProcessResult postprocess(Collection<AIPEntity> aipEntities) throws ModuleException {
        if (PostProcessingTestPlugin.class.equals(errorSimulator.getSimulateErrorForStep())) {
            throw new ModuleException("Simulated exception for step PreprocessingTestPlugin");
        }
        return null;
    }

}
