/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.exception;

/**
 *
 * Base JPA multitenant exception
 * 
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class JpaMultitenantException extends Exception {

    public JpaMultitenantException(Throwable t) {
        super(t);
    }
}
