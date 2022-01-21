/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.dto.sip;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.oais.builder.IPBuilder;
import fr.cnes.regards.framework.urn.EntityType;

/**
 *
 * This {@link SIP} builder must be used to build SIP either by value or by reference.<br/>
 *
 * To build SIP by reference, use {@link #buildReference(Path, String, String)} or
 * {@link #buildReference(URL, String, String)} directly.<br/>
 * To build SIP by value, use all other methods to fill in the SIP part by part then call {@link #build()} to get it at
 * the end.
 *
 * @author Marc Sordi
 *
 * @deprecated {@link SIP} fluent API instead
 */
@Deprecated
public class SIPBuilder extends IPBuilder<SIP> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPBuilder.class);

    public SIPBuilder(String providerId) {
        super(SIP.class, EntityType.DATA);
        Assert.hasLength(providerId, "Supply identifier is required.");
        ip.setId(providerId);
    }

    /**
     * Use this method to build a referenced SIP.
     * @param url URL of the SIP file
     * @param algorithm {@link MessageDigest} checksum algorithm
     * @param checksum checksum for current SIP file
     */
    public SIP buildReference(URL url, String algorithm, String checksum) {
        Assert.notNull(url, "URL is required");
        Assert.hasText(algorithm, "Checksum algorithm is required");
        Assert.hasText(checksum, "Checksum is required");
        SIPReference reference = new SIPReference();
        reference.setAlgorithm(algorithm);
        reference.setChecksum(checksum);
        reference.setUrl(url);
        ip.setRef(reference);
        return ip;
    }

    /**
     * Alias of method {@link #buildReference(URL, String, String)} with a {@link Path} reference instead of
     * {@link URL}.
     * @param filePath path to the SIP file
     * @param algorithm {@link MessageDigest} checksum algorithm
     * @param checksum checksum for current SIP file
     */
    public SIP buildReference(Path filePath, String algorithm, String checksum) {
        Assert.notNull(filePath, "File path is required");
        try {
            return buildReference(filePath.toUri().toURL(), algorithm, checksum);
        } catch (MalformedURLException e) {
            String errorMessage = String.format("Cannot transform %s to valid URL (MalformedURLException).",
                                                filePath.toString());
            LOGGER.error(errorMessage, e);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Alias for method {@link #buildReference(URL, String, String)} with MD5 default checksum algorithm
     * @param url URL of the SIP file
     * @param checksum checksum for current SIP file
     */
    public SIP buildReference(URL url, String checksum) {
        return buildReference(url, MD5_ALGORITHM, checksum);
    }

    /**
     * Alias for method {@link #buildReference(Path, String, String)} with MD5 default checksum algorithm
     * @param filePath path to the SIP file
     * @param checksum checksum for current SIP file
     */
    public SIP buildReference(Path filePath, String checksum) {
        return buildReference(filePath, MD5_ALGORITHM, checksum);
    }
}
