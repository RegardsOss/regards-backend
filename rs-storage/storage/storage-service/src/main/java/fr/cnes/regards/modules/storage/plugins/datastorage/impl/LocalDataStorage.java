/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugins.datastorage.impl;

import java.nio.file.Path;
import java.util.Map;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.plugins.datastorage.IDataStorage;
import fr.cnes.regards.modules.storage.plugins.datastorage.domain.DataStorageInfo;
import fr.cnes.regards.modules.storage.plugins.datastorage.domain.DataStorageType;
import fr.cnes.regards.modules.storage.plugins.datastorage.domain.validation.Directory;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Plugin(author = "Sylvain VISSIERE-GUERINET", description = "Plugin handling the storage on local",
        id = "LocalDataStorage", version = "1.0")
public class LocalDataStorage implements IDataStorage {

    // FIXME: comment ça se passe dans les plugin un @Value ou c'est un paramètre dynamique?
    private Path workspace;

    // to be updated by event
    @Directory
    private Map<String, Path> projectRootDirectories;

    public Map<String, Path> getProjectRootDirectories() {
        return projectRootDirectories;
    }

    public void addProjectRootDirectories(String pProject, @Directory Path pWorkspaceOfProject) {
        projectRootDirectories.put(pProject, pWorkspaceOfProject);
    }

    @Override
    public DataStorageType getType() {
        return DataStorageType.ONLINE;
    }

    @Override
    public Long storeAIPDescriptor(AIP pAip) {
        // execute storage of the descriptor
        // ask for the upload of files
        storeAIPFiles(pAip);
        return null;
    }

    @Override
    public Long retrieveAIP(AIP pAip) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long deleteAIP(AIP pAip) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataStorageInfo getInfo(String pProject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long storeAIPFiles(AIP pAip) {
        // schedule the upload job
        return null;
    }

}
