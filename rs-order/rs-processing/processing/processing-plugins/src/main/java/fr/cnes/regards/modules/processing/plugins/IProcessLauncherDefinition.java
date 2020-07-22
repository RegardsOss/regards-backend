package fr.cnes.regards.modules.processing.plugins;

import fr.cnes.regards.modules.processing.domain.engine.IExecutable;

public interface IProcessLauncherDefinition {

    String engineName();

    IExecutable executable();

}
