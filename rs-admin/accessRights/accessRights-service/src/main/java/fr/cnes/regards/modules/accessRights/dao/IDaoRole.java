package fr.cnes.regards.modules.accessRights.dao;

import java.util.List;

import fr.cnes.regards.modules.accessRights.domain.Role;

/*
 * LICENSE_PLACEHOLDER
 */
public interface IDaoRole {

    public Role getById(Integer pRoleId);

    public List<Role> getAll();
}
