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

import fr.cnes.regards.modules.storage.domain.AIPState;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.NotNull;

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
     * Regards session id
     */
    @NotNull
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
}
