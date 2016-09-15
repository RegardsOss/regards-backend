/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;

/**
 * @author svissier
 *
 */
@Service
public class UserServiceStub implements IUserService {

    /*
     * (non-Javadoc)
     * 
     * @see fr.cnes.regards.modules.accessRights.service.IUserService#retrieveUserList()
     */
    @Override
    public List<ProjectUser> retrieveUserList() {
        // TODO Auto-generated method stub
        return null;
    }

}
