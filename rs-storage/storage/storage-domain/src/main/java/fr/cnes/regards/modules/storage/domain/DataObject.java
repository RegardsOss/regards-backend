/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

/**
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class DataObject implements Serializable {

    private FileType type;

    private URL url;


    public DataObject() {

    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType pType) {
        type = pType;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL pUri) {
        url = pUri;
    }

    public DataObject generate() throws MalformedURLException {
        type = FileType.OTHER;
        url = new URL("ftp://bla");
        return this;
    }

    @Override
    public boolean equals(Object pOther) {
        return (pOther instanceof DataObject) && type.equals(((DataObject) pOther).type)
                && url.toString().equals(((DataObject) pOther).url.toString());
    }

    @Override
    public int hashCode() {
        return url.toString().hashCode();
    }

}
