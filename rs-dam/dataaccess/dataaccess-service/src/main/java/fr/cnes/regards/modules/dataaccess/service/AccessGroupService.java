/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.dataaccess.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.rest.utils.HttpUtils;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserEvent;
import fr.cnes.regards.modules.dataaccess.dao.IAccessGroupRepository;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.User;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.event.AccessGroupEvent;

/**
 *
 * Service handling {@link AccessGroup}
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Service
@MultitenantTransactional
@EnableFeignClients(clients = IProjectUsersClient.class)
public class AccessGroupService implements ApplicationListener<ApplicationReadyEvent> {

    public static final String ACCESS_GROUP_ALREADY_EXIST_ERROR_MESSAGE = "Access Group of name %s already exists! Name of an access group has to be unique.";

    private final IAccessGroupRepository accessGroupDao;

    private final IProjectUsersClient projectUserClient;

    /**
     * Publish for model changes
     */
    private final IPublisher publisher;

    private final ISubscriber subscriber;

    @Value("${spring.application.name}")
    private String microserviceName;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public AccessGroupService(final IAccessGroupRepository pAccessGroupDao,
            final IProjectUsersClient pProjectUserClient, final IPublisher pPublisher, final ISubscriber subscriber,
            IRuntimeTenantResolver runtimeTenantResolver) {
        super();
        accessGroupDao = pAccessGroupDao;
        projectUserClient = pProjectUserClient;
        publisher = pPublisher;
        this.subscriber = subscriber;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    /**
     * setter only used for unit testing purpose without spring context
     *
     * @param pMicroserviceName
     */
    public void setMicroserviceName(final String pMicroserviceName) {
        microserviceName = pMicroserviceName;
    }

    public Page<AccessGroup> retrieveAccessGroups(final Pageable pPageable) {
        return accessGroupDao.findAll(pPageable);
    }

    public AccessGroup createAccessGroup(final AccessGroup pToBeCreated) throws EntityAlreadyExistsException {
        final AccessGroup alreadyExisting = accessGroupDao.findOneByName(pToBeCreated.getName());
        if (alreadyExisting != null) {
            throw new EntityAlreadyExistsException(
                    String.format(ACCESS_GROUP_ALREADY_EXIST_ERROR_MESSAGE, pToBeCreated.getName()));
        }
        final AccessGroup created = accessGroupDao.save(pToBeCreated);
        // Publish attribute creation
        publisher.publish(new AccessGroupEvent(created));
        return created;
    }

    /**
     * @param pAccessGroupName
     * @return
     * @throws EntityNotFoundException
     */
    public AccessGroup retrieveAccessGroup(final String pAccessGroupName) throws EntityNotFoundException {
        final AccessGroup ag = accessGroupDao.findOneByName(pAccessGroupName);
        if (ag == null) {
            throw new EntityNotFoundException(pAccessGroupName, AccessGroup.class);
        }
        return ag;
    }

    /**
     * @param pAccessGroupName
     */
    public void deleteAccessGroup(final String pAccessGroupName) {
        final AccessGroup toDelete = accessGroupDao.findOneByName(pAccessGroupName);
        if (toDelete != null) {
            accessGroupDao.delete(toDelete.getId());
            // Publish attribute deletion
            publisher.publish(new AccessGroupEvent(toDelete));
        }
    }

    /**
     * @param pUserEmail
     * @param pAccessGroupName
     * @return
     * @throws EntityNotFoundException
     */
    public AccessGroup associateUserToAccessGroup(final String pUserEmail, final String pAccessGroupName)
            throws EntityNotFoundException {
        final User user = getUser(pUserEmail);
        if (user == null) {
            throw new EntityNotFoundException(pUserEmail, ProjectUser.class);
        }
        final AccessGroup ag = accessGroupDao.findOneByName(pAccessGroupName);
        ag.addUser(user);
        final AccessGroup updated = accessGroupDao.save(ag);
        // Publish
        publisher.publish(new AccessGroupEvent(updated));
        return updated;
    }

    private User getUser(final String pUserEmail) { // NOSONAR: method is used but by lambda so it is not recognized
        try {
            FeignSecurityManager.asSystem();
            final ResponseEntity<Resource<ProjectUser>> response = projectUserClient
                    .retrieveProjectUserByEmail(pUserEmail);
            final HttpStatus responseStatus = response.getStatusCode();
            if (!HttpUtils.isSuccess(responseStatus)) {
                // if it gets here it's mainly because of 404 so it means entity not found
                return null;
            }
            return new User(pUserEmail);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    /**
     * @param pUserEmail
     * @param pAccessGroupName
     * @throws EntityNotFoundException
     */
    public AccessGroup dissociateUserFromAccessGroup(final String pUserEmail, final String pAccessGroupName)
            throws EntityNotFoundException {
        final User user = getUser(pUserEmail);
        if (user == null) {
            throw new EntityNotFoundException(pUserEmail, ProjectUser.class);
        }
        final AccessGroup ag = accessGroupDao.findOneByName(pAccessGroupName);
        ag.removeUser(user);
        final AccessGroup updated = accessGroupDao.save(ag);
        // Publish
        publisher.publish(new AccessGroupEvent(updated));
        return updated;
    }

    /**
     * @param pUserEmail
     * @param pPageable
     * @return
     */
    public Page<AccessGroup> retrieveAccessGroupsOfUser(final String pUserEmail, final Pageable pPageable) {
        return accessGroupDao.findAllByUsersOrIsPublic(new User(pUserEmail), Boolean.TRUE, pPageable);
    }

    /**
     * @param pUserEmail
     * @param pNewAcessGroups
     * @return
     * @throws EntityNotFoundException
     */
    public void setAccessGroupsOfUser(final String pUserEmail, final List<AccessGroup> pNewAcessGroups)
            throws EntityNotFoundException {
        final Set<AccessGroup> actualAccessGroups = accessGroupDao.findAllByUsers(new User(pUserEmail));
        for (final AccessGroup actualGroup : actualAccessGroups) {
            dissociateUserFromAccessGroup(pUserEmail, actualGroup.getName());
        }
        for (final AccessGroup newGroup : pNewAcessGroups) {
            associateUserToAccessGroup(pUserEmail, newGroup.getName());
        }
    }

    /**
     * @param pId
     * @return
     */
    public boolean existGroup(final Long pId) {
        return accessGroupDao.exists(pId);
    }

    /**
     * @param pUser
     * @return
     */
    public boolean existUser(final User pUser) {
        final User user = getUser(pUser.getEmail());
        return user != null;
    }

    /**
     * only update the privacy of the group
     *
     * @param pAccessGroupName
     * @param pAccessGroup
     * @return updated accessGroup
     * @throws ModuleException
     */
    public AccessGroup update(final String pAccessGroupName, final AccessGroup pAccessGroup) throws ModuleException {
        final AccessGroup group = accessGroupDao.findOneByName(pAccessGroupName);
        if (group == null) {
            throw new EntityNotFoundException(pAccessGroupName);
        }
        if (!group.getId().equals(pAccessGroup.getId())) {
            throw new EntityInconsistentIdentifierException(group.getId(), pAccessGroup.getId(), AccessGroup.class);
        }
        group.setPublic(pAccessGroup.isPublic());
        return accessGroupDao.save(group);
    }

    private void removeUser(String email) {
        User toRemove = new User(email);
        Set<AccessGroup> groupsToProcess = accessGroupDao.findAllByUsers(toRemove);
        for (AccessGroup toProcess : groupsToProcess) {
            toProcess.removeUser(toRemove);
            accessGroupDao.save(toProcess);
        }
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    // this method is called out of context on microservice initialization
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(ProjectUserEvent.class, new ProjectUserEventHandler());
    }

    private class ProjectUserEventHandler implements IHandler<ProjectUserEvent> {

        @Override
        public void handle(TenantWrapper<ProjectUserEvent> pWrapper) {
            String tenant = pWrapper.getTenant();
            runtimeTenantResolver.forceTenant(tenant);
            ProjectUserEvent event = pWrapper.getContent();
            switch (event.getAction()) {
                case DELETION:
                    removeUser(event.getEmail());
                    break;
                default:
                    //nothing to do
                    break;
            }

        }

    }
}