package fr.cnes.regards.modules.accessRights.dao;

import java.util.List;

import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;

/*
 * LICENSE_PLACEHOLDER
 */
public interface IDaoResourcesAccess {

    public ResourcesAccess getById(Integer pResourcesAccessId);

    public List<ResourcesAccess> getAll();
}
