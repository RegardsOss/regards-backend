/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.config;

import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO.Rights;

/**
 * POJO used to import/export process configurations
 *
 * @author Sébastien Binda
 *
 */
public class ProcessConfigurationDTO {

    private String pluginConfBid;

    private Rights rights;

    public ProcessConfigurationDTO(String pluginConfBid, Rights rights) {
        super();
        this.pluginConfBid = pluginConfBid;
        this.rights = rights;
    }

    public String getPluginConfBid() {
        return pluginConfBid;
    }

    public void setPluginConfBid(String pluginConfBid) {
        this.pluginConfBid = pluginConfBid;
    }

    public Rights getRights() {
        return rights;
    }

    public void setRights(Rights rights) {
        this.rights = rights;
    }

}
