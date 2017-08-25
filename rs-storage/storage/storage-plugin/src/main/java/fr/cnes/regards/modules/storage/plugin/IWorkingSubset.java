package fr.cnes.regards.modules.storage.plugin;

import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.DataFile;

public interface IWorkingSubset {

    Set<DataFile> getDataFiles();

}
