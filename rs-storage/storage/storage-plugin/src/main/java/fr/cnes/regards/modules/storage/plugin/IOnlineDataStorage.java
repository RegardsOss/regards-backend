package fr.cnes.regards.modules.storage.plugin;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Multimap;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.DataFile;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IOnlineDataStorage<T extends IWorkingSubset>  extends IDataStorage<T>{

    @Override
    default DataStorageType getType() {
        return DataStorageType.ONLINE;
    }

}
