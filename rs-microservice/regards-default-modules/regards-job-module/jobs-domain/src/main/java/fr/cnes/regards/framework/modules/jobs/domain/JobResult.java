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

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.net.URI;
import java.net.URISyntaxException;

import org.hibernate.annotations.Type;

/**
 * Job result
 * @author Léo Mieulet
 * @author oroussel
 */
@Embeddable
public class JobResult {

    /**
     * Job mimetype
     */
    @Column(length = 80, nullable = false)
    private String mimeType;

    /**
     * Job path
     */
    @Column(nullable = false)
    @Type(type = "text")
    private String uri;

    /**
     * Default constructor
     */
    public JobResult() {
        super();
    }

    /**
     * Constructor with the attributes
     * @param mimeType the uri's MimeType
     * @param uri the uri's URI
     */
    public JobResult(String mimeType, URI uri) {
        super();
        this.mimeType = mimeType;
        this.uri = uri.toString();
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String pMimeType) {
        mimeType = pMimeType;
    }

    public URI getUri() {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e); // NOSONAR
        }
    }

    public void setUri(URI uri) {
        this.uri = uri.toString();
    }
}
