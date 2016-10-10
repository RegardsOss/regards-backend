/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service.stubs;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessRights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessRights.domain.AccountStatus;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;
import fr.cnes.regards.modules.accessRights.domain.instance.Account;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.service.IAccessRequestService;
import fr.cnes.regards.modules.accessRights.service.IAccountService;
import fr.cnes.regards.modules.accessRights.service.IRoleService;
import fr.cnes.regards.modules.accessRights.service.RoleService;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

@Service
@Profile("test")
@Primary
public class AccessRequestServiceStub implements IAccessRequestService {

    private static List<ProjectUser> projectUsers = new ArrayList<>();

    private final IAccountService accountService;

    private final IRoleService roleService;

    @Value("${regards.project.account_acceptance}")
    private String accessSetting;

    public AccessRequestServiceStub(final IAccountService pAccountService, final IRoleRepository pRoleRepository) {
        accountService = pAccountService;
        roleService = new RoleService(pRoleRepository);

        // Account account = new Account("login0@test.com", "firstName", "lastName", "password");
        String login = "login0@test.com";
        ProjectUser projectUser = new ProjectUser();
        projectUser.setEmail(login);
        projectUsers.add(projectUser);

        // account = new Account("toto@toto.toto", "firstName", "lastName", "password");
        login = "login1@test.com";
        projectUser = new ProjectUser();
        projectUser.setEmail(login);
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);
        projectUsers.add(projectUser);

        // account = new Account("titi@titi.titi", "firstName", "lastName", "password");
        login = "login2@test.com";
        projectUser = new ProjectUser();
        projectUser.setEmail(login);
        projectUser.setStatus(UserStatus.ACCESS_DENIED);
        projectUsers.add(projectUser);

    }

    @Override
    public List<ProjectUser> retrieveAccessRequestList() {
        return projectUsers.stream().filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCES))
                .collect(Collectors.toList());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IAccessRequestService#requestAccess(fr.cnes.regards.modules.
     * accessRights.domain.Account)
     */
    @Override
    public AccessRequestDTO requestAccess(final AccessRequestDTO pDto) throws AlreadyExistingException {
        if (existAccessRequest(pDto.getEmail())) {
            throw new AlreadyExistingException(
                    pDto.getEmail() + " already has made an access request for this project");
        }

        // If no role provided, set to default role
        if (pDto.getRole() == null) {
            pDto.setRole(roleService.getDefaultRole());
        }

        // If no associated account
        if (!accountService.existAccount(pDto.getEmail())) {
            // Initialize a new final Account with provided final info and the final pending status
            final Account account = new Account();
            account.setEmail(pDto.getEmail());
            account.setFirstName(pDto.getFirstName());
            account.setLastName(pDto.getLastName());
            account.setLogin(pDto.getLogin());
            account.setPassword(pDto.getPassword());
            account.setStatus(AccountStatus.PENDING);

            // Create it via the account service
            accountService.createAccount(account);
        }

        // Initialize the project user
        final ProjectUser projectUser = new ProjectUser();
        projectUser.setEmail(pDto.getEmail());
        projectUser.setPermissions(pDto.getPermissions());
        projectUser.setRole(pDto.getRole());
        projectUser.setMetaData(pDto.getMetaData());
        projectUser.setStatus(UserStatus.WAITING_ACCES);

        // "Save" it
        projectUsers.add(projectUser);
        return pDto;

    }

    @Override
    public List<String> getAccessSettingList() {
        final List<String> accessSettings = new ArrayList<>();
        accessSettings.add(accessSetting);
        return accessSettings;
    }

    @Override
    public void updateAccessSetting(final String pUpdatedProjectUserSetting) throws InvalidValueException {

        if (pUpdatedProjectUserSetting.toLowerCase().equals("manual")
                || pUpdatedProjectUserSetting.equals("auto-accept")) {
            accessSetting = pUpdatedProjectUserSetting.toLowerCase();
            return;
        }
        throw new InvalidValueException("Only value accepted : manual or auto-accept");

    }

    // public boolean existAccessRequest(final Account pAccount) {
    // return projectUsers.stream().filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCES))
    // .filter(p -> p.getAccount().equals(pAccount)).findFirst().isPresent();
    // }

    public boolean existAccessRequest(final String pEmail) {
        return projectUsers.stream().filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCES))
                .filter(p -> p.getEmail().equals(pEmail)).findFirst().isPresent();
    }

    @Override
    public boolean existAccessRequest(final Long pAccessRequestId) {
        return projectUsers.stream().filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCES))
                .filter(p -> p.getId() == pAccessRequestId).findFirst().isPresent();
    }

    @Override
    public void removeAccessRequest(final Long pAccessId) {
        if (existAccessRequest(pAccessId)) {
            projectUsers = projectUsers.stream().filter(p -> p.getId() != pAccessId).collect(Collectors.toList());
            return;
        }
        throw new NoSuchElementException(pAccessId + "");
    }

    @Override
    public void acceptAccessRequest(final Long pAccessId) {
        if (existAccessRequest(pAccessId)) {
            projectUsers = projectUsers.stream().map(p -> p.getId() == pAccessId ? p.accept() : p)
                    .collect(Collectors.toList());
            return;
        }
        throw new NoSuchElementException(pAccessId + "");
    }

    @Override
    public void denyAccessRequest(final Long pAccessId) {
        if (existAccessRequest(pAccessId)) {
            projectUsers = projectUsers.stream().map(p -> p.getId() == pAccessId ? p.deny() : p)
                    .collect(Collectors.toList());
            return;
        }
        throw new NoSuchElementException(pAccessId + "");
    }

}
