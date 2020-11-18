package fr.cnes.regards.modules.processing.plugins;

import fr.cnes.regards.modules.processing.domain.engine.IExecutable;

public interface IProcessLauncherDefinition {

    default String engineName() {
        return "JOBS";
    }

    IExecutable executable();

}
