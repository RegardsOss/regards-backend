package fr.cnes.regards.modules.order.domain.exception;

/**
 * Lists possible errors with label field at order creation
 *
 * @author Raphaël Mechali
 */
public enum OrderLabelErrorEnum {
    TOO_MANY_CHARACTERS_IN_LABEL,
    LABEL_NOT_UNIQUE_FOR_OWNER,
}
