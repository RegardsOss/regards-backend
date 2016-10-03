/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.access.service;

import java.util.List;
import java.util.NoSuchElementException;

import javax.naming.OperationNotSupportedException;

import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

/**
 * 
 * @author cmertz
 *
 */
public interface INavigationContextService {

    NavigationContext create(NavigationContext pNavigationContext) throws AlreadyExistingException;

    void update(String pTinyUrl, NavigationContext pNavigationContext)
            throws OperationNotSupportedException, NoSuchElementException;

    void delete(String pTinyUrl) throws NoSuchElementException;

    NavigationContext load(String pTinyUrl) throws NoSuchElementException;

    List<NavigationContext> list();

}
