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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipValidation;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.chain.ProcessingChainTestErrorSimulator;

/**
 * Test plugin for the processing chains.
 * @author Sébastien Binda
 */
@Plugin(author = "REGARDS Team", description = "Test plugin for SIP validation", id = "TestSIPValidation",
        version = "1.0.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class ValidationTestPlugin implements ISipValidation {

    @Autowired
    private ProcessingChainTestErrorSimulator errorSimulator;

    @Override
    public void validate(final SIP sip, Errors errors) {
        if (ValidationTestPlugin.class.equals(errorSimulator.getSimulateErrorForStep())) {
            errors.rejectValue("SimulatedErrorField", "SimulatedErrorCode");
        }
    }

}
