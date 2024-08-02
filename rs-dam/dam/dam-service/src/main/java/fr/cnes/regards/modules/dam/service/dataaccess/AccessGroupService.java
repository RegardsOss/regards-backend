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
package fr.cnes.regards.modules.dam.service.dataaccess;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.*;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.dam.dao.dataaccess.IAccessGroupRepository;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.event.AccessGroupAction;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.event.AccessGroupEvent;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.event.PublicAccessGroupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service handling {@link AccessGroup}
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Léo Mieulet
 */
@Service
@MultitenantTransactional
public class AccessGroupService implements IAccessGroupService, InitializingBean {

    public static final String ACCESS_GROUP_ALREADY_EXIST_ERROR_MESSAGE = "Access Group of name %s already exists! Name of an access group has to be unique.";

    public static final String ACCESS_GROUP_PUBLIC_DOCUMENTS = "Public";

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessGroupService.class);

    private final IAccessGroupRepository accessGroupRepository;

    private final IPublisher publisher;

    private final ITenantResolver tenantResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public AccessGroupService(IAccessGroupRepository accessGroupRepository,
                              IPublisher publisher,
                              ITenantResolver tenantResolver,
                              IRuntimeTenantResolver runtimeTenantResolver) {
        this.accessGroupRepository = accessGroupRepository;
        this.publisher = publisher;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenantResolver = tenantResolver;
    }

    @Override
    public void afterPropertiesSet() {
        // Ensure the existence of the default Group Access for Documents.
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                initDefaultAccessGroup();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Override
    public void initDefaultAccessGroup() {
        // Build the AccessGroup we need in the current project
        AccessGroup publicDocumentAccessGroup = new AccessGroup();
        publicDocumentAccessGroup.setName(AccessGroupService.ACCESS_GROUP_PUBLIC_DOCUMENTS);
        publicDocumentAccessGroup.setPublic(true);
        publicDocumentAccessGroup.setInternal(true);
        try {
            createAccessGroup(publicDocumentAccessGroup);
        } catch (EntityAlreadyExistsException e) {
            // the entity already exists, no problem
            LOGGER.trace("Entity already exists, no problem", e);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to register the public AccessGroup used by documents", e);
            // Do not prevent microservice to boot
        }
    }

    @Override
    public Page<AccessGroup> retrieveAccessGroups(boolean isPublic, Pageable pageable) {
        if (isPublic) {
            return accessGroupRepository.findByIsPublic(isPublic, pageable);
        }

        return accessGroupRepository.findAll(pageable);
    }

    @Override
    public AccessGroup createAccessGroup(AccessGroup toBeCreated) throws EntityAlreadyExistsException {
        if (accessGroupRepository.findOneByName(toBeCreated.getName()) != null) {
            throw new EntityAlreadyExistsException(String.format(ACCESS_GROUP_ALREADY_EXIST_ERROR_MESSAGE,
                                                                 toBeCreated.getName()));
        }
        // Save the new access group in bd
        AccessGroup created = accessGroupRepository.save(toBeCreated);
        if (created.isPublic()) {
            publisher.publish(new PublicAccessGroupEvent(created, AccessGroupAction.CREATE));
        }
        publisher.publish(new AccessGroupEvent(created, AccessGroupAction.CREATE));
        return created;
    }

    @Override
    public AccessGroup retrieveAccessGroup(String accessGroupName) throws EntityNotFoundException {
        AccessGroup accessGroup = accessGroupRepository.findOneByName(accessGroupName);
        if (accessGroup == null) {
            throw new EntityNotFoundException(accessGroupName, AccessGroup.class);
        }
        return accessGroup;
    }

    @Override
    public Optional<AccessGroup> getByName(String name) {
        return accessGroupRepository.findByName(name);
    }

    @Override
    public void deleteAccessGroup(String accessGroupName)
        throws EntityOperationForbiddenException, EntityNotFoundException {
        AccessGroup toDelete = retrieveAccessGroup(accessGroupName);
        // Prevent users to delete the public AccessGroup used by Documents
        if (toDelete.isInternal()) {
            throw new EntityOperationForbiddenException(toDelete.getName(),
                                                        AccessGroup.class,
                                                        "Cannot remove the public access group used by Documents");
        }
        accessGroupRepository.deleteById(toDelete.getId());
        if (toDelete.isPublic()) {
            publisher.publish(new PublicAccessGroupEvent(toDelete, AccessGroupAction.DELETE));
        }
        publisher.publish(new AccessGroupEvent(toDelete, AccessGroupAction.DELETE));
    }

    @Override
    public boolean existGroup(Long id) {
        return accessGroupRepository.existsById(id);
    }

    @Override
    public AccessGroup update(String accessGroupName, AccessGroup updatedGroup) throws ModuleException {

        AccessGroup oldGroup = retrieveAccessGroup(accessGroupName);
        if (!oldGroup.getId().equals(updatedGroup.getId())) {
            throw new EntityInconsistentIdentifierException(oldGroup.getId(), updatedGroup.getId(), AccessGroup.class);
        }

        // Update visibility
        boolean wasPublic = oldGroup.isPublic();
        boolean isPublic = updatedGroup.isPublic();
        if (wasPublic && !isPublic) {
            // this case does not concern real users because group is not removed once it becomes private
            // However, false users (public) should have their access group cache updated
            publisher.publish(new PublicAccessGroupEvent(oldGroup, AccessGroupAction.DELETE));
        }
        if (isPublic && !wasPublic) {
            // Publish proper event in order for rs-admin to link group to users
            publisher.publish(new PublicAccessGroupEvent(oldGroup, AccessGroupAction.CREATE));
        }

        oldGroup.setPublic(updatedGroup.isPublic());

        return accessGroupRepository.save(oldGroup);
    }

}
