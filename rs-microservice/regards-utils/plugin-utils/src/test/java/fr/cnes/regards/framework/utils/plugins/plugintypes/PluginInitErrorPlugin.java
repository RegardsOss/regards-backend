/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.plugins.plugintypes;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.domain.exception.PluginInitException;
import fr.cnes.regards.framework.utils.plugins.basic.ISamplePlugin;

/**
 * @author Léo Mieulet
 */
@Plugin(description = "Plugin init error plugin test", id = "PluginInitErrorPlugin", version = "0.0.1",
    author = "REGARDS Team", contact = "regards@c-s.fr", license = "GPLv3", owner = "CS",
    url = "https://github.com/RegardsOss")
public class PluginInitErrorPlugin implements ISamplePlugin {

    /**
     * Plugin init method
     */
    @PluginInit
    public void init() throws PluginInitException {
        throw new PluginInitException("This plugin does not like to be run, sorry");
    }

    @Override
    public String echo(String pMessage) {
        return this.getClass().getName();
    }

    @Override
    public int add(int pFist, int pSecond) {
        return 0;
    }

}
