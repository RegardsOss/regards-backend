/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;

/**
 *
 * Archival Information Package representation
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class AIP implements Serializable {

    /**
     * SIP ID
     */
    private String sipId;

    /**
     * private Id for the application, it's a {@link UniformResourceName} but due to the need of retrieving all AIP's
     * version(which is in {@link UniformResourceName}) it's mapped to a String, validated as a URN
     */
    private String ipId;

    /**
     * Type of this AIP
     */
    private EntityType type;

    /**
     * Tag list
     */
    private List<String> tags;

    /**
     * List of Information Object
     */
    @NotNull
    private List<InformationObject> informationObjects;

    private List<Event> history;

    /**
     * State determined through different storage steps
     */
    @GsonIgnore
    private AIPState state;

    private AIP() {
    }

    public AIP(EntityType pType) {
        type = pType;
        tags = new ArrayList<>();
        informationObjects = new ArrayList<>();
    }

    public AIP generateRandomAIP() throws NoSuchAlgorithmException, MalformedURLException {
        sipId = String.valueOf(generateRandomString(new Random(), 40));
        ipId = new UniformResourceName(OAISIdentifier.AIP, EntityType.COLLECTION, "tenant", UUID.randomUUID(), 1)
                .toString();
        tags = generateRandomTags();
        informationObjects = generateRandomInformationObjects();
        history = Lists.newArrayList(new Event("addition of this aip into our beautiful system!", OffsetDateTime.now(),
                                               EventType.SUBMISSION));
        return this;
    }

    private List<InformationObject> generateRandomInformationObjects()
            throws NoSuchAlgorithmException, MalformedURLException {
        int listMaxSize = 5;
        Random random = new Random();
        int listSize = random.nextInt(listMaxSize) + 1;
        List<InformationObject> informationObjects = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            informationObjects.add((new InformationObject()).generateRandomInformationObject());
        }
        return informationObjects;
    }

    private List<String> generateRandomTags() {
        int listMaxSize = 15;
        int tagMaxSize = 10;
        Random random = new Random();
        int listSize = random.nextInt(listMaxSize);
        List<String> tags = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            char[] tag = generateRandomString(random, tagMaxSize);
            tags.add(String.valueOf(tag));
        }
        return tags;
    }

    /**
     * @return
     */
    private char[] generateRandomString(Random random, int maxStringLength) {
        String possibleLetters = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWYXZ";
        int tagSize = random.nextInt(maxStringLength);
        char[] tag = new char[tagSize];
        for (int j = 0; j < tagSize; j++) {
            tag[j] = possibleLetters.charAt(random.nextInt(possibleLetters.length()));
        }
        return tag;
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

    public void setSipId(String pSipId) {
        sipId = pSipId;
    }

    public String getIpId() {
        return ipId;
    }

    public void setIpId(String pIpId) {
        ipId = pIpId;
    }

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType pType) {
        type = pType;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> pTags) {
        tags = pTags;
    }

    public List<InformationObject> getInformationObjects() {
        return informationObjects;
    }

    public void setInformationObjects(List<InformationObject> pInformationObjects) {
        informationObjects = pInformationObjects;
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
        //then we get the one we want, the latest of the latest
        return latestEvents.stream().filter(Optional::isPresent).map(Optional::get)
                .sorted(Comparator.comparing(Event::getDate).reversed()).findFirst().orElse(null);
    }

    public Event getSubmissionEvent() {
        return getHistory().stream().filter(e -> e.getType().equals(EventType.SUBMISSION)).findFirst().orElse(null);
    }

    public void addEvent(Event event) {
        getHistory().add(event);
    }
}
