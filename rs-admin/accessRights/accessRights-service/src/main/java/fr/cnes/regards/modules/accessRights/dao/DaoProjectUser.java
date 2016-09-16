/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;

@Repository
public class DaoProjectUser implements IDaoProjectUser {

    @Override
    public ProjectUser getByEmail(String pEmail) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ProjectUser> getAll() {
        // TODO Auto-generated method stub
        return null;
    }

}
