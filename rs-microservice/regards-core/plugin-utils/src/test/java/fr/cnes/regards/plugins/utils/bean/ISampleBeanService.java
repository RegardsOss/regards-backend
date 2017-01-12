/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.bean;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 * @author Christophe Mertz
 *
 */
@PluginInterface
public interface ISampleBeanService {
    
    public String getId();
    
    public void setId(String pId);

}
