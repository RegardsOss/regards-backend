package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.util.Set;

import org.assertj.core.util.Sets;

public class STAFUrlFactory {

    public static final String STAF_URL_PROTOCOLE = "staf";

    public static final String STAF_URL_REGEXP = "^staf://(.*)/([^?]*)?{0,1}(.*)$";

    private STAFUrlFactory() {

    }

    public static Set<STAFPhysicalFile> fromSTAFUrl(String pUrl) {
        Set<STAFPhysicalFile> stafLocations = Sets.newHashSet();
        // TODO : !!
        return stafLocations;

    }

    public static String toSTAFUrl(Set<STAFPhysicalFile> pFileLocations) {
        String stafUrl = "";
        // TODO : !!
        return stafUrl;
    }

}
