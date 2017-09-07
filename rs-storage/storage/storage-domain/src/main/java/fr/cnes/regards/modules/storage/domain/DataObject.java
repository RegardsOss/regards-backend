/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import fr.cnes.regards.framework.urn.DataType;

/**
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class DataObject implements Serializable {

    @NotNull
    private DataType type;

    @NotNull
    private URL url;

    public DataObject() {
    }

    public DataObject(DataType type, URL url) {
        this.type = type;
        this.url = url;
    }

    public DataType getType() {
        return type;
    }

    public void setType(DataType pType) {
        type = pType;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL pUri) {
        url = pUri;
    }

    public DataObject generate() throws MalformedURLException {
        type = DataType.OTHER;
        url = new URL("ftp://bla");
        return this;
    }

    @Override
    public boolean equals(Object pOther) {
        return (pOther instanceof DataObject) && type.equals(((DataObject) pOther).type) && url.toString()
                .equals(((DataObject) pOther).url.toString());
    }

    @Override
    public int hashCode() {
        return url.toString().hashCode();
    }

}
