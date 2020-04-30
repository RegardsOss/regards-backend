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
package fr.cnes.regards.framework.oais;

import java.net.MalformedURLException;
import java.nio.file.Path;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 *
 * {@link OAISDataObject} storage location information
 *
 * @author Marc SORDI
 *
 */
public class OAISDataObjectLocation {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAISDataObjectLocation.class);

    private static final String URL_REQUIRED = "URL is required";

    /**
     * Storage identifier may be null, so the file is directly accessible through FILE or HTTP URL protocol
     */
    private String storage;

    /**
     * URL to access the file
     */
    @NotNull(message = URL_REQUIRED)
    @Pattern(regexp = "\\b[a-zA-Z]+://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
    private String url;

    /**
     * Optional path identifying the base directory where is store the file
     */
    private String storePath;

    /**
     * Build a file location directly accessible through FILE or HTTP URL protocol
     */
    public static OAISDataObjectLocation build(String url) {
        Assert.notNull(url, URL_REQUIRED);
        return buildInternal(url, null, null);
    }

    /**
     * Build a file location
     * accessible through storage service if storage identifier is recognize
     * else just treated as a reference.
     */
    public static OAISDataObjectLocation build(String url, String storage) {
        Assert.notNull(url, URL_REQUIRED);
        Assert.hasText(storage, "Storage identifier is required");
        return buildInternal(url, storage, null);
    }

    /**
     * Build a file location
     * accessible through storage service if storage identifier is recognize
     * else just treated as a reference.
     */
    public static OAISDataObjectLocation build(String url, String storage, String storePath) {
        Assert.notNull(url, URL_REQUIRED);
        Assert.hasText(storage, "Storage identifier is required");
        return buildInternal(url, storage, storePath);
    }

    /**
     * Build a file location directly accessible through FILE protocol
     */
    public static OAISDataObjectLocation build(Path path) {
        Assert.notNull(path, "File path is required");
        try {
            return buildInternal(path.toUri().toURL().toString(), null, null);
        } catch (MalformedURLException e) {
            String errorMessage = String.format("Cannot transform %s to valid URL (MalformedURLException).",
                                                path.toString());
            LOGGER.error(errorMessage, e);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static OAISDataObjectLocation buildInternal(String url, String storage, String storePath) {
        OAISDataObjectLocation location = new OAISDataObjectLocation();
        location.setUrl(url);
        location.setStorage(storage);
        location.setStorePath(storePath);
        return location;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStorePath() {
        return storePath;
    }

    public void setStorePath(String storePath) {
        this.storePath = storePath;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (storage == null ? 0 : storage.hashCode());
        result = prime * result + (url == null ? 0 : url.hashCode());
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
        OAISDataObjectLocation other = (OAISDataObjectLocation) obj;
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
