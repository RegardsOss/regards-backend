/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * This exception is thrown when a plugin is instanciated with a dynamic parameter that should not be.
 *
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class UnexpectedDynamicParameter extends ModuleException {

    public UnexpectedDynamicParameter(String pErrorMessage) {
        super(pErrorMessage);
    }
}
