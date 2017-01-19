/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
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
@Entity
@Table(name = "t_aip")
public class AIP {

    /**
     * Database Id
     */
    private Long id;

    /**
     * checksum of the AIP
     */
    private transient String checksum;

    /**
     * SIP ID
     */
    private String sipId;

    /**
     * private Id for the application
     */
    private UniformResourceName ipId;

    /**
     * Last Event that affected this AIP
     */
    private Event lastEvent;

    /**
     * Submission Date into REGARDS
     */
    private LocalDateTime submissionDate;

    /**
     * Type of this AIP
     */
    private AipType type;

    /**
     * List of tag
     */
    private List<String> tags;

    /**
     * List of Information Object
     */
    private List<InformationObject> informationObjects;

    /**
     * State of this AIP
     */
    private transient AIPState state;

    private AIP() {
    }

    public AIP(AipType pType) {
        type = pType;
        tags = new ArrayList<>();
        informationObjects = new ArrayList<>();
    }

    public AIP generateAIP() throws NoSuchAlgorithmException, MalformedURLException {
        sipId = String.valueOf(generateRandomString(new Random(), 40));
        ipId = new UniformResourceName(OAISIdentifier.SIP, AipType.COLLECTION, "tenant", UUID.randomUUID(), 1);
        tags = generateRandomTags();
        informationObjects = generateRandomInformationObjects();
        checksum = "checksum";
        state = AIPState.VALID;
        submissionDate = LocalDateTime.now();
        lastEvent = new Event("toc");
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

    @NotNull
    @Column(name = "sipid")
    public String getSipId() {
        return sipId;
    }

    public void setSipId(String pSipId) {
        sipId = pSipId;
    }

    @NotNull
    @Column(name = "ipid", unique = true)
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
    @CollectionTable(name = "t_aip_tag")
    @Column(name = "value", length = 200)
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

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "aip_id", foreignKey = @ForeignKey(name = "fk_aip_data_objects"))
    @Column
    public List<DataObject> getDataObjects() {
        return informationObjects.stream().map(io -> io.getContentInformation().getDataObject())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    private void setDataObjects(List<DataObject> pDataObjects) { // NOSONAR

    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDateTime pSubmissionDate) {
        submissionDate = pSubmissionDate;
    }

}
