/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.microservices.administration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.IProjectConnectionService;
import fr.cnes.regards.modules.project.service.IProjectService;
import fr.cnes.regards.modules.project.service.ProjectConnectionService;
import fr.cnes.regards.modules.project.service.ProjectService;

/**
 *
 * Overrides the default method to initiate the list of connections for the multitenants database. The project
 * connections are read from the instance database through the ProjectService.
 *
 * @author Sébastien Binda

 */
public class LocalTenantConnectionResolver implements ITenantConnectionResolver {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(LocalTenantConnectionResolver.class);

    /**
     * The {@link ProjectService} management.
     */
    private final IProjectService projectService;

    /**
     * The {@link ProjectConnectionService} management.
     */
    private final IProjectConnectionService projectConnectionService;

    /**
     *
     * Constructor
     *
     * @param pProjectService
     *            the {@link ProjectService}
     * @param pProjectConnectionService
     *            the  {@link ProjectConnectionService}

     */
    public LocalTenantConnectionResolver(final IProjectService pProjectService,
            final IProjectConnectionService pProjectConnectionService) {
        super();
        projectService = pProjectService;
        projectConnectionService = pProjectConnectionService;
    }

    @Override
    public List<TenantConnection> getTenantConnections(String microservice) throws JpaMultitenantException {

        List<TenantConnection> tenantConnections = new ArrayList<>();

        List<ProjectConnection> projectConnections = projectConnectionService.retrieveProjectConnections(microservice);
        // Transform to tenant connection
        if (projectConnections != null) {
            for (ProjectConnection projectConnection : projectConnections) {
                tenantConnections.add(projectConnection.toTenantConnection());
            }
        }
        return tenantConnections;
    }

    @Override
    public void addTenantConnection(String microserviceName, final TenantConnection pTenantConnection)
            throws JpaMultitenantException {
        try {
            final Project project = projectService.retrieveProject(pTenantConnection.getTenant());

            final ProjectConnection projectConnection = new ProjectConnection(project,
                                                                              microserviceName,
                                                                              pTenantConnection.getUserName(),
                                                                              pTenantConnection.getPassword(),
                                                                              pTenantConnection.getDriverClassName(),
                                                                              pTenantConnection.getUrl());

            projectConnectionService.createStaticProjectConnection(projectConnection);
        } catch (final ModuleException e) {
            LOG.error("Error adding new tenant. Cause : {}", e.getMessage());
            LOG.debug(e.getMessage(), e);
        }

    }

    @Override
    public void updateState(String microservice, String tenant, TenantConnectionState state,
            Optional<String> errorCause) throws JpaMultitenantException {
        try {
            projectConnectionService.updateState(microservice, tenant, state, errorCause);
        } catch (EntityNotFoundException e) {
            throw new JpaMultitenantException(e);
        }

    }
}
