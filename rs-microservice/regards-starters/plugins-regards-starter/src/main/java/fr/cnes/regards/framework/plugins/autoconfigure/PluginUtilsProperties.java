/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.plugins.autoconfigure;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * class regrouping properties about the plugins
 * 
 * @author Christophe Mertz
 *
 */
@ConfigurationProperties(prefix = "regards.plugins")
public class PluginUtilsProperties {

    /**
     * A {@link List} of package to scan
     */
    private List<String> packagesToScan;
    
    public List<String> getPackagesToScan() {
        return packagesToScan;
    }

    public void setPackagesToScan(List<String> pScanPrefix) {
        packagesToScan = pScanPrefix;
    }
}