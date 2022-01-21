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

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.oais.AbstractInformationPackage;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dto.sip.validator.CheckSIP;
import fr.cnes.regards.modules.ingest.dto.sip.validator.CheckSIPId;

/**
 *
 * SIP representation based on OAIS information package standard structure as well as GeoJson structure.<br/>
 * Base representation is used for SIP passed by value.<br/>
 * "ref" extension attribute is used for SIP passed by reference.<br/>
 *
 * To build a {@link SIP}, you have to use a {@link SIPBuilder}.
 *
 * @author Marc Sordi
 *
 */
@CheckSIP(message = "The SIP must be sent either by reference or by value")
@CheckSIPId(message = "The SIP identifier is required")
public class SIP extends AbstractInformationPackage<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIP.class);

    /**
     * ref : extension attribute for SIP passed by reference. Should be null if SIP passed by value.
     */
    @Valid
    private SIPReference ref;

    public SIPReference getRef() {
        return ref;
    }

    public void setRef(SIPReference ref) {
        this.ref = ref;
    }

    public boolean isRef() {
        return ref != null;
    }

    // Fluent API

    /**
     * Build a new {@link SIP}
     * @param type {@link EntityType}
     * @param providerId the provider id
     */
    public static SIP build(EntityType type, String providerId) {
        Assert.notNull(type, "OAISEntity type is required.");
        Assert.hasLength(providerId, "Provider identifier is required.");
        return new SIP().withIdAndType(providerId, type);
    }

    /**
     * Use this method to build a referenced SIP.
     * @param type {@link EntityType}
     * @param providerId the provider id
     * @param url URL of the SIP file
     * @param algorithm {@link MessageDigest} checksum algorithm
     * @param checksum checksum for current SIP file
     */
    public static SIP buildReference(EntityType type, String providerId, URL url, String algorithm, String checksum) {

        Assert.notNull(url, "URL is required");
        Assert.hasText(algorithm, "Checksum algorithm is required");
        Assert.hasText(checksum, "Checksum is required");
        SIPReference reference = new SIPReference();
        reference.setAlgorithm(algorithm);
        reference.setChecksum(checksum);
        reference.setUrl(url);

        SIP sip = build(type, providerId);
        sip.setRef(reference);
        sip.setProperties(null); // Remove properties so remove above fake CAT
        return sip;
    }

    /**
     * Alias of method {@link #buildReference(EntityType, String, URL, String)} with a {@link Path} reference instead of
     * {@link URL}.
     * @param type {@link EntityType}
     * @param providerId the provider id
     * @param filePath path to the SIP file
     * @param algorithm {@link MessageDigest} checksum algorithm
     * @param checksum checksum for current SIP file
     */
    public static SIP buildReference(EntityType type, String providerId, Path filePath, String algorithm,
            String checksum) {
        Assert.notNull(filePath, "File path is required");
        try {
            return buildReference(type, providerId, filePath.toUri().toURL(), algorithm, checksum);
        } catch (MalformedURLException e) {
            String errorMessage = String.format("Cannot transform %s to valid URL (MalformedURLException).",
                                                filePath.toString());
            LOGGER.error(errorMessage, e);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Alias for method {@link #buildReference(EntityType, String, URL, String, String)} with MD5 default checksum algorithm
     * @param type {@link EntityType}
     * @param providerId the provider id
     * @param url URL of the SIP file
     * @param checksum checksum for current SIP file
     */
    public static SIP buildReference(EntityType type, String providerId, URL url, String checksum) {
        return buildReference(type, providerId, url, ContentInformation.MD5_ALGORITHM, checksum);
    }

    /**
     * Alias for method {@link #buildReference(EntityType, String, Path, String)} with MD5 default checksum algorithm
     * @param type {@link EntityType}
     * @param providerId the provider id
     * @param filePath path to the SIP file
     */
    public static SIP buildReference(EntityType type, String providerId, Path filePath, String checksum) {
        return buildReference(type, providerId, filePath, ContentInformation.MD5_ALGORITHM, checksum);
    }
}
