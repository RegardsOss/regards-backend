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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.check;

import java.util.Map;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.entities.domain.Dataset;

/**
 * Manage Spot4 Doris1B data prefixes.<br>
 * This {@link Plugin} checks that the file exists and is accessible and add a prefix to the product name.
 * 
 * @author Christophe Mertz
 *
 */
@Plugin(description = "Spot4Doris1BCheckingPlugin", id = "Spot4Doris1BCheckingPlugin", version = "1.0.0",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class Spot4Doris1BCheckingPlugin extends AbstractDoris1BCheckingPlugin {

    /**
     * Initialize the {@link Map} for the association {@link Dataset} name, prefix.
     */
    public void initPrefixMap() {
        addDatasetNamePrexif("DA_TC_SPOT4_DORIS1B_MOE_CDDIS", PREFIX_MOE_CDDIS);
        addDatasetNamePrexif("DA_TC_SPOT4_DORIS1B_MOE_CDDIS_COM", PREFIX_MOE_CDDIS_COM);
        addDatasetNamePrexif("DA_TC_SPOT4_DORIS1B_POE_CDDIS_COM", PREFIX_POE_CDDIS_COM);
    }
}
