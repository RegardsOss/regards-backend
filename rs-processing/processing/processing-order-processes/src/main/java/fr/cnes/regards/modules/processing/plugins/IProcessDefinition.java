/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.plugins;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.processing.order.OrderProcessInfo;

/**
 * This interface is an aggregation of interfaces defining processes in the context of rs-order, to be mapped
 * onto the {@link fr.cnes.regards.modules.processing.domain.PProcess} interface.
 *
 * @author gandrieu
 */
@PluginInterface(description = "Defines the quotas, rights, parameters and launching properties for a Process")
public interface IProcessDefinition extends IProcessCheckerDefinition,
    IProcessParametersDefinition,
    IProcessLauncherDefinition,
    IProcessForecastDefinition
{

    OrderProcessInfo processInfo();

}
