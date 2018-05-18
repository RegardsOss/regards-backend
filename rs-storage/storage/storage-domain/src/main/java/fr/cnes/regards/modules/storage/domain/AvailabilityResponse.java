package fr.cnes.regards.modules.storage.domain;

import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * POJO allowing to know which files are already available and which one could not be requested to be available
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class AvailabilityResponse {

    /**
     * Checksums of files that could not be made available
     */
    private Set<String> errors;

    /**
     * Checksums of files that are already available
     */
    private Set<String> alreadyAvailable;

    /**
     * Default constructor
     */
    public AvailabilityResponse() {
    }

    /**
     * Constructor setting the attributes from the parameters
     * @param errors
     * @param onlineFiles
     * @param nearlineAvailable
     */
    public AvailabilityResponse(Set<String> errors, Set<StorageDataFile> onlineFiles, Set<StorageDataFile> nearlineAvailable) {
        this.errors = errors;
        Set<StorageDataFile> alreadyAvailableData = Sets.newHashSet(onlineFiles);
        alreadyAvailableData.addAll(nearlineAvailable);
        this.alreadyAvailable = alreadyAvailableData.stream().map(df -> df.getChecksum()).collect(Collectors.toSet());
    }

    /**
     * @return the errors
     */
    public Set<String> getErrors() {
        return errors;
    }

    /**
     * Set the errors
     * @param errors
     */
    public void setErrors(Set<String> errors) {
        this.errors = errors;
    }

    /**
     * @return the already available files
     */
    public Set<String> getAlreadyAvailable() {
        return alreadyAvailable;
    }

    /**
     * Set the already available files
     * @param alreadyAvailable
     */
    public void setAlreadyAvailable(Set<String> alreadyAvailable) {
        this.alreadyAvailable = alreadyAvailable;
    }
}
