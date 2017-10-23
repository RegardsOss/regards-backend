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
package fr.cnes.regards.modules.ingest.domain.plugin;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.SIPReference;

/**
 * First <b>optional</b> step of the SIP processing chain
 *
 * @author Marc Sordi
 *
 */
@PluginInterface(description = "SIP preprocessing plugin contract")
public interface IPreprocessSIP {

    /**
     * Allows to make some action before SIP processing starts.
     * @param sip {@link SIP} to be processed
     */
    void preprocess(SIP sip);

    /**
     * Read a referenced {@link SIP} (only available for referenced SIP!)
     * @param ref {@link SIPReference}
     * @return a completely filled {@link SIP}
     */
    SIP read(SIPReference ref);
}
