/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.exception;

public class AlreadyExistingException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -2444321445173304039L;

    public AlreadyExistingException(String dataId) {
        super("Data with id : " + dataId + " already Exist");
    }
}
