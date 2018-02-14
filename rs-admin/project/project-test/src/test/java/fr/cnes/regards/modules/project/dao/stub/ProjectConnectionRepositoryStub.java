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
package fr.cnes.regards.modules.project.dao.stub;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class ProjectConnectionRepositoryStub
 *
 * STUB for JPA ProjectConnection Repository.
 *
 * @author CS
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@Repository
@Primary
public class ProjectConnectionRepositoryStub extends JpaRepositoryStub<ProjectConnection>
        implements IProjectConnectionRepository {

    @Override
    public ProjectConnection findOneByProjectNameAndMicroservice(final String pProjectName,
            final String pMicroService) {
        ProjectConnection result = null;
        for (final ProjectConnection conn : this.entities) {
            if ((conn.getProject() != null) && conn.getProject().getName().equals(pProjectName)
                    && conn.getMicroservice().equals(pMicroService)) {
                result = conn;
                break;
            }
        }

        return result;
    }

    @Override
    public Page<ProjectConnection> findByProjectName(final String pProjectName, final Pageable pPageable) {
        final List<ProjectConnection> list = entities.stream()
                .filter(e -> e.getProject().getName().equals(pProjectName)).collect(Collectors.toList());
        return new PageImpl<>(list);
    }

    @Override
    public List<ProjectConnection> findByMicroserviceAndProjectIsDeletedFalse(String microservice) {
        List<ProjectConnection> list = entities.stream().filter(e -> e.getMicroservice().equals(microservice))
                .collect(Collectors.toList());
        return list;
    }

    @Override
    public List<ProjectConnection> findByMicroserviceAndStateAndProjectIsDeletedFalse(String microservice,
            TenantConnectionState state) {
        List<ProjectConnection> list = entities.stream().filter(e -> e.getMicroservice().equals(microservice))
                .collect(Collectors.toList());
        return list;
    }

    @Override
    public List<ProjectConnection> findByUserNameAndPasswordAndUrl(String username, String password, String url) {
        List<ProjectConnection> list = entities.stream().filter(e -> (e.getUserName().equals(username)
                && e.getPassword().equals(password) && e.getUrl().equals(url))).collect(Collectors.toList());
        return list;
    }

}
