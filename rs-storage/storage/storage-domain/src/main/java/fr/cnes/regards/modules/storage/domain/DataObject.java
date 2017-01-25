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
 * FIXME: duplicate from dam? or that's the original and dam should depends on storage? if not duplicate, could create a
 * class that extends both this one and DataEntity in dam
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Entity(name = "t_data_object")
public class DataObject implements Serializable {

    @Id
    @SequenceGenerator(name = "DataObjectSequence", initialValue = 1, sequenceName = "seq_data_object")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DataObjectSequence")
    private Long id;

    @Enumerated(EnumType.STRING)
    private FileType type;

    @Column
    private URL url;

    @Column(length = 64)
    private transient String checksum;

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

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String pChecksum) {
        checksum = pChecksum;
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
