package fr.cnes.regards.modules.accessRights.domain;

/*
 * LICENSE_PLACEHOLDER
 */
public enum HttpVerb {
    OPTIONs(0), GET(1), HEAD(2), POST(3), PUT(4), DELETE(5), PATCH(6), TRACE(7);

    private int value;

    HttpVerb(int pValue) {
        value = pValue;
    }

    public int getValue() {
        return value;
    }
}