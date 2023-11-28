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
package fr.cnes.regards.modules.processing.controller;

import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import io.vavr.collection.Stream;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.stream.Collectors;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.METADATA_SUFFIX;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESSPLUGIN_PATH;

/**
 * This class is the controller to create {@link fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration}
 * out of {@link PluginMetaData} found in the classpath corresponding to processes.
 *
 * @author gandrieu
 */
@RestController
@RequestMapping(path = PROCESSPLUGIN_PATH + METADATA_SUFFIX)
public class ProcessPluginMetadataController {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "List all detected process plugins", role = DefaultRole.ADMIN)
    public Collection<PluginMetaData> listAllDetectedPlugins() {
        return Stream.ofAll(PluginUtils.getPlugins().values())
                     .filter(md -> md.getInterfaceNames()
                                     .stream()
                                     .anyMatch(i -> i.endsWith(IProcessDefinition.class.getName())))
                     .collect(Collectors.toList());
    }

}
