/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.rest.assembler;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.SimpleResourceAssemblerSupport;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

/**
 * Custom {@link SimpleResourceAssemblerSupport} for {@link PluginServiceDto}s.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class PluginServiceDtoResourcesAssembler extends SimpleResourceAssemblerSupport<PluginServiceDto> {

    /**
     * The resource service
     */
    private final IResourceService resourceService;

    /**
     *
     */
    public PluginServiceDtoResourcesAssembler(IResourceService resourceService) {
        super();
        this.resourceService = resourceService;
    }

    @Override
    public EntityModel<PluginServiceDto> toModel(PluginServiceDto element) {
        return resourceService.toResource(element);

    }

}
