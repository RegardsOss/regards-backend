package fr.cnes.regards.framework.staf.protocol;

public enum STAFUrlParameter {

    /**
     * filename parameter of the url to indicate the name of the file associated to the URL into the given TAR.
     * Exemple : staf:/ARCHIVE/node/foo.tar?filename=bar.txt
     */
    TAR_FILENAME_PARAMETER("filename"),

    /**
     * parts parameter of the url to indicate the number of cute files stored into STAF and composing the full file stored.
     * Exemple for a file cuted in 12 parts into STAF : staf:/ARCHIVE/node/foo.txt?parts=12
     */
    CUT_PARTS_PARAMETER("parts");

    private String parameterName;

    private STAFUrlParameter(String pParameterName) {
        parameterName = pParameterName;
    }

    public String getParameterName() {
        return parameterName;
    }

}
