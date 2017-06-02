/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * SampleServicePlugin
 *
 * @author Christophe Mertz
 */
@Plugin(description = "Sample plugin test", id = "aSampleServicePlugin", version = "0.0.1",
        author = "REGARDS Dream Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class SampleServicePlugin implements ISampleServicePlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleServicePlugin.class);

    /**
     * A {@link String} parameter
     */
    @PluginParameter(description = "string parameter", name = SUFFIXE, defaultValue = "Hello", optional = true)
    private String suffix;

    /**
     * A {@link Integer} parameter
     */
    @PluginParameter(description = "int parameter", name = COEFF)
    private Integer coef;

    /**
     * A {@link Boolean} parameter
     */
    @PluginParameter(description = "boolean parameter", name = ACTIVE, defaultValue = "false")
    private Boolean isActive;

    @Override
    public String echo(final String pMessage) {
        final StringBuffer str = new StringBuffer();
        if (isActive) {
            str.append(pMessage + " --> the suffix is '" + suffix + "'");
        } else {

            str.append(this.getClass().getName() + ":is not active");
        }
        return str.toString();
    }

    @Override
    public int add(final int pFist, final int pSecond) {
        final int res = coef * (pFist + pSecond);
        LOGGER.info("add result : " + res);
        return res;
    }

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + this.getClass().getName() + SUFFIXE + ":" + suffix + "|" + ACTIVE + ":"
                + isActive + "|" + COEFF + ":" + coef);
    }

    @Override
    public ResponseEntity<?> apply() {
        if (!isActive) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        int n = add(10, 20);
        return ResponseEntity.ok(echo("res=" + n));
    }

    @Override
    public boolean isApplyableOnOneData() {
        return true;
    }

    @Override
    public boolean isApplyableOnManyData() {
        return false;
    }

    @Override
    public boolean isApplyableOnQuery() {
        return true;
    }

}
