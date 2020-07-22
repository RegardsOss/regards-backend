package fr.cnes.regards.modules.processing.plugins;

import io.vavr.control.Option;

public interface IProcessQuotasDefinition {

    Option<Integer> maxConcurrentExecutions();

    Option<Long> maxBytesInCache();

}
