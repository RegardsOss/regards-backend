package fr.cnes.regards.modules.storage.domain.database;

import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class AvailabilityResponse {

    private Set<String> errors;

    private Set<String> alreadyAvailable;

    public AvailabilityResponse() {
    }

    public AvailabilityResponse(Set<String> errors, Set<DataFile> onlineFiles, Set<DataFile> nearlineAvailable) {
        this.errors = errors;
        Set<DataFile> alreadyAvailableData = Sets.newHashSet(onlineFiles);
        alreadyAvailableData.addAll(nearlineAvailable);
        this.alreadyAvailable = alreadyAvailableData.stream().map(df -> df.getChecksum()).collect(Collectors.toSet());
    }

    public Set<String> getErrors() {
        return errors;
    }

    public void setErrors(Set<String> errors) {
        this.errors = errors;
    }

    public Set<String> getAlreadyAvailable() {
        return alreadyAvailable;
    }

    public void setAlreadyAvailable(Set<String> alreadyAvailable) {
        this.alreadyAvailable = alreadyAvailable;
    }
}
