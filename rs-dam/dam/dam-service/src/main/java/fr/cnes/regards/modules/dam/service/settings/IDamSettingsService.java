package fr.cnes.regards.modules.dam.service.settings;

import fr.cnes.regards.framework.module.rest.exception.EntityException;

public interface IDamSettingsService {

    boolean isStoreFiles();

    void setStoreFiles(Boolean isStoreFiles) throws EntityException;

    String getStorageLocation();

    void setStorageLocation(String location) throws EntityException;

    String getStorageSubDirectory();

    void setStorageSubDirectory(String subDirectory) throws EntityException;

}
