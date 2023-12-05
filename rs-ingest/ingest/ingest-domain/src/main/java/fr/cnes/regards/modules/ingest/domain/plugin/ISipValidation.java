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

import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import org.springframework.validation.Errors;

/**
 * Second <b>required</b> step of the SIP processing chain
 *
 * @author Marc Sordi
 */
@FunctionalInterface
@PluginInterface(description = "SIP validation plugin contract")
public interface ISipValidation {

    /**
     * Allows to validate SIP
     *
     * @param sip    {@link SIPDto} to be validated
     * @param errors list of validation errors
     */
    void validate(final SIPDto sip, Errors errors);
}
