/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

@Deprecated
public class InvalidValueException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -346047284102185904L;

    public InvalidValueException() {
        super();
    }

    public InvalidValueException(final String message) {
        super(message);
    }
}
