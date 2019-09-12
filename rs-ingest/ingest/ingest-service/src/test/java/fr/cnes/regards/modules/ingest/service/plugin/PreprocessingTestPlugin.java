/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.exception.InvalidSIPReferenceException;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipPreprocessing;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIPReference;
import fr.cnes.regards.modules.ingest.service.chain.ProcessingChainTestErrorSimulator;

/**
 * Test plugin for the processing chains.
 * @author Sébastien Binda
 */
@Plugin(author = "REGARDS Team", description = "Test plugin for SIP preprocessing", id = "TestSIPPreprocessing",
        version = "1.0.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class PreprocessingTestPlugin implements ISipPreprocessing {

    public static final String SIP_ID_TEST = "SIPID_001";

    @Autowired
    private ProcessingChainTestErrorSimulator errorSimulator;

    @Override
    public void preprocess(SIP pSip) throws ProcessingStepException {
        if (PreprocessingTestPlugin.class.equals(errorSimulator.getSimulateErrorForStep())) {
            throw new ProcessingStepException("Simulated exception for step PreprocessingTestPlugin");
        }
    }

    @Override
    public SIP read(SIPReference pRef) throws InvalidSIPReferenceException {
        if (PreprocessingTestPlugin.class.equals(errorSimulator.getSimulateErrorForStep())) {
            throw new InvalidSIPReferenceException("Simulated exception for step PreprocessingTestPlugin");
        }
        // Simulate creation of a new SIP
        return SIP.build(EntityType.DATA, SIP_ID_TEST);
    }

}
