/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.ingest.domain.request.manifest;

/**
 * Object extracted from either an IngestRequest or an existing AIP,
 * and used to send the manifest on the right storage location
 * @author Léo Mieulet
 */
public class StoreLocation {
    /**
     * Storage identifier
     */
    private String storage;

    /**
     * Optional path identifying the base directory in which to store related files
     */
    private String storePath;

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getStorePath() {
        return storePath;
    }

    public void setStorePath(String storePath) {
        this.storePath = storePath;
    }

    public static StoreLocation build(String storage, String storePath) {
        StoreLocation sl = new StoreLocation();
        sl.setStorage(storage);
        sl.setStorePath(storePath);
        return sl;
    }
}
