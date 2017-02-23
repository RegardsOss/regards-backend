/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.aggregate;

/**
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class AggregationException extends Exception {

    public AggregationException(String pMessage) {
        super(pMessage);
    }

    public AggregationException(String pMessage, Throwable pT) {
        super(pMessage, pT);
    }
}
