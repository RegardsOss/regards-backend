package fr.cnes.regards.modules.storage.plugin.staf.domain;

public class STAFUrlFactory {

    public static final String STAF_URL_PROTOCOLE = "staf";

    public static final String STAF_URL_REGEXP = "^staf://(.*)/([^?]*)?{0,1}(.*)$";

    private STAFUrlFactory() {

    }

    public static String getSTAFFullURL(String pArchiveName, String pStafNode, String pStafFileName) {
        return "";
    }

}
