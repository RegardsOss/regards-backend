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
package fr.cnes.regards.modules.access.services.service.ui;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.access.services.dao.ui.ILinkUIPluginsDatasetsRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginConfigurationRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginDefinitionRepository;
import fr.cnes.regards.modules.access.services.domain.event.UIPluginConfigurationEvent;
import fr.cnes.regards.modules.access.services.domain.ui.LinkUIPluginsDatasets;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;

/**
 * Class PluginConfigurationService
 *
 * Business service to manage {@link UIPluginConfiguration} entities.
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@Service
@RegardsTransactional
public class UIPluginConfigurationService implements IUIPluginConfigurationService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UIPluginConfigurationService.class);

    /**
     * Builds a pedicate telling if the passed {@link UIPluginConfiguration} is applicable on passed {@link ServiceScope}.
     * Returns <code>true</code> if passed <code>pApplicationMode</code> is <code>null</code>.
     */
    private static final Function<List<ServiceScope>, Predicate<UIPluginConfiguration>> IS_APPLICABLE_ON = pApplicationModes -> pConfiguration -> (pApplicationModes == null)
            || pConfiguration.getPluginDefinition().getApplicationModes().containsAll(pApplicationModes);

    private final IUIPluginDefinitionRepository pluginRepository;

    private final ILinkUIPluginsDatasetsRepository linkedUiPluginRespository;

    private final IUIPluginConfigurationRepository repository;

    private final IPublisher publisher;

    /**
     * Client to control roles
     */
    private final IRolesClient rolesClient;

    /**
     * Authentication resolver
     */
    private final IAuthenticationResolver authResolver;

    /**
     * @param pPluginRepository
     * @param pLinkedUiPluginRespository
     * @param pRepository
     * @param pPublisher
     */
    public UIPluginConfigurationService(IUIPluginDefinitionRepository pluginRepository,
            ILinkUIPluginsDatasetsRepository linkedUiPluginRespository, IUIPluginConfigurationRepository repository,
            IPublisher publisher, IRolesClient rolesClient, IAuthenticationResolver authResolver) {
        super();
        this.pluginRepository = pluginRepository;
        this.linkedUiPluginRespository = linkedUiPluginRespository;
        this.repository = repository;
        this.publisher = publisher;
        this.rolesClient = rolesClient;
        this.authResolver = authResolver;
    }

    @Override
    public Page<UIPluginConfiguration> retrievePluginConfigurations(final UIPluginTypesEnum pPluginType,
            final Boolean pIsActive, final Boolean pIsLinkedToAllEntities, final Pageable pPageable) {
        if ((pPluginType == null) && (pIsActive == null) && (pIsLinkedToAllEntities == null)) {
            return repository.findAll(pPageable);
        } else {
            if (pPluginType != null) {
                if ((pIsActive != null) && (pIsLinkedToAllEntities != null)) {
                    return repository.findByPluginDefinitionTypeAndActiveAndLinkedToAllEntities(pPluginType, pIsActive,
                                                                                                pIsLinkedToAllEntities,
                                                                                                pPageable);
                } else if (pIsActive != null) {
                    return repository.findByPluginDefinitionTypeAndActive(pPluginType, pIsActive, pPageable);
                } else {
                    return repository.findByPluginDefinitionType(pPluginType, pPageable);
                }
            } else if ((pIsActive != null) && (pIsLinkedToAllEntities != null)) {
                return repository.findByActiveAndLinkedToAllEntities(pIsActive, pIsLinkedToAllEntities, pPageable);
            } else if (pIsLinkedToAllEntities != null) {
                return repository.findByLinkedToAllEntities(pIsLinkedToAllEntities, pPageable);
            } else {
                return repository.findByActive(pIsActive, pPageable);
            }
        }
    }

    @Override
    public Page<UIPluginConfiguration> retrievePluginConfigurations(final UIPluginDefinition pPluginDefinition,
            final Boolean pIsActive, final Boolean pIsLinkedToAllEntities, final Pageable pPageable)
            throws EntityException {
        if ((pPluginDefinition == null) || (pPluginDefinition.getId() == null)) {
            throw new EntityInvalidException("Plugin Identifier cannot be null");
        }

        // retrieve plugin
        if (!pluginRepository.existsById(pPluginDefinition.getId())) {
            throw new EntityNotFoundException(pPluginDefinition.getId(), UIPluginDefinition.class);
        }
        return repository.findByPluginDefinition(pPluginDefinition, pPageable);
    }

    @Override
    public UIPluginConfiguration retrievePluginconfiguration(final Long pPluginConfigurationId)
            throws EntityInvalidException {
        if (pPluginConfigurationId == null) {
            throw new EntityInvalidException("Plugin Identifier cannot be null");
        }

        return repository.findById(pPluginConfigurationId).orElse(null);
    }

    @Override
    public UIPluginConfiguration updatePluginconfiguration(final UIPluginConfiguration pPluginConfiguration)
            throws EntityException {
        if ((pPluginConfiguration == null) || (pPluginConfiguration.getId() == null)) {
            throw new EntityInvalidException("PluginConfiguration Identifier cannot be null");
        }

        if (!repository.existsById(pPluginConfiguration.getId())) {
            throw new EntityNotFoundException(pPluginConfiguration.getId(), UIPluginConfiguration.class);
        }

        // Check configuration json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(pPluginConfiguration.getConf(), Object.class);
        } catch (RuntimeException e) {
            throw new EntityInvalidException("Configuration is not a valid json format.", e);
        }

        UIPluginConfiguration updated = repository.save(pPluginConfiguration);
        publisher.publish(new UIPluginConfigurationEvent(updated));
        return updated;
    }

    @Override
    public UIPluginConfiguration createPluginconfiguration(final UIPluginConfiguration pPluginConfiguration)
            throws EntityException {
        if ((pPluginConfiguration == null) || (pPluginConfiguration.getId() != null)) {
            throw new EntityInvalidException("PluginConfiguration is invalid");
        }

        // Check configuration json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(pPluginConfiguration.getConf(), Object.class);
        } catch (final RuntimeException e) {
            throw new EntityInvalidException("Configuration is not a valid json format.", e);
        }

        UIPluginConfiguration created = repository.save(pPluginConfiguration);
        publisher.publish(new UIPluginConfigurationEvent(created));
        return created;
    }

    @Override
    public void deletePluginconfiguration(final UIPluginConfiguration pPluginConfiguration) throws EntityException {
        if ((pPluginConfiguration == null) || (pPluginConfiguration.getId() == null)) {
            throw new EntityInvalidException("PluginConfiguration Identifier cannot be null");
        }

        if (!repository.existsById(pPluginConfiguration.getId())) {
            throw new EntityNotFoundException(pPluginConfiguration.getId(), UIPluginConfiguration.class);
        }

        // Remove the config from the links
        try (Stream<LinkUIPluginsDatasets> links = linkedUiPluginRespository
                .findAllByServicesContaining(pPluginConfiguration)) {
            links.peek(link -> link.getServices().remove(pPluginConfiguration)).forEach(link -> {
                if (link.getServices().isEmpty()) {
                    linkedUiPluginRespository.delete(link);
                } else {
                    linkedUiPluginRespository.save(link);
                }
            });
        }

        repository.delete(pPluginConfiguration);
        publisher.publish(new UIPluginConfigurationEvent(pPluginConfiguration));

    }

    @Override
    public List<UIPluginConfiguration> retrieveActivePluginServices(final String pDatasetId,
            List<ServiceScope> pApplicationModes) {
        return retrieveActivePluginServices(Lists.newArrayList(pDatasetId), pApplicationModes);
    }

    @Override
    public List<UIPluginConfiguration> retrieveActivePluginServices(List<String> pDatasetIds,
            List<ServiceScope> pApplicationModes) {
        final Set<UIPluginConfiguration> activePluginsConfigurations = Sets.newHashSet();
        if ((pDatasetIds != null) && !pDatasetIds.isEmpty()) {
            final List<LinkUIPluginsDatasets> links = linkedUiPluginRespository.findByDatasetIdIn(pDatasetIds);
            activePluginsConfigurations.addAll(getCommonServicesFromLinks(links));
        }

        // Retrieve plugins linked to all dataset
        final List<UIPluginConfiguration> linkedToAllDataset = repository
                .findByActiveAndLinkedToAllEntitiesAndPluginDefinitionType(true, true, UIPluginTypesEnum.SERVICE);
        if (linkedToAllDataset != null) {
            linkedToAllDataset.forEach(pluginConf -> {
                if (!activePluginsConfigurations.contains(pluginConf)) {
                    activePluginsConfigurations.add(pluginConf);
                }
            });
        }
        try (Stream<UIPluginConfiguration> stream = activePluginsConfigurations.stream()) {
            return stream.filter(IS_APPLICABLE_ON.apply(pApplicationModes))
                    // Apply UIPlugin authorization
                    .filter(uiPluginConfiguration -> {
                        boolean usableByCurrentUser = false;
                        String roleName = uiPluginConfiguration.getPluginDefinition().getRoleName();
                        String userRoleName = authResolver.getRole();
                        if ((roleName == null) || userRoleName.equals(roleName)) {
                            usableByCurrentUser = true;
                        } else {
                            try {
                                usableByCurrentUser = rolesClient.shouldAccessToResourceRequiring(roleName).getBody();
                            } catch (EntityNotFoundException e) {
                                LOGGER.error("Failed to retrieve role authorisation. UIPluginDefinition role {} and UIPluginConf id {}",
                                             roleName, uiPluginConfiguration.getId(), e);
                            }
                        }
                        return usableByCurrentUser;
                    }).collect(Collectors.toList());
        }
    }

    /**
     * Retrieve unqi services from given {@link LinkUIPluginsDatasets}s
     * @param links
     * @return {@link UIPluginConfiguration}s
     */
    private Set<UIPluginConfiguration> getCommonServicesFromLinks(List<LinkUIPluginsDatasets> links) {
        final Set<UIPluginConfiguration> services = Sets.newHashSet();
        boolean first = true;
        // Create a set with common service between all links (dataset/plugins)
        for (LinkUIPluginsDatasets link : links) {
            if (first) {
                services.addAll(link.getServices());
                first = false;
            } else {
                services.retainAll(link.getServices());
            }
        }
        // Return only the active ones.
        return services.stream().filter(s -> s.getActive()).collect(Collectors.toSet());
    }

    @Override
    public Page<UIPluginConfiguration> retrievePluginConfigurations(PageRequest pageable) {
        return this.repository.findAll(pageable);
    }

}
