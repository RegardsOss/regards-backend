/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.access.dao;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.access.domain.NavigationContext;

/**
 * {@link NavigationContext} repository
 * 
 * @author Christophe Mertz
 *
 */
public interface INavigationContextRepository extends CrudRepository<NavigationContext, Long> {

}