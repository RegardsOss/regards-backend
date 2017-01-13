/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

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
public class DataObject {

    @Id
    @SequenceGenerator(name = "DataObjectSequence", initialValue = 1, sequenceName = "seq_data_object")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DataObjectSequence")
    private Long id;

    @Enumerated(EnumType.STRING)
    private FileType type;

    @Column(length = 32)
    private String checksum;

    @Column
    private URL url;

    public DataObject() {

    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType pType) {
        type = pType;
    }

    public URL getUri() {
        return url;
    }

    public void setUri(URL pUri) {
        url = pUri;
    }

    public DataObject generate() throws MalformedURLException {
        type = FileType.OTHER;
        url = new URL("ftp://bla");
        return this;
    }

}
