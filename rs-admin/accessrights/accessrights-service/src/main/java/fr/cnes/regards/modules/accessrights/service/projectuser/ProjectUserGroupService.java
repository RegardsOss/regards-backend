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
package fr.cnes.regards.modules.accessrights.service.projectuser;

import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.ProjectUserSpecificationsBuilder;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.SearchProjectUserParameters;
import fr.cnes.regards.modules.dam.client.dataaccess.IAccessGroupClient;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Service
@MultitenantTransactional
public class ProjectUserGroupService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectUserGroupService.class);

    private final IProjectUserRepository projectUserRepository;

    private final IAccessGroupClient accessGroupClient;

    private final Gson gson;

    public ProjectUserGroupService(IProjectUserRepository projectUserRepository,
                                   IAccessGroupClient accessGroupClient,
                                   Gson gson) {
        this.projectUserRepository = projectUserRepository;
        this.accessGroupClient = accessGroupClient;
        this.gson = gson;
    }

    public void validateAccessGroups(Set<String> accessGroups, boolean checkPublic)
        throws EntityNotFoundException, EntityInvalidException {
        try {
            FeignSecurityManager.asSystem();
            for (String group : accessGroups) {
                ResponseEntity<EntityModel<AccessGroup>> response = accessGroupClient.retrieveAccessGroup(group);
                if (response == null
                    || !response.getStatusCode().is2xxSuccessful()
                    || HateoasUtils.unwrap(response.getBody()) == null) {
                    throw new EntityNotFoundException(group, AccessGroup.class);
                } else {
                    if (checkPublic && HateoasUtils.unwrap(response.getBody()).isPublic()) {
                        throw new EntityInvalidException("Public group membership can't be modified");
                    }
                }
            }
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            LOG.error(e.getMessage(), e);
            ServerErrorResponse errorResponse = gson.fromJson(e.getResponseBodyAsString(), ServerErrorResponse.class);
            throw new EntityInvalidException(errorResponse.getMessages());
        } finally {
            FeignSecurityManager.reset();
        }
    }

    public void addPublicGroup(String accessGroup) {
        Pageable pageable = PageRequest.of(0, 100);
        Page<ProjectUser> projectUserPage;
        do {
            projectUserPage = projectUserRepository.findAll(pageable);
            pageable = pageable.next();
            projectUserPage.getContent().forEach(projectUser -> projectUser.getAccessGroups().add(accessGroup));
        } while (projectUserPage.hasNext());
    }

    public void removeGroup(String accessGroup) {
        removeGroup(accessGroup, (group, pageable) -> {
            SearchProjectUserParameters filters = new SearchProjectUserParameters().withAccessGroupsIncluded(Arrays.asList(
                accessGroup));
            return projectUserRepository.findAll(new ProjectUserSpecificationsBuilder().withParameters(filters).build(),
                                                 pageable);
        });
    }

    private void removeGroup(String accessGroup, BiFunction<String, Pageable, Page<ProjectUser>> find) {
        Pageable pageable = PageRequest.of(0, 100);
        Page<ProjectUser> projectUserPage;
        do {
            projectUserPage = find.apply(accessGroup, pageable);
            pageable = pageable.next();
            projectUserPage.getContent().forEach(projectUser -> projectUser.getAccessGroups().remove(accessGroup));
        } while (projectUserPage.hasNext());
    }

    public Set<String> getPublicGroups() throws EntityInvalidException {
        Set<String> publicGroups;
        try {
            FeignSecurityManager.asSystem();
            publicGroups = HateoasUtils.retrieveAllPages(100,
                                                         pageable -> accessGroupClient.retrieveAccessGroupsList(true,
                                                                                                                pageable.getPageNumber(),
                                                                                                                pageable.getPageSize()))
                                       .stream()
                                       .map(AccessGroup::getName)
                                       .collect(Collectors.toSet());
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            LOG.error(e.getMessage(), e);
            ServerErrorResponse errorResponse = gson.fromJson(e.getResponseBodyAsString(), ServerErrorResponse.class);
            throw new EntityInvalidException(errorResponse.getMessages());
        } finally {
            FeignSecurityManager.reset();
        }
        return publicGroups;
    }

    public void linkAccessGroups(String email, List<String> groups) throws EntityNotFoundException {
        ProjectUser projectUser = projectUserRepository.findOneByEmail(email)
                                                       .orElseThrow(() -> new EntityNotFoundException(email,
                                                                                                      ProjectUser.class));
        if (!CollectionUtils.isEmpty(groups)) {
            projectUser.getAccessGroups().addAll(groups);
        }
    }

}
