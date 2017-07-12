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
package fr.cnes.regards.framework.modules.jobs.domain;

import java.net.URI;

/**
 * Job output
 * 
 * @author Léo Mieulet
 */
public class Output {

    /**
     * Job mimetype
     */
    private String mimeType;

    /**
     * Job path
     */
    private URI data;

    /**
     * Default constructor
     */
    public Output() {
        super();
    }

    /**
     * Constructor with the attributes
     * 
     * @param pMimeType
     *            the data's MimeType
     * @param pData
     *            the data's URI
     */
    public Output(String pMimeType, URI pData) {
        super();
        this.mimeType = pMimeType;
        this.data = pData;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String pMimeType) {
        mimeType = pMimeType;
    }

    public URI getData() {
        return data;
    }

    public void setData(final URI pData) {
        data = pData;
    }
}
