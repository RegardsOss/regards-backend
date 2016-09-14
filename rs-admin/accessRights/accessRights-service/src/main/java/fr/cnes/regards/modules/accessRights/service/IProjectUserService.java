/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

public interface IProjectUserService {

    List<ProjectUser> retrieveAccessRequestList();

    ProjectUser requestAccess(ProjectUser pAccessRequest) throws AlreadyExistingException;

    List<String> getAccessSettingList();

    void removeAccessRequest(String pAccessId);

    void acceptAccessRequest(String pAccessId);

    void denyAccessRequest(String pAccessId);

    void updateAccessSetting(String pUpdatedProjectUserSetting) throws InvalidValueException;

}
