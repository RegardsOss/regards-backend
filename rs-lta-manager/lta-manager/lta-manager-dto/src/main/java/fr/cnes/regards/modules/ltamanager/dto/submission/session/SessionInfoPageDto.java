/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ltamanager.dto.submission.session;

import org.springframework.hateoas.PagedModel;

/**
 * Override default page model by adding the status attribute.
 * To get pagination links when encapsulate into Resource, we need to override PageModel
 *
 * @author Thomas GUILLOU
 **/
public class SessionInfoPageDto<T> extends PagedModel<T> {

    private SessionStatus globalStatus;

    public SessionInfoPageDto(SessionStatus globalStatus, PagedModel<T> pageModel) {
        super(pageModel.getContent(), pageModel.getMetadata(), pageModel.getLinks());
        this.globalStatus = globalStatus;
    }

    public SessionStatus getGlobalStatus() {
        return globalStatus;
    }
}
