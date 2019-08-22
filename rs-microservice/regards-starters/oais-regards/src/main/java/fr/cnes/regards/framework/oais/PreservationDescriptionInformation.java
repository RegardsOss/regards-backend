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
package fr.cnes.regards.framework.oais;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.oais.adapter.InformationPackageMap;

/**
 * OAIS Preservation Description Information object
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 */
public class PreservationDescriptionInformation {

    public static final String CONTEXT_INFO_TAGS_KEY = "tags";

    /**
     * Should contains the tags too as a "special" key.
     *
     * <b>Must contain <code>categories</code> property as a list of string</v>
     */
    @NotEmpty(message = "Context information is required")
    private final InformationPackageMap contextInformation = new InformationPackageMap();

    @NotNull(message = "Reference information is required")
    private final Map<String, String> referenceInformation = new HashMap<>();

    @NotNull(message = "Provenance information is required")
    private final ProvenanceInformation provenanceInformation = new ProvenanceInformation();

    @NotNull(message = "Fixity information is required")
    private final InformationPackageMap fixityInformation = new InformationPackageMap();

    @NotNull(message = "Access right information is required")
    private final AccessRightInformation accessRightInformation = new AccessRightInformation();

    /**
     * @return the reference information
     */
    public Map<String, String> getReferenceInformation() {
        return referenceInformation;
    }

    /**
     * @return the context information
     */
    public Map<String, Object> getContextInformation() {
        return contextInformation;
    }

    /**
     * @return the tags
     */
    public Collection<String> getTags() {
        @SuppressWarnings("unchecked")
        Collection<String> tags = (Collection<String>) getContextInformation().get(CONTEXT_INFO_TAGS_KEY);
        if (tags == null) {
            tags = Sets.newHashSet();
            getContextInformation().put(CONTEXT_INFO_TAGS_KEY, tags);
        }
        return tags;
    }

    /**
     * @return the access right information
     */
    public AccessRightInformation getAccessRightInformation() {
        return accessRightInformation;
    }

    /**
     * @return the provenance information
     */
    public ProvenanceInformation getProvenanceInformation() {
        return provenanceInformation;
    }

    /**
     * @return the fixity information
     */
    public Map<String, Object> getFixityInformation() {
        return fixityInformation;
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

        if (!contextInformation.equals(that.contextInformation)) {
            return false;
        }
        if (!referenceInformation.equals(that.referenceInformation)) {
            return false;
        }
        if (!provenanceInformation.equals(that.provenanceInformation)) {
            return false;
        }
        if (!fixityInformation.equals(that.fixityInformation)) {
            return false;
        }
        return accessRightInformation.equals(that.accessRightInformation);
    }

    @Override
    public int hashCode() {
        int result = contextInformation.hashCode();
        result = 31 * result + referenceInformation.hashCode();
        result = 31 * result + provenanceInformation.hashCode();
        result = 31 * result + fixityInformation.hashCode();
        result = 31 * result + accessRightInformation.hashCode();
        return result;
    }
}
