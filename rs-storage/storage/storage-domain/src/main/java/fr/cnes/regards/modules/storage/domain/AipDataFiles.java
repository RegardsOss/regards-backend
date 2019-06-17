package fr.cnes.regards.modules.storage.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * Dto associating an {@link AIP} to the public information of its files
 * @author Sylvain VISSIERE-GUERINET
 */
public class AipDataFiles {

    /**
     * The aip
     */
    private AIP aip;

    /**
     * its file public information
     */
    private Set<DataFileDto> dataFiles = new HashSet<>();

    /**
     * Default constructor
     */
    public AipDataFiles() {
    }

    /**
     * Constructor providing the aip and data files to extract the public information
     */
    public AipDataFiles(AIP aip, Collection<StorageDataFile> dataFiles) {
        this.aip = aip;
        // only set files public information if there is information to set
        if ((dataFiles != null) && (dataFiles.size() != 0)) {
            this.dataFiles.addAll(dataFiles.stream().map(DataFileDto::fromDataFile).collect(Collectors.toSet()));
        }
    }

    /**
     * @return the aip
     */
    public AIP getAip() {
        return aip;
    }

    /**
     * Set the aip
     */
    public void setAip(AIP aip) {
        this.aip = aip;
    }

    /**
     * @return the files public information
     */
    public Set<DataFileDto> getDataFiles() {
        return dataFiles;
    }

    /**
     * Set the files public information
     */
    public void setDataFiles(Set<DataFileDto> dataFiles) {
        this.dataFiles = dataFiles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        AipDataFiles that = (AipDataFiles) o;
        return Objects.equals(aip, that.aip);
    }

    @Override
    public int hashCode() {

        return Objects.hash(aip);
    }
}
