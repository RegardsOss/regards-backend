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
     * should contains the tags too as a "special" key
     */
    private Map<String, Object> contextInformation;

    @NotNull
    private Map<String, String> referenceInformation;

    @NotNull
    private final ProvenanceInformation provenanceInformation = new ProvenanceInformation();

    @NotNull
    private Map<String, Object> fixityInformation;

    @NotNull
    private final AccessRightInformation accessRightInformation = new AccessRightInformation();

    public Map<String, String> getReferenceInformation() {
        if (referenceInformation == null) {
            referenceInformation = new HashMap<>();
        }
        return referenceInformation;
    }

    public Map<String, Object> getContextInformation() {
        if (contextInformation == null) {
            contextInformation = new HashMap<>();
        }
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
        if (fixityInformation == null) {
            fixityInformation = new HashMap<>();
        }
        return fixityInformation;
    }

    public AccessRightInformation getAccesRightInformation() {
        return accessRightInformation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PreservationDescriptionInformation that = (PreservationDescriptionInformation) o;

        if (contextInformation != null ?
                !contextInformation.equals(that.contextInformation) :
                that.contextInformation != null) {
            return false;
        }
        if (provenanceInformation != null ?
                !provenanceInformation.equals(that.provenanceInformation) :
                that.provenanceInformation != null) {
            return false;
        }
        if (fixityInformation != null ?
                !fixityInformation.equals(that.fixityInformation) :
                that.fixityInformation != null) {
            return false;
        }
        return accessRightInformation != null ?
                accessRightInformation.equals(that.accessRightInformation) :
                that.accessRightInformation == null;
    }

    @Override
    public int hashCode() {
        int result = contextInformation != null ? contextInformation.hashCode() : 0;
        result = 31 * result + (provenanceInformation != null ? provenanceInformation.hashCode() : 0);
        result = 31 * result + (fixityInformation != null ? fixityInformation.hashCode() : 0);
        result = 31 * result + (accessRightInformation != null ? accessRightInformation.hashCode() : 0);
        return result;
    }
}
