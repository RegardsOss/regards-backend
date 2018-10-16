/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.util.Assert;

/**
 * Extra information useful for bulk SIP submission.<br/>
 * The processing chain name is required and is linked to an existing processing chain.<br/>
 * The session identifier allows to make consistent group of SIP.
 *
 * @author Marc Sordi
 *
 */
public class IngestMetadata {

    /**
     * Processing chain name
     */
    @NotBlank
    private String processing;

    /**
     * Session name
     */
    private String session;

    public String getProcessing() {
        return processing;
    }

    public void setProcessing(String processing) {
        this.processing = processing;
    }

    public Optional<String> getSession() {
        return Optional.ofNullable(session);
    }

    public void setSession(String session) {
        this.session = session;
    }

    public static IngestMetadata build(String ingestProcessingChain, String session) {
        Assert.notNull(ingestProcessingChain, "Ingest processing chain is required");
        IngestMetadata m = new IngestMetadata();
        m.setProcessing(ingestProcessingChain);
        m.setSession(session);
        return m;
    }
}
