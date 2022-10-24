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
package fr.cnes.regards.modules.ltamanager.service.session;

import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionRequestInfoDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.session.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

/**
 * Lta session status, with requests infos paginated.
 *
 * @author Thomas GUILLOU
 **/
public class SessionInfoItemized extends PageImpl<SubmissionRequestInfoDto> {

    private SessionStatus globalStatus;

    public <T> SessionInfoItemized(SessionStatus sessionStatus,
                                   List<SubmissionRequestInfoDto> resources,
                                   Page<T> pageRequests) {
        super(resources, pageRequests.getPageable(), pageRequests.getTotalElements());
        this.globalStatus = sessionStatus;
    }

    public SessionStatus getGlobalStatus() {
        return globalStatus;
    }
}
