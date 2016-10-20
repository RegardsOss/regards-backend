/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.projects;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

public interface IResourcesAccessRepository extends CrudRepository<ResourcesAccess, Long> {

}
