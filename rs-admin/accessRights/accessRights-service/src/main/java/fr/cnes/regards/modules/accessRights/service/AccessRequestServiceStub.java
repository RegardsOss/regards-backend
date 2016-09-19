/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/*
 * LICENSE_PLACEHOLDER
 */
@Service
public class AccessRequestServiceStub implements IAccessRequestService {

    private static List<ProjectUser> projectUsers_ = new ArrayList<>();

    @Autowired
    private IAccountService accountService;

    @Autowired
    private IRoleService roleService;

    @Value("${regards.project.account_acceptance}")
    private String accessSetting_;

    @Override
    public List<ProjectUser> retrieveAccessRequestList() {
        return projectUsers_.stream().filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCES))
                .collect(Collectors.toList());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IAccessRequestService#requestAccess(fr.cnes.regards.modules.
     * accessRights.domain.Account)
     */
    @Override
    public ProjectUser requestAccess(ProjectUser pAccessRequest) throws AlreadyExistingException {
        if (!this.accountService.existAccount(pAccessRequest.getAccount().getId())) {
            this.accountService.createAccount(pAccessRequest.getAccount());
        }
        if (existAccessRequest(pAccessRequest.getAccount())) {
            throw new AlreadyExistingException(
                    pAccessRequest.getAccount().getEmail() + " already has made an access request for this project");
        }
        if (pAccessRequest.getRole() == null) {
            pAccessRequest.setRole(roleService.getDefaultRole());
        }

        projectUsers_.add(pAccessRequest);
        return pAccessRequest;

    }

    @Override
    public List<String> getAccessSettingList() {
        List<String> accessSettings = new ArrayList<>();
        accessSettings.add(accessSetting_);
        return accessSettings;
    }

    @Override
    public void updateAccessSetting(String pUpdatedProjectUserSetting) throws InvalidValueException {

        if (pUpdatedProjectUserSetting.toLowerCase().equals("manual")
                || pUpdatedProjectUserSetting.equals("auto-accept")) {
            accessSetting_ = pUpdatedProjectUserSetting.toLowerCase();
            return;
        }
        throw new InvalidValueException("Only value accepted : manual or auto-accept");

    }

    public boolean existAccessRequest(Account pAccount) {
        return projectUsers_.stream().filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCES))
                .filter(p -> p.getAccount().equals(pAccount)).findFirst().isPresent();
    }

    public boolean existAccessRequest(Long pAccessRequestId) {
        return projectUsers_.stream().filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCES))
                .filter(p -> p.getId() == pAccessRequestId).findFirst().isPresent();
    }

    @Override
    public void removeAccessRequest(Long pAccessId) {
        if (existAccessRequest(pAccessId)) {
            projectUsers_ = projectUsers_.stream().filter(p -> p.getId() != pAccessId).collect(Collectors.toList());
            return;
        }
        throw new NoSuchElementException(pAccessId + "");
    }

    @Override
    public void acceptAccessRequest(Long pAccessId) {
        if (existAccessRequest(pAccessId)) {
            projectUsers_ = projectUsers_.stream().map(p -> p.getId() == pAccessId ? p.accept() : p)
                    .collect(Collectors.toList());
            return;
        }
        throw new NoSuchElementException(pAccessId + "");
    }

    @Override
    public void denyAccessRequest(Long pAccessId) {
        if (existAccessRequest(pAccessId)) {
            projectUsers_ = projectUsers_.stream().map(p -> p.getId() == pAccessId ? p.deny() : p)
                    .collect(Collectors.toList());
            return;
        }
        throw new NoSuchElementException(pAccessId + "");
    }

}
