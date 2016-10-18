/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import fr.cnes.regards.modules.accessRights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidEntityException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

public interface IAccessRequestService {

    List<ProjectUser> retrieveAccessRequestList();

    AccessRequestDTO requestAccess(AccessRequestDTO pAccessRequest)
            throws AlreadyExistingException, InvalidEntityException;

    List<String> getAccessSettingList();

    void updateAccessSetting(String pUpdatedProjectUserSetting) throws InvalidValueException;

    /**
     * @param pAccessId
     *            The resource access (project user) 's id
     * @throws EntityNotFoundException
     *             Thrown when a project user with passed id could not be found
     */
    void removeAccessRequest(Long pAccessId) throws EntityNotFoundException;

    /**
     * @param pAccessId
     *            The resource access (project user) 's id
     * @throws EntityNotFoundException
     *             Thrown when a project user with passed id could not be found
     */
    void acceptAccessRequest(Long pAccessId) throws EntityNotFoundException;

    /**
     * @param pAccessId
     *            The resource access (project user) 's id
     * @throws EntityNotFoundException
     *             Thrown when a project user with passed id could not be found
     */
    void denyAccessRequest(Long pAccessId) throws EntityNotFoundException;

    boolean existAccessRequest(Long pAccessRequestId);

}
