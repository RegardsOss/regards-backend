/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.exception;

/**
 * Base JPA exception
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class JpaException extends Exception {

    public JpaException(Throwable t) {
        super(t);
    }
}