/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 *
 * Class ProjectUsersClientStub
 *
 * Stub to simulate the responses from the administration services for the ProjectUsers entities.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Component
@Primary
public class ProjectUsersClientStub implements IProjectUsersClient {

    /**
     * Locale list of users
     */
    private static List<ProjectUser> users = new ArrayList<>();

    /**
     * Id generator count
     */
    private static long idCount = 0;

    @Override
    public ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveProjectUserList(final int pPage,
            final int pSize) {
        final PageMetadata metadata = new PageMetadata(pSize, pPage, users.size());
        final PagedResources<Resource<ProjectUser>> resource = new PagedResources<>(HateoasUtils.wrapList(users),
                metadata, new ArrayList<>());
        return new ResponseEntity<PagedResources<Resource<ProjectUser>>>(resource, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<ProjectUser>> retrieveProjectUserByEmail(final String pUserEmail) {
        ProjectUser result = null;
        for (final ProjectUser user : users) {
            if (user.getEmail().equals(pUserEmail)) {
                result = user;
                break;
            }
        }
        if (result == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Resource<ProjectUser>>(HateoasUtils.wrap(result), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<ProjectUser>> updateProjectUser(final Long pUserId,
            final ProjectUser pUpdatedProjectUser) {
        return null;
    }

    @Override
    public ResponseEntity<Void> removeProjectUser(final Long pUserId) {
        return null;
    }

    @Override
    public ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveAccessRequestList(final int pPage,
            final int pSize) {
        return null;
    }

    @Override
    public ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveRoleProjectUserList(final Long pRoleId,
            final int pPage, final int pSize) {
        return null;
    }

    @Override
    public ResponseEntity<Resource<ProjectUser>> retrieveProjectUser(final Long pUserId) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.cnes.regards.modules.accessrights.client.IProjectUsersClient#isAdmin(java.lang.String)
     */
    @Override
    public ResponseEntity<Boolean> isAdmin(String pUserEmail) {
        return null;
    }

}
