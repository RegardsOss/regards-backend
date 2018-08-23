package fr.cnes.regards.framework.modules.plugins.service.it;

import fr.cnes.regards.framework.modules.plugins.ISamplePlugin;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * Sample plugin with sensitive parameter. Be aware that {@link SensitivePlugin#echo(String)} method does not care
 * about the method parameter, it only echoes the plugin parameter
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(description = "Test sensitive plugin", id = "SensitivePlugin", version = "0.0.1", author = "REGARDS Dream Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class SensitivePlugin implements ISamplePlugin {

    public static final String MESSAGE_PLUGIN_PARAM = "message";

    @PluginParameter(name = MESSAGE_PLUGIN_PARAM, label = "message", sensitive = true)
    private String message;

    @Override
    public String echo(String pMessage) {
        return message;
    }

    @Override
    public int add(int pFirst, int pSecond) {
        return 0;
    }
}
