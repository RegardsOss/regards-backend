/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.plugins;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;

/**
 * SamplePlugin
 *
 * @author Christophe Mertz
 */
@Plugin(description = "Sample plugin test", id = "aOtherFakePlugin", version = "12345-6789-11", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class FakeOtherPlugin implements IComplexInterfacePlugin {

    @Override
    public int mult(int pFirst, int pSecond) {
        return 0;
    }

}
