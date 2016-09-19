/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao;

import java.util.List;

import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;

public interface IDaoResourcesAccess {

    public ResourcesAccess getById(Integer pResourcesAccessId);

    public List<ResourcesAccess> getAll();
}
