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
import fr.cnes.regards.modules.ingest.domain.exception.TagAIPException;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;

/**
 * Fourth <b>optional</b> step of the SIP processing chain.
 *
 * @author Marc Sordi
 *
 */
@FunctionalInterface
@PluginInterface(description = "AIP tag plugin contract")
public interface IAipTagging {

    /**
     * Tag AIP
     * @param aips {@link AIP} to tag
     */
    void tag(List<AIP> aips) throws TagAIPException;
}
