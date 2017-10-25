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
package fr.cnes.regards.modules.search.rest.assembler.link;

import org.springframework.hateoas.Resource;

import fr.cnes.regards.modules.entities.domain.Dataset;

/**
 * Functionnal interface for any class/service able to add HATEOAS links to a HATEOAS resource.
 * For example, it can be implemented by a controller or a resources assembler.
 *
 * @author Xavier-Alexandre Brochard
 */
@FunctionalInterface
public interface ILinksAdder {

    /**
     * Add links to the passed resource
     * @param pResource
     * @return the resource augmented with links
     */
    Resource<Dataset> addLinks(Resource<Dataset> pResource);

}