/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.domain.download;

import com.google.gson.annotations.SerializedName;

/**
 * The DTO representing the response of a file download from catalog
 * when the user hasn't accepted the license yet.
 * It contains 2 links.
 * The first link give access to the license.
 * The second link enables the license acceptation and the download of the file.
 *
 * @author Thomas Fache
 **/
public class MissingLicenseDownload implements Download {

    @SerializedName("license")
    private final String linkToLicense;

    @SerializedName("accept")
    private final String linkToAcceptAndDownload;

    /**
     * Constructor of the DTO for the file download response when the license isn't accepted yet.
     *
     * @param licenseLink           link to retrieve the license
     * @param acceptAndDownloadLink link to accept the licence and download the file
     */
    public MissingLicenseDownload(String licenseLink, String acceptAndDownloadLink) {
        linkToLicense = licenseLink;
        linkToAcceptAndDownload = acceptAndDownloadLink;
    }

    /**
     * @return the link to retrieve the license
     */
    public String getLinkToLicense() {
        return linkToLicense;
    }

    /**
     * @return the link to accept the licence and download the file
     */
    public String getLinkToAcceptAndDownload() {
        return linkToAcceptAndDownload;
    }
}
