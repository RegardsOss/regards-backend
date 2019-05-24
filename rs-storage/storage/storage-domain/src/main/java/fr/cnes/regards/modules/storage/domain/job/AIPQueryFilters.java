/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain.job;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.cnes.regards.modules.storage.domain.AIPState;

/**
 * This object allows several query filters to be passed to a POST endpoint or to Job.
 * @author Léo Mieulet
 */
public class AIPQueryFilters {

    /**
     *  state of the AIP (optional)
     */
    private AIPState state;

    /**
     * AIP provider id
     */
    private String providerId;

    /**
     * (optional)
     */
    private OffsetDateTime from;

    /**
     * (optional)
     */
    private OffsetDateTime to;

    /**
     * list of API id included (optional)
     */
    private Set<String> aipIds = new HashSet<>();

    /**
     * list of API id excluded (optional)
     */
    private Set<String> aipIdsExcluded = new HashSet<>();

    /**
     * list of tags already set up on entities (optional)
     */
    private List<String> tags = new ArrayList<>();

    /**
     * Data storage ids on which filtered aip should be stored on
     */
    private Set<Long> storedOn = new HashSet<>();

    /**
     * Regards session id
     */
    private String session;

    public AIPQueryFilters() {
    }

    public AIPState getState() {
        return state;
    }

    public OffsetDateTime getFrom() {
        return from;
    }

    public OffsetDateTime getTo() {
        return to;
    }

    public Set<String> getAipIds() {
        return aipIds;
    }

    public Set<String> getAipIdsExcluded() {
        return aipIdsExcluded;
    }

    public String getSession() {
        return session;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setState(AIPState state) {
        this.state = state;
    }

    public void setFrom(OffsetDateTime from) {
        this.from = from;
    }

    public void setTo(OffsetDateTime to) {
        this.to = to;
    }

    public void setAipIds(Set<String> aipIds) {
        this.aipIds = aipIds;
    }

    public void setAipIdsExcluded(Set<String> aipIdsExcluded) {
        this.aipIdsExcluded = aipIdsExcluded;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Set<Long> getStoredOn() {
        return storedOn;
    }

    public void setStoredOn(Set<Long> storedOn) {
        this.storedOn = storedOn;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Filters: [" + (state != null ? "state=" + state + ", " : "")
                + (providerId != null ? "providerId=" + providerId + ", " : "")
                + (from != null ? "from=" + from + ", " : "") + (to != null ? "to=" + to + ", " : "")
                + (aipIds != null ? "aipIds=" + aipIds + ", " : "")
                + (aipIdsExcluded != null ? "aipIdsExcluded=" + aipIdsExcluded + ", " : "")
                + (tags != null ? "tags=" + tags + ", " : "") + (storedOn != null ? "storedOn=" + storedOn + ", " : "")
                + (session != null ? "session=" + session : "") + "]";
    }

}
