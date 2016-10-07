/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao.projects;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;

public interface IResourcesAccessRepository extends CrudRepository<ResourcesAccess, Long> {

}
