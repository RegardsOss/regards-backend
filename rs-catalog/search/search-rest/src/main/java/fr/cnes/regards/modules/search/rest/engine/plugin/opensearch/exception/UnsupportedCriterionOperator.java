package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception;

@SuppressWarnings("serial")
public class UnsupportedCriterionOperator extends Exception {

    public UnsupportedCriterionOperator() {
        super();
    }

    public UnsupportedCriterionOperator(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public UnsupportedCriterionOperator(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedCriterionOperator(String message) {
        super(message);
    }

    public UnsupportedCriterionOperator(Throwable cause) {
        super(cause);
    }

}
