/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.access.service;

import java.util.List;

import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;

/**
 * 
 * @author Christophe Mertz
 *
 */
public interface INavigationContextService {

    NavigationContext create(NavigationContext pNavigationContext) throws AlreadyExistingException;

    void update(NavigationContext pNavigationContext) throws EntityNotFoundException;

    void delete(Long pNavCtxId) throws EntityNotFoundException;

    NavigationContext load(Long pNavCtxId) throws EntityNotFoundException;

    List<NavigationContext> list();

}
