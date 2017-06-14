/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.accessright;

import fr.cnes.regards.framework.module.rest.exception.SearchException;

/**
 *
 * This exception is thrown if access right filter cannot be set.
 *
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class AccessRightFilterException extends SearchException {

    public AccessRightFilterException(String pErrorMessage) {
        super(pErrorMessage);
    }

}
