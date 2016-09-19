/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;

public interface IResourcesAccessRepository extends CrudRepository<ResourcesAccess, Long> {

}
