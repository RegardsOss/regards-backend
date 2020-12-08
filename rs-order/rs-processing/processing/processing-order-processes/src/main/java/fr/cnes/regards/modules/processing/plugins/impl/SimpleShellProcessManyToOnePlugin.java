/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.plugins.impl;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.processing.domain.engine.IOutputToInputMapper;
import fr.cnes.regards.modules.processing.order.Cardinality;
import fr.cnes.regards.modules.processing.order.OrderProcessInfo;
import fr.cnes.regards.modules.processing.order.Scope;
import fr.cnes.regards.modules.processing.order.SizeLimit;

/**
 * This class is a sample plugin launching a shell script.
 *
 * @author gandrieu
 */
@Plugin(id = SimpleShellProcessManyToOnePlugin.SIMPLE_SHELL_PROCESS_MANY_TO_ONE_PLUGIN, version = "1.0.0-SNAPSHOT",
        description = "Launch a shell script", author = "REGARDS Team", contact = "regards@c-s.fr", license = "GPLv3",
        owner = "CSSI", url = "https://github.com/RegardsOss", markdown = "SimpleShellProcessManyToOnePlugin.md")
public class SimpleShellProcessManyToOnePlugin extends AbstractSimpleShellProcessPlugin {

    public static final String SIMPLE_SHELL_PROCESS_MANY_TO_ONE_PLUGIN = "SimpleShellProcessManyToOnePlugin";

    @Override
    public IOutputToInputMapper inputOutputMapper() {
        return IOutputToInputMapper.allMappings();
    }

    @Override
    public OrderProcessInfo processInfo() {
        return new OrderProcessInfo(Scope.SUBORDER, Cardinality.ONE_PER_EXECUTION,
                io.vavr.collection.List.of(DataType.RAWDATA),
                new SizeLimit(maxFilesInInput == 0L ? SizeLimit.Type.NO_LIMIT : SizeLimit.Type.FILES, maxFilesInInput),
                sizeForecast().get());
    }
}
