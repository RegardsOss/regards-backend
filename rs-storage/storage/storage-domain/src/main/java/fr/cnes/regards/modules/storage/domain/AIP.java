/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.hibernate.validator.constraints.NotBlank;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.oais.AbstractInformationPackage;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.InformationObject;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.oais.urn.validator.RegardsOaisUrnAsString;

/**
 *
 * Archival Information Package representation
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
public class AIP extends AbstractInformationPackage {

    /**
     * SIP ID
     */
    private String sipId;

    /**
     * private Id for the application, it's a {@link UniformResourceName} but due to the need of retrieving all AIP's
     * version(which is in {@link UniformResourceName}) it's mapped to a String, validated as a URN
     */
    @NotBlank
    @RegardsOaisUrnAsString
    private String ipId;

    private List<Event> history;

    /**
     * State determined through different storage steps
     */
    @GsonIgnore
    private AIPState state;

    public AIP() {
        super();
    }

    public AIPState getState() {
        return state;
    }

    public void setState(AIPState state) {
        this.state = state;
    }

    public String getSipId() {
        return sipId;
    }

    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    public String getIpId() {
        return ipId;
    }

    public void setIpId(String ipId) {
        this.ipId = ipId;
    }

    public List<Event> getHistory() {
        if (history == null) {
            history = Lists.newArrayList();
        }
        return history;
    }

    public void setHistory(List<Event> history) {
        this.history = history;
    }

    @Override
    public boolean equals(Object pOther) {
        return (pOther instanceof AIP) && ipId.equals(((AIP) pOther).ipId);
    }

    /**
     * Abstraction on where the last event is and how to get it
     * @return last event occurred to this aip
     */
    public Event getLastEvent() {
        Set<Optional<Event>> latestEvents = Sets.newHashSet();
        // first lets get all the latest events, Optional in case there is no event for one of them: highly improbable
        for (InformationObject io : informationObjects) {
            latestEvents.add(io.getPdi().getProvenanceInformation().getHistory().stream()
                    .sorted(Comparator.comparing(Event::getDate).reversed()).findFirst());
        }
        latestEvents.add(getHistory().stream().sorted(Comparator.comparing(Event::getDate).reversed()).findFirst());
        // then we get the one we want, the latest of the latest
        return latestEvents.stream().filter(Optional::isPresent).map(Optional::get)
                .sorted(Comparator.comparing(Event::getDate).reversed()).findFirst().orElse(null);
    }

    public Event getSubmissionEvent() {
        return getHistory().stream().filter(e -> e.getType().equals(EventType.SUBMISSION.name())).findFirst()
                .orElse(null);
    }

    public void addEvent(@Nullable String type, String comment, OffsetDateTime date) {
        Event event = new Event();
        event.setType(type);
        event.setComment(comment);
        event.setDate(date);
        getHistory().add(event);
    }

    public void addEvent(@Nullable String type, String comment) {
        addEvent(type, comment, OffsetDateTime.now());
    }
}
