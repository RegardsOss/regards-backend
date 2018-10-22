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
package fr.cnes.regards.modules.ingest.domain.builder;

import java.util.Collection;

import org.springframework.util.Assert;

import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;

/**
 *
 * Builder for {@link SIPCollection}.<br/>
 *
 * <b>Processing chain identifier</b> is required in order to process the current {@link SIP} collection. Optionally,
 * you can give a <b>sessionId</b> to group a set of {@link SIP} through one or more submission requests.<br/>
 *
 * After builder creation, you have to fill in the {@link SIP} collection using {@link SIPCollectionBuilder#add(SIP)} or
 * {@link SIPCollectionBuilder#addAll(Collection)}.<br/>
 *
 * To build {@link SIP}, use {@link SIPBuilder}.
 *
 * At the end, get your collection calling {@link SIPCollectionBuilder#build()}.
 *
 * @author Marc Sordi
 *
 */
public class SIPCollectionBuilder {

    private final SIPCollection collection = new SIPCollection();

    public SIPCollectionBuilder(String processingChain, String sessionId) {
        Assert.hasText(processingChain, "Processing chain identifier is required");
        collection.getMetadata().setProcessing(processingChain);
        collection.getMetadata().setSession(sessionId);
    }

    public SIPCollectionBuilder(String processingChain) {
        this(processingChain, null);
    }

    public SIPCollection build() {
        return collection;
    }

    /**
     * Add {@link SIP} to the collection
     * @param sip {@link SIP} to add
     */
    public void add(SIP sip) {
        collection.add(sip);
    }

    /**
     * Add this collection of {@link SIP} to the current collection
     * @param sips
     */
    public void addAll(Collection<SIP> sips) {
        collection.addAll(sips);
    }
}
