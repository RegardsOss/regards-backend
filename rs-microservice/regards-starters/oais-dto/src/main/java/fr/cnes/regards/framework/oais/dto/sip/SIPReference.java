/*

 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.oais.dto.sip;

import fr.cnes.regards.framework.oais.dto.validator.ValidSIPChecksum;
import fr.cnes.regards.framework.utils.file.validation.HandledMessageDigestAlgorithm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.net.URL;
import java.security.MessageDigest;

@ValidSIPChecksum(message = "The sip checksum is not valid for this algorithm")
public class SIPReference {

    @Schema(description = "URL of the json file containing SIP.")
    @NotNull(message = "SIP reference URL is required")
    private URL url;

    @NotBlank(message = "SIP reference checksum is required")
    private String checksum;

    /**
     * All available {@link MessageDigest} algorithm
     */
    @Schema(description = "Checksum algorithm of the json file containing SIP.")
    @NotBlank(message = "SIP reference algorithm is required")
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
