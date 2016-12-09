/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GSON properties
 *
 * @author Marc Sordi
 *
 */
@ConfigurationProperties(prefix = "regards.gson")
public class GsonProperties {

    private String scanPrefix = "fr.cnes.regards";

    public String getScanPrefix() {
        return scanPrefix;
    }

    public void setScanPrefix(String pScanPrefix) {
        scanPrefix = pScanPrefix;
    }
}
