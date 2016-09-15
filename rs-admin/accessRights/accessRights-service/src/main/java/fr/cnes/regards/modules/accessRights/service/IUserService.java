/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;

/**
 * @author svissier
 *
 */
public interface IUserService {

    /**
     * @return
     */
    List<ProjectUser> retrieveUserList();

}
