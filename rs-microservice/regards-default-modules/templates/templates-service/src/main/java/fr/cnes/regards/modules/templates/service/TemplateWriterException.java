/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.service;

/**
 *
 * @author Xavier-Alexandre Brochard
 */
public class TemplateWriterException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -5051656021884503724L;

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param pCause
     *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public TemplateWriterException(final Throwable pCause) {
        super("An error occured during template writing", pCause);
    }

}
