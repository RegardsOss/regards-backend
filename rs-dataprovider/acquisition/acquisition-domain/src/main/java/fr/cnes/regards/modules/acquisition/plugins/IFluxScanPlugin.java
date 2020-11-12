package fr.cnes.regards.modules.acquisition.plugins;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.acquisition.domain.chain.ScanDirectoriesInfo;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.NotImplementedException;

/**
 * First <b>required</b> step of acquisition processing chain. This step is used to make disk scanning for file
 * detection by using a stream to minimize the mapping of the folder(s) being scanned.
 *
 * @author Simon MILHAU
 *
 */
@PluginInterface(description = "Files scanning by stream plugin contract")
public interface IFluxScanPlugin extends IScanPlugin {

    @Override
    default Map<Path, Optional<OffsetDateTime>> scan(Set<ScanDirectoriesInfo> scanDirectoriesInfo) {
        throw new NotImplementedException("Stream scan should not be used for synchronous scanning !!");
    }

    Map<Path, Optional<OffsetDateTime>> stream(Set<ScanDirectoriesInfo> scanDirectoriesInfo) throws ModuleException;
}
