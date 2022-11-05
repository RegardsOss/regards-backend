package fr.cnes.regards.modules.access.services.rest.user.mock;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserSearchParameters;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.SearchProjectUserParameters;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Primary
@Component
public class ProjectUsersClientMock implements IProjectUsersClient, IResourceController<ProjectUser> {

    public static final Random r = new Random();

    public static final Long ROLE_STUB_ID = (long) r.nextInt(10_000);

    public static final String ROLE_STUB_NAME = "role";

    private static final Role ROLE_STUB;

    public static final Long PROJECT_USER_STUB_ID = (long) r.nextInt(10_000);

    public static final String PROJECT_USER_STUB_EMAIL = "foo@bar.com";

    public static final ProjectUser PROJECT_USER_STUB;

    public static final String FIRST_NAME = "firstName";

    public static final String LAST_NAME = "lastName";

    static {
        ROLE_STUB = new Role(ROLE_STUB_NAME);
        ROLE_STUB.setId(ROLE_STUB_ID);
        PROJECT_USER_STUB = new ProjectUser(PROJECT_USER_STUB_EMAIL,
                                            ROLE_STUB,
                                            Collections.emptyList(),
                                            Collections.emptySet());
        PROJECT_USER_STUB.setMaxQuota(StorageRestClientMock.USER_QUOTA_LIMITS_STUB_MAX_QUOTA);
        PROJECT_USER_STUB.setCurrentQuota(StorageRestClientMock.CURRENT_USER_QUOTA_STUB);
        PROJECT_USER_STUB.setFirstName(FIRST_NAME);
        PROJECT_USER_STUB.setLastName(LAST_NAME);
        PROJECT_USER_STUB.setAccessGroups(Collections.singleton("group"));
        PROJECT_USER_STUB.setId(PROJECT_USER_STUB_ID);
    }

    public static final int TOTAL_ELEMENTS_STUB = 1;

    public static final int TOTAL_PAGES_STUB = 1;

    public static final int PAGE_NUMBER_STUB = 0;

    public static final int PAGE_SIZE_STUB = 1;

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private PagedResourcesAssembler<ProjectUser> assembler;

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveProjectUserList(ProjectUserSearchParameters parameters,
                                                                                        Pageable pageable) {
        return singleProjectUserPagedResponse();
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveProjectUserList(SearchProjectUserParameters filters,
                                                                                        Pageable pageable) {
        return singleProjectUserPagedResponse();

    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveAccessRequestList(Pageable pageable) {
        return singleProjectUserPagedResponse();
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> createUser(@Valid AccessRequestDto pDto) {
        return singleProjectUserResponse(makeFoobarProjectUser());
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUser(Long pUserId) {
        return singleProjectUserResponse(makeFoobarProjectUser());
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUserByEmail(String pUserEmail) {
        return singleProjectUserResponse(makeFoobarProjectUser());
    }

    @Override
    public ResponseEntity<Boolean> isAdmin(String userEmail) {
        return new ResponseEntity<>(false, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> updateProjectUser(Long pUserId, ProjectUser pUpdatedProjectUser) {
        return singleProjectUserResponse(pUpdatedProjectUser);
    }

    @Override
    public ResponseEntity<Void> removeProjectUser(Long pUserId) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> retrieveCurrentProjectUser() {
        return singleProjectUserResponse(makeFoobarProjectUser());
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> updateCurrentProjectUser(ProjectUser updatedProjectUser) {
        return singleProjectUserResponse(updatedProjectUser);
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUserList(Long pRoleId,
                                                                                            Pageable pageable) {
        return singleProjectUserPagedResponse();
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUsersList(String pRole,
                                                                                             Pageable pageable) {
        return singleProjectUserPagedResponse();
    }

    @Override
    public ResponseEntity<Void> linkAccessGroups(String email, List<String> groups) {
        return null;
    }

    @Override
    public ResponseEntity<Void> updateOrigin(String email, String origin) {
        return null;
    }

    @Override
    public ResponseEntity<Void> sendVerificationEmail(String email) {
        return null;
    }

    @Override
    public EntityModel<ProjectUser> toResource(ProjectUser element, Object... extras) {
        return resourceService.toResource(element);
    }

    protected ResponseEntity<EntityModel<ProjectUser>> singleProjectUserResponse(ProjectUser projectUser) {
        return new ResponseEntity<>(toResource(projectUser), HttpStatus.OK);
    }

    protected ResponseEntity<PagedModel<EntityModel<ProjectUser>>> singleProjectUserPagedResponse() {
        return new ResponseEntity<>(toPagedResources(new PageImpl<>(Arrays.asList(makeFoobarProjectUser()),
                                                                    new MockedPageable(PAGE_NUMBER_STUB,
                                                                                       PAGE_SIZE_STUB),
                                                                    TOTAL_PAGES_STUB), assembler),
                                    HttpHeaders.EMPTY,
                                    HttpStatus.OK);
    }

    private ProjectUser makeFoobarProjectUser() {
        return PROJECT_USER_STUB;
    }

    private static class MockedPageable extends AbstractPageRequest {

        public MockedPageable(int page, int size) {
            super(page, size);
        }

        @Override
        public Sort getSort() {
            return Sort.by(Sort.Direction.ASC, "id");
        }

        @Override
        public MockedPageable next() {
            return new MockedPageable(getPageNumber() + 1, getPageSize());
        }

        @Override
        public MockedPageable previous() {
            return getPageNumber() == 0 ?
                new MockedPageable(getPageNumber(), getPageSize()) :
                new MockedPageable(getPageNumber() - 1, getPageSize());
        }

        @Override
        public MockedPageable first() {
            return new MockedPageable(0, getPageSize());
        }

        @Override
        public MockedPageable withPage(int pageNumber) {
            return new MockedPageable(pageNumber, getPageSize());
        }
    }
}
