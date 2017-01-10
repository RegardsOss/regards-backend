/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * SampleBeanFactoryPlugin
 * 
 * @author Christophe Mertz
 *
 */
@Plugin(author = "CSSI", description = "Sample plugin test", id = "aSamplePlugin", version = "0.0.1")
public class SampleBeanFactoryPlugin implements ISamplePlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleBeanFactoryPlugin.class);

    /**
     * A {@link String} parameter
     */
    @PluginParameter(description = "string parameter", name = SUFFIXE)
    private String suffix;

    /**
     * An Autowired field 
     */
    @Autowired
    private ISampleBeanService sampleBeanService;

    @Override
    public String echo(final String pMessage) {
        final StringBuffer str = new StringBuffer();
        str.append(this.getClass().getCanonicalName() + " -> " + pMessage + " - " + this.suffix);
        sampleBeanService.setId("---> add string with PluginService");
        return str.toString() + sampleBeanService.getId();
    }

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + " suffixe=" + this.suffix);
    }

}
