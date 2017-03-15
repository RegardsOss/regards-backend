/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 * Error occurs when an exception is thrown during OpenSearch query parsing process.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
public class SearchException extends ModuleException {

    /**
     * Serial
     */
    private static final long serialVersionUID = 3010327942501659512L;

    public SearchException(String pQuery, Throwable pCause) {
        super(String.format("Could not handle the query %s", pQuery), pCause);
    }

}
