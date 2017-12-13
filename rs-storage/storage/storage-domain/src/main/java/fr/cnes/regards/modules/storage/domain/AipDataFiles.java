package fr.cnes.regards.modules.storage.domain;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * Dto associating an {@link AIP} to the public information of its files
 *
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
    private Set<DataFileDto> dataFiles;

    /**
     * Constructor providing the aip and data files to extract the public information
     * @param aip
     * @param dataFiles
     */
    public AipDataFiles(AIP aip, StorageDataFile... dataFiles) {
        this.aip = aip;
        //only set files public information if there is information to set
        if(dataFiles != null && dataFiles.length != 0) {
            this.dataFiles = Arrays.stream(dataFiles).map(DataFileDto::fromDataFile).collect(Collectors.toSet());
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
     * @param aip
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
     * @param dataFiles
     */
    public void setDataFiles(Set<DataFileDto> dataFiles) {
        this.dataFiles = dataFiles;
    }
}
