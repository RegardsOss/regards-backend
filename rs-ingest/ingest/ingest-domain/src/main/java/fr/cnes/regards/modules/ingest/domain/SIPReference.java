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
package fr.cnes.regards.modules.ingest.domain;

import javax.validation.constraints.NotNull;
import java.net.URL;
import java.security.MessageDigest;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.utils.file.validation.HandledMessageDigestAlgorithm;

/**
 *
 * This class represents a SIP passed by reference
 *
 * @author Marc Sordi
 *
 */
public class SIPReference {

    @NotNull
    private URL url;

    @NotBlank
    private String checksum;

    /**
     * All available {@link MessageDigest} algorithm
     */
    @NotBlank
    @HandledMessageDigestAlgorithm
    private String algorithm;

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
