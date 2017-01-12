/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

/**
 * FIXME: duplicate from dam? or that's the original and dam should depends on storage? if not duplicate, could create a
 * class that extends both this one and DataEntity in dam
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class DataObject {

    private FileType type;

    private UniformResourceName urn;

    public DataObject() {

    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType pType) {
        type = pType;
    }

    public UniformResourceName getUri() {
        return urn;
    }

    public void setUri(UniformResourceName pUri) {
        urn = pUri;
    }

    public DataObject generate() {
        type = FileType.OTHER;
        urn = new UniformResourceName();
        return this;
    }

}
