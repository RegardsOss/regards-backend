/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

/**
 *
 *
 * Exception managing resource
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class MethodException extends Exception {

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = -4654621513797038591L;

    public MethodException(String pMessage) {
        super(pMessage);
    }

    public MethodException(Throwable pCause) {
        super(pCause);
    }
}
