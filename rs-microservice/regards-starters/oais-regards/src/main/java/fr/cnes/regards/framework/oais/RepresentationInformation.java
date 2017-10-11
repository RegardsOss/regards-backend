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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 *
 * OAIS representation information
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
public class RepresentationInformation {

    @Valid
    private Semantic semantic;

    @NotNull(message = "At least syntax is required in optional representation information")
    @Valid
    private Syntax syntax;

    private EnvironmentDescription environmentDescription;

    public Syntax getSyntax() {
        return syntax;
    }

    public void setSyntax(Syntax syntax) {
        this.syntax = syntax;
    }

    public Semantic getSemantic() {
        return semantic;
    }

    public void setSemantic(Semantic semantic) {
        this.semantic = semantic;
    }

    public EnvironmentDescription getEnvironmentDescription() {
        return environmentDescription;
    }

    public void setEnvironmentDescription(EnvironmentDescription environmentDescription) {
        this.environmentDescription = environmentDescription;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((semantic == null) ? 0 : semantic.hashCode());
        result = (prime * result) + ((syntax == null) ? 0 : syntax.hashCode());
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
        RepresentationInformation other = (RepresentationInformation) obj;
        if (semantic == null) {
            if (other.semantic != null) {
                return false;
            }
        } else if (!semantic.equals(other.semantic)) {
            return false;
        }
        if (syntax == null) {
            if (other.syntax != null) {
                return false;
            }
        } else if (!syntax.equals(other.syntax)) {
            return false;
        }
        return true;
    }

}
