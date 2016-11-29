/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 *
 * @author Xavier-Alexandre Brochard
 */
public class TemplateWriterException extends ModuleException {

    /**
     *
     */
    private static final long serialVersionUID = -5051656021884503724L;

    /**
     * Constructs a new template writer exception.
     */
    public TemplateWriterException() {
        super("An error occured during template writing");
    }

}
