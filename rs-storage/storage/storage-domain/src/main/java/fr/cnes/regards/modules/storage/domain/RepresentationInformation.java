/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

public class RepresentationInformation implements Serializable {

    private Semantic semantic;

    @NotNull
    private Syntax syntax;

    public RepresentationInformation() {

    }

    public Syntax getSyntax() {
        return syntax;
    }

    public void setSyntax(Syntax pSyntax) {
        syntax = pSyntax;
    }

    public Semantic getSemantic() {
        return semantic;
    }

    public void setSemantic(Semantic pSemantic) {
        semantic = pSemantic;
    }

    public RepresentationInformation generate() {
        semantic = new Semantic().generate();
        syntax = new Syntax().generate();
        return this;
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
        } else
            if (!semantic.equals(other.semantic)) {
                return false;
            }
        if (syntax == null) {
            if (other.syntax != null) {
                return false;
            }
        } else
            if (!syntax.equals(other.syntax)) {
                return false;
            }
        return true;
    }

}
