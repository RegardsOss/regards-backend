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
package fr.cnes.regards.modules.ltamanager.rest.submission;

import fr.cnes.regards.framework.hateoas.IResourceService;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Abstract controller to handle CRUD operations on
 * {@link fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest}
 **/
@RequestMapping(AbstractSubmissionController.ROOT_PATH)
public abstract class AbstractSubmissionController {

    public static final String ROOT_PATH = "/products";

    protected final IResourceService resourceService;

    protected AbstractSubmissionController(IResourceService resourceService) {
        this.resourceService = resourceService;
    }

}
