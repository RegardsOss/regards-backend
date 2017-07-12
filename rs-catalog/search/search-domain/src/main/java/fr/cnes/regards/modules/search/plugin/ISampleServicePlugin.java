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
package fr.cnes.regards.modules.search.plugin;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 * ISampleServicePlugin
 * 
 * @author Christophe Mertz
 *
 */
@PluginInterface(description = "hello sample plugin interface")
public interface ISampleServicePlugin extends IService {

    /**
     * constant suffix
     */
    public static final String SUFFIXE = "suffix";

    /**
     * constant is active
     */
    public static final String ACTIVE = "isActive";

    /**
     * constant coeff
     */
    public static final String COEFF = "coeff";

    /**
     * method echo
     * 
     * @param pMessage
     *            message to display
     * 
     * @return the message
     */
    String echo(String pMessage);

    /**
     * method add
     * 
     * @param pFirst
     *            first element
     * @param pSecond
     *            second item
     * @return the result
     */
    int add(int pFirst, int pSecond);

}
