/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.modules.test.users.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.microservices.modules.test.RegardsIntegrationTest;
import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.HttpVerb;
import fr.cnes.regards.modules.accessRights.domain.MetaData;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.service.test.UserServiceStub;

/**
 * @author svissier
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UsersControllerIT extends RegardsIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersControllerIT.class);

    private TestRestTemplate restTemplate;

    private String apiUsers;

    private String apiUserId;

    private String apiUserPermissions;

    private String apiUserMetaData;

    @Autowired
    private UserServiceStub serviceStub;

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword;

    @Before
    public void init() {
        if (restTemplate == null) {
            restTemplate = buildOauth2RestTemplate("acme", "acmesecret", "admin", "admin", "");
        }
        this.apiUsers = getApiEndpoint().concat("/users");
        this.apiUserId = this.apiUsers + "/{user_id}";
        this.apiUserPermissions = this.apiUserId + "/permissions";
        this.apiUserMetaData = this.apiUserId + "/metadata";
    }

    @Test
    public void aGetAllUsers() {
        ParameterizedTypeReference<List<ProjectUser>> typeRef = new ParameterizedTypeReference<List<ProjectUser>>() {
        };
        ResponseEntity<List<ProjectUser>> response = restTemplate.exchange(this.apiUsers, HttpMethod.GET, null,
                                                                           typeRef);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void cGetUser() {

        Long userId = this.serviceStub.retrieveUserList().get(0).getId();

        assertFalse(!this.serviceStub.existUser(userId));
        ParameterizedTypeReference<ProjectUser> typeRef = new ParameterizedTypeReference<ProjectUser>() {
        };
        ResponseEntity<ProjectUser> response = restTemplate.exchange(this.apiUserId, HttpMethod.GET, null, typeRef,
                                                                     userId);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ResponseEntity<Object> responseNotFound = restTemplate.exchange(this.apiUserId, HttpMethod.GET, null,
                                                                        Object.class, Integer.MAX_VALUE);
        assertEquals(HttpStatus.NOT_FOUND, responseNotFound.getStatusCode());

    }

    @Test
    public void cGetUserMetaData() {
        Long userId = this.serviceStub.retrieveUserList().get(0).getId();

        assertFalse(!this.serviceStub.existUser(userId));
        ParameterizedTypeReference<List<MetaData>> typeRef = new ParameterizedTypeReference<List<MetaData>>() {
        };
        ResponseEntity<List<MetaData>> response = restTemplate.exchange(this.apiUserMetaData, HttpMethod.GET, null,
                                                                        typeRef, userId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void cGetUserPermissions() {

        Long userId = this.serviceStub.retrieveUserList().get(0).getId();

        assertFalse(!this.serviceStub.existUser(userId));
        ParameterizedTypeReference<Couple<List<ResourcesAccess>, Role>> typeRef = new ParameterizedTypeReference<Couple<List<ResourcesAccess>, Role>>() {
        };
        ResponseEntity<Couple<List<ResourcesAccess>, Role>> response = restTemplate
                .exchange(this.apiUserPermissions, HttpMethod.GET, null, typeRef, userId);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ResponseEntity<Object> responseNotFound = restTemplate.exchange(this.apiUserPermissions, HttpMethod.GET, null,
                                                                        Object.class, Integer.MAX_VALUE);
        assertEquals(HttpStatus.NOT_FOUND, responseNotFound.getStatusCode());

    }

    @Test
    public void dUpdateUserMetaData() {
        Long userId = this.serviceStub.retrieveUserList().get(0).getId();

        List<MetaData> newPermissionList = new ArrayList<>();
        newPermissionList.add(new MetaData());
        newPermissionList.add(new MetaData());

        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        HttpEntity<List<MetaData>> request = new HttpEntity<>(newPermissionList);
        ResponseEntity<Void> response = restTemplate.exchange(this.apiUserMetaData, HttpMethod.PUT, request, typeRef,
                                                              userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void dUpdateUserPermissions() {
        Long userId = this.serviceStub.retrieveUserList().get(0).getId();

        List<ResourcesAccess> newPermissionList = new ArrayList<>();
        newPermissionList.add(new ResourcesAccess(463L, "new", "new", "new", HttpVerb.PUT));
        newPermissionList.add(new ResourcesAccess(350L, "neww", "neww", "neww", HttpVerb.DELETE));

        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        HttpEntity<List<ResourcesAccess>> request = new HttpEntity<>(newPermissionList);
        ResponseEntity<Void> response = restTemplate.exchange(this.apiUserPermissions, HttpMethod.PUT, request, typeRef,
                                                              userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void dDeleteUserMetaData() {
        Long userId = this.serviceStub.retrieveUserList().get(0).getId();
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };

        ResponseEntity<Void> response = restTemplate.exchange(this.apiUserMetaData, HttpMethod.DELETE, null, typeRef,
                                                              userId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void dDeleteUserPermissions() {
        Long userId = this.serviceStub.retrieveUserList().get(0).getId();
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };

        ResponseEntity<Void> response = restTemplate.exchange(this.apiUserPermissions, HttpMethod.DELETE, null, typeRef,
                                                              userId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void dUpdateUser() {
        Long userId = this.serviceStub.retrieveUserList().get(0).getId();
        ProjectUser updated = this.serviceStub.retrieveUser(userId);
        assertThat(updated.getId() == userId);
        updated.setLastConnection(LocalDateTime.now());
        ;
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        HttpEntity<ProjectUser> request = new HttpEntity<>(updated);
        ResponseEntity<Void> response = restTemplate.exchange(this.apiUserId, HttpMethod.PUT, request, typeRef, userId);
        // if that's the same functional ID and the parameter is valid:
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // if that's not the same functional ID and the parameter is valid:
        ProjectUser notSameID = new ProjectUser();
        HttpEntity<ProjectUser> requestOperationNotAllowed = new HttpEntity<>(notSameID);
        ResponseEntity<Void> responseOperationNotAllowed = restTemplate
                .exchange(this.apiUserId, HttpMethod.PUT, requestOperationNotAllowed, typeRef, userId);
        assertEquals(HttpStatus.BAD_REQUEST, responseOperationNotAllowed.getStatusCode());
    }

    @Test
    public void eDeleteUser() {
        Long userId = this.serviceStub.retrieveUserList().get(0).getId();
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        ResponseEntity<Void> response = restTemplate.exchange(this.apiUserId, HttpMethod.DELETE, null, typeRef, userId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
