package fr.cnes.regards.modules.processing.plugins;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.urn.DataType;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;

@PluginInterface(description = "Defines the quotas, rights, parameters and launching properties for a Process")
public interface IProcessDefinition extends IProcessCheckerDefinition,
    IProcessParametersDefinition,
    IProcessLauncherDefinition,
    IProcessForecastDefinition
{

    Map<String, String> processInfo();

}
