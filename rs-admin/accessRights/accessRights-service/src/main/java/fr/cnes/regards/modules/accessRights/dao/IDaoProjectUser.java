/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao;

import java.util.List;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;

public interface IDaoProjectUser {

    public ProjectUser getByEmail(String pEmail);

    public List<ProjectUser> getAll();
}
