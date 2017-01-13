/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import fr.cnes.regards.modules.storage.urn.OAISIdentifier;
import fr.cnes.regards.modules.storage.urn.UniformResourceName;

/**
 *
 * Archival Information Package representation
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Entity(name = "t_aip")
public class AIP {

    /**
     * Database Id
     */
    private Long id;

    private String checksum;

    // FIXME: constraint the column
    private UniformResourceName sipId;

    /**
     * private Id for the application
     */
    // FIXME: constraint the column
    private UniformResourceName ipId;

    private Event lastEvent;

    private AipType type;

    private List<String> tags;

    private List<InformationObject> informationObjects;

    private AIPState state;

    public AIP(AipType pType) {
        type = pType;
        tags = new ArrayList<>();
        informationObjects = new ArrayList<>();
    }

    public AIP generateAIP() throws NoSuchAlgorithmException, MalformedURLException {
        sipId = new UniformResourceName(OAISIdentifier.SIP, AipType.COLLECTION, "tenant", UUID.randomUUID(), 1);
        ipId = new UniformResourceName(OAISIdentifier.SIP, AipType.COLLECTION, "tenant", UUID.randomUUID(), 1);
        tags = generateRandomTags();
        informationObjects = generateRandomInformationObjects();
        checksum = "checksum";
        state = AIPState.VALID;
        return this;
    }

    private List<InformationObject> generateRandomInformationObjects()
            throws NoSuchAlgorithmException, MalformedURLException {
        int listMaxSize = 5;
        Random random = new Random();
        int listSize = random.nextInt(listMaxSize);
        List<InformationObject> informationObjects = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            informationObjects.add((new InformationObject()).generateRandomInformationObject());
        }
        return informationObjects;
    }

    private List<String> generateRandomTags() {
        String possibleLetters = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWYXZ";
        int listMaxSize = 15;
        int tagMaxSize = 10;
        Random random = new Random();
        int listSize = random.nextInt(listMaxSize);
        List<String> tags = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            int tagSize = random.nextInt(tagMaxSize);
            char[] tag = new char[tagSize];
            for (int j = 0; j < tagSize; j++) {
                tag[j] = possibleLetters.charAt(random.nextInt(possibleLetters.length()));
            }
            tags.add(String.valueOf(tag));
        }
        return tags;
    }

    @NotNull
    @Column(name = "sipid")
    @Convert(converter = fr.cnes.regards.modules.storage.urn.converters.UrnConverter.class)
    public UniformResourceName getSipId() {
        return sipId;
    }

    public void setSipId(UniformResourceName pSipId) {
        sipId = pSipId;
    }

    @NotNull
    @Column(name = "ipid")
    @Convert(converter = fr.cnes.regards.modules.storage.urn.converters.UrnConverter.class)
    public UniformResourceName getIpId() {
        return ipId;
    }

    public void setIpId(UniformResourceName pIpId) {
        ipId = pIpId;
    }

    @Column(length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    public AipType getType() {
        return type;
    }

    public void setType(AipType pType) {
        type = pType;
    }

    @ElementCollection
    @CollectionTable(name = "ta_aip_tag", joinColumns = @JoinColumn(name = "aip_id"),
            foreignKey = @ForeignKey(name = "fk_aip_tag"))
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> pTags) {
        tags = pTags;
    }

    @Transient
    public List<InformationObject> getInformationObjects() {
        return informationObjects;
    }

    public void setInformationObjects(List<InformationObject> pInformationObjects) {
        informationObjects = pInformationObjects;
    }

    @Column(length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    public AIPState getState() {
        return state;
    }

    public void setState(AIPState pState) {
        state = pState;
    }

    @Id
    @SequenceGenerator(name = "AipSequence", initialValue = 1, sequenceName = "seq_aip")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AipSequence")
    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

    @NotNull
    @Column(length = 32)
    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String pChecksum) {
        checksum = pChecksum;
    }

    @Embedded
    public Event getLastEvent() {
        return lastEvent;
    }

    public void setLastEvent(Event pLastEvent) {
        lastEvent = pLastEvent;
    }

    @OneToMany
    @JoinColumn(name = "aip_id", foreignKey = @ForeignKey(name = "fk_aip_data_objects"))
    @Column
    public List<DataObject> getDataObjects() {
        return informationObjects.stream().map(io -> io.getContentInformation().getDataObject())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    private void setDataObjects(List<DataObject> pDataObjects) { // NOSONAR

    }

}
