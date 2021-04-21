package fr.cnes.regards.modules.access.services.rest.user.mock;

import fr.cnes.regards.modules.accessrights.client.IRegistrationClient;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

@Primary
@Component
public class RegistrationClientMock implements IRegistrationClient {

    public static final String ACCESS_REQUEST_STUB_EMAIL = "foo@bar.com";
    public static final String ACCESS_REQUEST_STUB_FIRSTNAME = "foo";
    public static final String ACCESS_REQUEST_STUB_LASTNAME = "bar";
    public static final String ACCESS_REQUEST_STUB_ROLE = "role";
    public static final List<MetaData> ACCESS_REQUEST_STUB_META = Collections.emptyList();
    public static final String ACCESS_REQUEST_STUB_PASSWORD = "password";
    public static final String ACCESS_REQUEST_STUB_ORIGIN_URL = "originURL";
    public static final String ACCESS_REQUEST_STUB_REQUEST_LINK = "requestLink";
    public static final AccessRequestDto ACCESS_REQUEST_STUB = new AccessRequestDto(
        ACCESS_REQUEST_STUB_EMAIL,
        ACCESS_REQUEST_STUB_FIRSTNAME,
        ACCESS_REQUEST_STUB_LASTNAME,
        ACCESS_REQUEST_STUB_ROLE,
        ACCESS_REQUEST_STUB_META,
        ACCESS_REQUEST_STUB_PASSWORD,
        ACCESS_REQUEST_STUB_ORIGIN_URL,
        ACCESS_REQUEST_STUB_REQUEST_LINK
    );

    @Override
    public ResponseEntity<EntityModel<AccessRequestDto>> requestAccess(@Valid AccessRequestDto pAccessRequest) {
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<EntityModel<AccessRequestDto>> requestExternalAccess(@Valid AccessRequestDto pAccessRequest) {
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> verifyEmail(String token) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> acceptAccessRequest(Long pAccessId) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> denyAccessRequest(Long pAccessId) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> activeAccess(Long accessId) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> inactiveAccess(Long accessId) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> removeAccessRequest(Long pAccessId) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<EntityModel<ProjectUser>>> retrieveAccessRequestList(int pPage, int pSize) {
        return null;
    }
}
