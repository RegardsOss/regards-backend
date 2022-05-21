package fr.cnes.regards.modules.accessrights.service.utils;

import com.google.gson.Gson;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.project.domain.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AccountUtilsServiceTest {

    private static final String TENANT = "project";

    private static final String EMAIL = "email@test.com";

    private static final String FIRST_NAME = "Firstname";

    private static final String LAST_NAME = "Lirstname";

    private static final List<MetaData> META_DATA = new ArrayList<>();

    private static final String PASSWORD = "password";

    private static final Role ROLE = new Role("role name", null);

    private static final String ORIGIN_URL = "originUrl";

    private static final String REQUEST_LINK = "requestLink";

    private static final String ORIGIN = "origin";

    private static final Set<String> ACCESS_GROUPS = Collections.singleton("group");

    @Mock
    private IAccountsClient accountsClient;

    @Mock
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Mock
    private Gson gson;

    @InjectMocks
    private AccountUtilsService accountUtilsService;

    private AccessRequestDto accessRequestDto;

    private Account expectedAccount;

    @BeforeEach
    void setUp() {
        Project project = new Project();
        project.setName(TENANT);
        accessRequestDto = new AccessRequestDto(EMAIL,
                                                FIRST_NAME,
                                                LAST_NAME,
                                                ROLE.getName(),
                                                META_DATA,
                                                PASSWORD,
                                                ORIGIN_URL,
                                                REQUEST_LINK,
                                                ORIGIN,
                                                ACCESS_GROUPS,
                                                0L);
        expectedAccount = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);
        expectedAccount.setProjects(Collections.singleton(project));
    }

    @Test
    void retrieveAccount() throws EntityInvalidException {
        // Given
        Mockito.when(accountsClient.retrieveAccounByEmail(EMAIL))
               .thenReturn(ResponseEntity.ok(EntityModel.of(expectedAccount)));
        // When
        Account retrievedAccount = accountUtilsService.retrieveAccount(EMAIL);
        // Then
        assertEquals(expectedAccount, retrievedAccount);
    }

    @Test
    void retrieveAccountWhenNotFound() throws EntityInvalidException {
        // Given
        Mockito.when(accountsClient.retrieveAccounByEmail(EMAIL)).thenReturn(ResponseEntity.notFound().build());
        // When
        Account retrievedAccount = accountUtilsService.retrieveAccount(EMAIL);
        // Then
        assertNull(retrievedAccount);
    }

    @Test
    void createAccount() throws EntityException {
        // Given
        mockCreation();
        // When
        Account createdAccount = accountUtilsService.createAccount(accessRequestDto, false, null);
        // Then
        assertEquals(expectedAccount, createdAccount);
    }

    @Test
    void createAccountWithInvalidParameters() {

        AccessRequestDto invalidRequest = new AccessRequestDto();
        Class<EntityNotFoundException> expectedType = EntityNotFoundException.class;

        // Given
        BeanUtils.copyProperties(accessRequestDto, invalidRequest);
        invalidRequest.setEmail(null);
        // When - Then
        assertThrows(expectedType, () -> accountUtilsService.createAccount(invalidRequest, false, null));

        // Given
        BeanUtils.copyProperties(accessRequestDto, invalidRequest);
        invalidRequest.setFirstName(null);
        // When - Then
        assertThrows(expectedType, () -> accountUtilsService.createAccount(invalidRequest, false, null));

        // Given
        BeanUtils.copyProperties(accessRequestDto, invalidRequest);
        invalidRequest.setLastName(null);
        // When - Then
        assertThrows(expectedType, () -> accountUtilsService.createAccount(invalidRequest, false, null));

        // Given
        BeanUtils.copyProperties(accessRequestDto, invalidRequest);
        invalidRequest.setPassword(null);
        // When - Then
        assertThrows(expectedType, () -> accountUtilsService.createAccount(invalidRequest, false, null));

        // Given
        BeanUtils.copyProperties(accessRequestDto, invalidRequest);
        invalidRequest.setOrigin(Account.REGARDS_ORIGIN);
        // When - Then
        assertThrows(expectedType, () -> accountUtilsService.createAccount(invalidRequest, true, null));

        // Given
        BeanUtils.copyProperties(accessRequestDto, invalidRequest);
        invalidRequest.setOrigin(null);
        // When - Then
        assertThrows(expectedType, () -> accountUtilsService.createAccount(invalidRequest, true, null));
    }

    @Test
    void createAccountExternal() throws EntityException {
        // Given
        mockCreation();
        accessRequestDto.setOrigin(ORIGIN);
        expectedAccount.setOrigin(ORIGIN);
        // When
        Account createdAccount = accountUtilsService.createAccount(accessRequestDto, true, null);
        // Then
        assertEquals(expectedAccount, createdAccount);
    }

    @Test
    void createAccountWithStatus() throws EntityException {
        // Given
        mockCreation();
        expectedAccount.setStatus(AccountStatus.ACTIVE);
        // When
        Account createdAccount = accountUtilsService.createAccount(accessRequestDto, false, AccountStatus.ACTIVE);
        // Then
        assertEquals(expectedAccount, createdAccount);
    }

    private void mockCreation() {
        Mockito.when(accountsClient.createAccount(any(AccountNPassword.class))).thenAnswer(invocation -> {
            AccountNPassword accountNPassword = invocation.getArgument(0);
            Account account = accountNPassword.getAccount();
            Project project = new Project();
            project.setName(accountNPassword.getProject());
            account.setProjects(Collections.singleton(project));
            account.setPassword(accountNPassword.getPassword());
            return ResponseEntity.ok(EntityModel.of(account));
        });
    }

}