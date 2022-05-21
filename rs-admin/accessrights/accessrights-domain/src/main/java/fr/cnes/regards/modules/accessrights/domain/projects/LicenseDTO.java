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
package fr.cnes.regards.modules.accessrights.domain.projects;

/**
 * @author Sylvain Vissiere-Guerinet
 */
public class LicenseDTO {

    /**
     * whether the current user has accepted or not the licence
     */
    private boolean accepted;

    /**
     * URL to download the licence
     */
    private String licenseLink;

    /**
     * Default constructor
     */
    protected LicenseDTO() {
        // for (de)serialization
    }

    /**
     * Constructor setting the parameters as attributes
     *
     * @param pAccepted
     * @param pLicenceLink
     */
    public LicenseDTO(boolean pAccepted, String pLicenceLink) {
        super();
        accepted = pAccepted;
        licenseLink = pLicenceLink == null ? "" : pLicenceLink;
    }

    /**
     * @return whether the license is accepted or not
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Set whether the license is accepted or not
     *
     * @param pAccepted
     */
    public void setAccepted(boolean pAccepted) {
        accepted = pAccepted;
    }

    /**
     * @return the link to the license
     */
    public String getLicenceLink() {
        return licenseLink;
    }

    /**
     * Set the link to the license
     *
     * @param pLicenceLink
     */
    public void setLicenceLink(String pLicenceLink) {
        licenseLink = pLicenceLink == null ? "" : pLicenceLink;
    }

}
