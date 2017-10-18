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
package fr.cnes.regards.framework.oais;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.google.common.collect.Sets;

/**
 *
 * OAIS Preservation Description Information object
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
public class PreservationDescriptionInformation {

    public static final String CONTEXT_INFO_TAGS_KEY = "tags";

    /**
     * Should contains the tags too as a "special" key
     */
    @NotNull
    private final Map<String, Object> contextInformation = new HashMap<>();;

    @NotNull
    private final Map<String, String> referenceInformation = new HashMap<>();

    @NotNull
    private final ProvenanceInformation provenanceInformation = new ProvenanceInformation();

    @NotNull
    private final Map<String, Object> fixityInformation = new HashMap<>();

    @NotNull
    private final AccessRightInformation accessRightInformation = new AccessRightInformation();

    public Map<String, String> getReferenceInformation() {
        return referenceInformation;
    }

    public Map<String, Object> getContextInformation() {
        return contextInformation;
    }

    public Collection<String> getTags() {
        @SuppressWarnings("unchecked")
        Collection<String> tags = (Collection<String>) getContextInformation().get(CONTEXT_INFO_TAGS_KEY);
        if (tags == null) {
            tags = Sets.newHashSet();
            getContextInformation().put(CONTEXT_INFO_TAGS_KEY, tags);
        }
        return tags;
    }

    public AccessRightInformation getAccessRightInformation() {
        return accessRightInformation;
    }

    public ProvenanceInformation getProvenanceInformation() {
        return provenanceInformation;
    }

    public Map<String, Object> getFixityInformation() {
        return fixityInformation;
    }

    public AccessRightInformation getAccesRightInformation() {
        return accessRightInformation;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((accessRightInformation == null) ? 0 : accessRightInformation.hashCode());
        result = (prime * result) + ((contextInformation == null) ? 0 : contextInformation.hashCode());
        result = (prime * result) + ((fixityInformation == null) ? 0 : fixityInformation.hashCode());
        result = (prime * result) + ((provenanceInformation == null) ? 0 : provenanceInformation.hashCode());
        result = (prime * result) + ((referenceInformation == null) ? 0 : referenceInformation.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PreservationDescriptionInformation other = (PreservationDescriptionInformation) obj;
        if (accessRightInformation == null) {
            if (other.accessRightInformation != null) {
                return false;
            }
        } else if (!accessRightInformation.equals(other.accessRightInformation)) {
            return false;
        }
        if (contextInformation == null) {
            if (other.contextInformation != null) {
                return false;
            }
        } else if (!contextInformation.equals(other.contextInformation)) {
            return false;
        }
        if (fixityInformation == null) {
            if (other.fixityInformation != null) {
                return false;
            }
        } else if (!fixityInformation.equals(other.fixityInformation)) {
            return false;
        }
        if (provenanceInformation == null) {
            if (other.provenanceInformation != null) {
                return false;
            }
        } else if (!provenanceInformation.equals(other.provenanceInformation)) {
            return false;
        }
        if (referenceInformation == null) {
            if (other.referenceInformation != null) {
                return false;
            }
        } else if (!referenceInformation.equals(other.referenceInformation)) {
            return false;
        }
        return true;
    }
}
