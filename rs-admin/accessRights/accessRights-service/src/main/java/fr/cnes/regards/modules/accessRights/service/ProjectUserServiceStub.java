/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/*
 * LICENSE_PLACEHOLDER
 */
@Service
public class ProjectUserServiceStub implements IProjectUserService {

    private static List<ProjectUser> projectUsers_ = new ArrayList<>();

    @Value("${regards.project.account_acceptance}")
    private String accessSetting_;

    @Override
    public List<ProjectUser> retrieveAccessRequestList() {
        return projectUsers_.stream().filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCES))
                .collect(Collectors.toList());
    }

    @Override
    public ProjectUser requestAccess(ProjectUser pAccessRequest) throws AlreadyExistingException {
        if (existAccessRequest(pAccessRequest.getEmail())) {
            throw new AlreadyExistingException(pAccessRequest.getEmail());
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

    public boolean existAccessRequest(String pAccessId) {
        return projectUsers_.stream().filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCES))
                .filter(p -> p.getEmail().equals(pAccessId)).findFirst().isPresent();
    }

    @Override
    public void removeAccessRequest(String pAccessId) {
        if (existAccessRequest(pAccessId)) {
            projectUsers_ = projectUsers_.stream().filter(p -> !p.getEmail().equals(pAccessId))
                    .collect(Collectors.toList());
            return;
        }
        throw new NoSuchElementException(pAccessId);
    }

    @Override
    public void acceptAccessRequest(String pAccessId) {
        if (existAccessRequest(pAccessId)) {
            projectUsers_ = projectUsers_.stream().map(p -> p.getEmail().equals(pAccessId) ? p.accept() : p)
                    .collect(Collectors.toList());
            return;
        }
        throw new NoSuchElementException(pAccessId);
    }

    @Override
    public void denyAccessRequest(String pAccessId) {
        if (existAccessRequest(pAccessId)) {
            projectUsers_ = projectUsers_.stream().map(p -> p.getEmail().equals(pAccessId) ? p.deny() : p)
                    .collect(Collectors.toList());
            return;
        }
        throw new NoSuchElementException(pAccessId);
    }

}
