package fr.cnes.regards.modules.access.services.client;

import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.validation.Valid;

/**
 * Make Spring DI happy.
 */
@Primary
@Component
public class ProjectUsersClientMock implements IProjectUsersClient {

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveProjectUserList(String pStatus, String pEmailStart, int pPage, int pSize) {
        return null;
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveAccessRequestList(int pPage, int pSize) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> createUser(@Valid AccessRequestDto pDto) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUser(Long pUserId) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUserByEmail(String pUserEmail) {
        return null;
    }

    @Override
    public ResponseEntity<Boolean> isAdmin(String userEmail) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> updateProjectUser(Long pUserId, ProjectUser pUpdatedProjectUser) {
        return null;
    }

    @Override
    public ResponseEntity<Void> removeProjectUser(Long pUserId) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> retrieveCurrentProjectUser() {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> updateCurrentProjectUser(ProjectUser updatedProjectUser) {
        return null;
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUserList(Long pRoleId, int pPage, int pSize) {
        return null;
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUsersList(String pRole, int pPage, int pSize) {
        return null;
    }
}
