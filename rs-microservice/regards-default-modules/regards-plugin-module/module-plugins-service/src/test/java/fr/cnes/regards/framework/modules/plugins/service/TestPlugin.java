package fr.cnes.regards.framework.modules.plugins.test; 
 
import java.util.List; 
 
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin; 
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter; 
 
@Plugin(description = "Complex Plugin de test", id = "complexPlugin", version = "0.0.1", author = "REGARDS Dream Team", 
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss") 
public class TestPlugin implements ITestPlugin { 
 
    @PluginParameter(name = "stringParam", description = "la description", optional = true) 
    private String stringParam; 
 
    @PluginParameter(name = "pojoParam", description = "la description", optional = true) 
    private TestPojo pojoParam; 
 
    @PluginParameter(name = "pojoParams", description = "la description", optional = true) 
    private List<TestPojo> pojoParams; 
 
} 