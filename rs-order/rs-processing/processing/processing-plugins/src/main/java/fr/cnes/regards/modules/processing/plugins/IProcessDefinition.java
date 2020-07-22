package fr.cnes.regards.modules.processing.plugins;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.urn.DataType;
import io.vavr.collection.Seq;

@PluginInterface(description = "Defines the quotas, rights, parameters and launching properties for a Process")
public interface IProcessDefinition extends
    IProcessQuotasDefinition,
    IProcessRightsDefinition,
    IProcessParametersDefinition,
    IProcessLauncherDefinition,
    IProcessForecastDefinition
{

    String processName();

    boolean isActive();

    Seq<DataType> requiredDataTypes();

}
