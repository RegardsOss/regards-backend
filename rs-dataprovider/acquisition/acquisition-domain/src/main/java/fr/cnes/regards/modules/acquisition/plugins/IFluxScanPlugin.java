package fr.cnes.regards.modules.acquisition.plugins;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import org.apache.commons.lang3.NotImplementedException;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * First <b>required</b> step of acquisition processing chain. This step is used to make disk scanning for file
 * detection by using a stream to minimize the mapping of the folder(s) being scanned.
 *
 * @author Simon MILHAU
 */
@PluginInterface(description = "Files scanning by stream plugin contract")
public interface IFluxScanPlugin extends IScanPlugin {

    @Override
    default List<Path> scan(Path dirPath, Optional<OffsetDateTime> scanningDate) {
        throw new NotImplementedException("Stream scan should not be used for synchronous scanning !!");
    }

    List<Stream<Path>> stream(Path dirPath, Optional<OffsetDateTime> scanningDate) throws ModuleException;
}
