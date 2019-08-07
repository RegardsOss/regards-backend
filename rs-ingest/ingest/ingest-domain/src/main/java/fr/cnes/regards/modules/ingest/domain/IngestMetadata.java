/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import org.springframework.util.Assert;

/**
 * Extra information useful for bulk SIP submission.<br/>
 * The processing chain name is required and is linked to an existing processing chain.<br/>
 * The sessionSource and sessionName allows to make consistent group of SIP.
 *
 * @author Marc Sordi
 * @author Léo Mieulet
 *
 */
@Embeddable
public class IngestMetadata {
    /**
     * {@link fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain} name
     */
    @NotBlank(message = "Processing chain name is required")
    @Column(length = 100)
    private String processing;

    /**
     * Session source
     */
    @NotNull(message = "Session source is required")
    @Column(length = 128, name = "session_source")
    private String sessionSource;

    /**
     * Session name
     */
    @NotNull(message = "Session name is required")
    @Column(length = 128, name = "session_name")
    private String sessionName;


    public String getProcessing() {
        return processing;
    }

    public void setProcessing(String processing) {
        this.processing = processing;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getSessionSource() {
        return sessionSource;
    }

    public void setSessionSource(String sessionSource) {
        this.sessionSource = sessionSource;
    }

    public static IngestMetadata build(String ingestProcessingChain, String sessionSource, String sessionName) {
        Assert.notNull(ingestProcessingChain, "Ingest processing chain is required");
        IngestMetadata m = new IngestMetadata();
        m.setProcessing(ingestProcessingChain);
        m.setSessionSource(sessionSource);
        m.setSessionName(sessionName);
        return m;
    }
}
