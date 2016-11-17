/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 * Entity does not have any identifier
 *
 * @author Marc Sordi
 * @deprecated use {@link EntityNotIdentifiableException}
 */
@Deprecated
public class ModuleEntityNotIdentifiableException extends ModuleException {

    private static final long serialVersionUID = -1220166163207297225L;

    public ModuleEntityNotIdentifiableException(String pErrorMessage) {
        super(pErrorMessage);
    }

}
