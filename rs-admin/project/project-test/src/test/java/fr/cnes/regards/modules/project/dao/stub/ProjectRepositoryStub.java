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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class ProjectRepositoryStub
 *
 * Stub for JPA Repository
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Repository
@Primary
public class ProjectRepositoryStub extends JpaRepositoryStub<Project> implements IProjectRepository {

    public ProjectRepositoryStub() {
        this.entities.add(new Project(0L, "desc", "icon", true, "name"));
    }

    @Override
    public Project findOneByNameIgnoreCase(final String pName) {
        Project result = null;
        final Optional<Project> project = this.entities.stream().filter(e -> e.getName().equals(pName)).findFirst();
        if (project.isPresent()) {
            result = project.get();
        }
        return result;
    }

    @Override
    public Page<Project> findByIsPublicTrue(final Pageable pPageable) {
        final List<Project> publicProjects = new ArrayList<>();
        publicProjects
                .addAll(this.entities.stream().filter(project -> project.isPublic()).collect(Collectors.toList()));
        return new PageImpl<>(publicProjects);
    }

    @Override
    public List<Project> findByIsDeletedFalse() {
        return entities.stream().filter(e -> !e.isDeleted()).collect(Collectors.toList());
    }

    @Override
    public boolean isActiveProject(Long id) {
        return true;
    }
}
