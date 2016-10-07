/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

public interface IAccessRequestService {

    List<ProjectUser> retrieveAccessRequestList();

    ProjectUser requestAccess(ProjectUser pAccessRequest) throws AlreadyExistingException;

    List<String> getAccessSettingList();

    void updateAccessSetting(String pUpdatedProjectUserSetting) throws InvalidValueException;

    /**
     * @param pAccessId
     */
    void removeAccessRequest(Long pAccessId);

    /**
     * @param pAccessId
     */
    void acceptAccessRequest(Long pAccessId);

    /**
     * @param pAccessId
     */
    void denyAccessRequest(Long pAccessId);

    boolean existAccessRequest(Long pAccessRequestId);

}
