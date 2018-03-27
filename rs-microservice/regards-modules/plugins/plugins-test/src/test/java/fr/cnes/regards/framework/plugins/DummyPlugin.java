package fr.cnes.regards.framework.plugins;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;

/**
 * Dummy plugin just there to be used as support for plugin service tests
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(description = "Dummy plugin test", id = "DummyPlugin", version = "0.0.1", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class DummyPlugin implements ISamplePlugin {

    @Override
    public String echo(String pMessage) {
        return null;
    }

    @Override
    public int add(int pFirst, int pSecond) {
        return 0;
    }
}
