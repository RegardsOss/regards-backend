package fr.cnes.regards.microservices.backend.pojo.administration;

/**
 * Created by lmieulet on 02/08/16.
 */
public enum HttpVerb {
    OPTIONs(0),
    GET(1),
    HEAD(2),
    POST(3),
    PUT(4),
    DELETE(5),
    PATCH(6),
    TRACE(7);

    private int value_;

    HttpVerb(int pValue) {
        value_ = pValue;
    }

    public int getValue() {
        return value_;
    }
}
