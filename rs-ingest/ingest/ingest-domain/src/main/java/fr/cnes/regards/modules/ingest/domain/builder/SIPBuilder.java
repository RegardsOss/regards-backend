/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.builder;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.oais.builder.IPBuilder;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.SIPReference;

/**
 * {@link SIP} builder.
 *
 * This builder must be used to build SIP either by value or by reference.
 *
 * @author Marc Sordi
 *
 */
public class SIPBuilder extends IPBuilder<SIP> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPBuilder.class);

    public SIPBuilder(String sipId) {
        super(SIP.class, EntityType.DATA);
        Assert.hasLength(sipId, "Supply identifier is required.");
        ip.setId(sipId);
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
     * Alias of method {@link #setReference(URL, String, String)} with a {@link Path} reference instead of {@link URL}.
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
}
