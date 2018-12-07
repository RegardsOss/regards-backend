package fr.cnes.regards.framework.module.rest.utils;

/**
 * Utility class to send back to the front a boolean representing the validity of something
 * @author Sylvain VISSIERE-GUERINET
 */
public final class Validity {

    private Boolean validity; //NOSONAR

    /**
     * private constructor for serialization issue
     */
    private Validity() {
    }

    /**
     * Constructor setting the validity
     */
    public Validity(final Boolean pValidity) {
        validity = pValidity;
    }

    /**
     * @return the validity
     */
    public Boolean getValidity() {
        return validity;
    }

    /**
     * Set the validity
     */
    public void setValidity(final Boolean pValidity) {
        validity = pValidity;
    }

}
