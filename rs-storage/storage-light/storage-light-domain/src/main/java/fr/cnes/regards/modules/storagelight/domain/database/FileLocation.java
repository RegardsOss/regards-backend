/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.domain.database;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Definition of a file location
 *
 * @author Sébastien Binda
 */
@Embeddable
public class FileLocation {

    /**
     * Storage where current file reference is stored.
     */
    @Column(length = 128, name = "storage")
    private String storage;

    /**
     * URL to access file reference through defined storage
     */
    @Column(length = 2048)
    private String url;

    public FileLocation() {
        super();
    }

    public FileLocation(String storage, String url) {
        super();
        this.storage = storage;
        this.url = url;
    }

    /**
     * @return the storage
     */
    public String getStorage() {
        return storage;
    }

    /**
     * @param storage the storage to set
     */
    public void setStorage(String storage) {
        this.storage = storage;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "FileLocation [" + (storage != null ? "storage=" + storage + ", " : "")
                + (url != null ? "url=" + url : "") + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((storage == null) ? 0 : storage.hashCode());
        result = (prime * result) + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FileLocation other = (FileLocation) obj;
        if (storage == null) {
            if (other.storage != null) {
                return false;
            }
        } else if (!storage.equals(other.storage)) {
            return false;
        }
        if (url == null) {
            if (other.url != null) {
                return false;
            }
        } else if (!url.equals(other.url)) {
            return false;
        }
        return true;
    }

}
