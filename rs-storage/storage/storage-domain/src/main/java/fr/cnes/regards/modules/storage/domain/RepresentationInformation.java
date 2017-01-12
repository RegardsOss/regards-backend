package fr.cnes.regards.modules.storage.domain;

public class RepresentationInformation {

    private Semantic semantic;

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
        this.semantic = new Semantic().generate();
        this.syntax = new Syntax().generate();
        return this;
    }

}
