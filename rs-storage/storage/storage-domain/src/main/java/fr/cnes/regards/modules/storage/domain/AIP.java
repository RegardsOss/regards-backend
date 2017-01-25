/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.io.Serializable;
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

import com.google.common.collect.Lists;

import fr.cnes.regards.modules.storage.urn.OAISIdentifier;
import fr.cnes.regards.modules.storage.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.urn.validator.URN;

/**
 *
 * Archival Information Package representation
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Entity
@Table(name = "t_aip")
// FIXME: url de stockage en base
public class AIP implements Serializable {

    /**
     * Database Id
     */
    private Long id;

    /**
     * SIP ID
     */
    private String sipId;

    /**
     * private Id for the application, it's a {@link UniformResourceName} but due to the need of retrieving all AIP's
     * version(which is in {@link UniformResourceName}) it's mapped to a String, validated as a URN
     */
    @URN(OAISIdentifier.AIP)
    private String ipId;

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
    private AIPState state;

    private String checksum;

    private AIP() {
    }

    public AIP(AipType pType) {
        type = pType;
        tags = new ArrayList<>();
        informationObjects = new ArrayList<>();
    }

    public AIP generateRandomAIP() throws NoSuchAlgorithmException, MalformedURLException {
        sipId = String.valueOf(generateRandomString(new Random(), 40));
        ipId = new UniformResourceName(OAISIdentifier.AIP, AipType.COLLECTION, "tenant", UUID.randomUUID(), 1)
                .toString();
        tags = generateRandomTags();
        informationObjects = generateRandomInformationObjects();
        state = AIPState.VALID;
        submissionDate = LocalDateTime.now();
        lastEvent = new Event("toc");
        checksum = "checksum";
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

    @NotNull
    @Column(name = "sipid")
    public String getSipId() {
        return sipId;
    }

    public void setSipId(String pSipId) {
        sipId = pSipId;
    }

    @NotNull
    @Column(name = "ipid", unique = true, length = 200)
    public String getIpId() {
        return ipId;
    }

    public void setIpId(@URN(OAISIdentifier.AIP) String pIpId) {
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
        return informationObjects.stream().map((InformationObject io) -> {
            String checksum = io.getPdi().getFixityInformation().getChecksum();
            io.getContentInformation().getDataObject().setChecksum(checksum);
            return io.getContentInformation().getDataObject();
        }).collect(Collectors.toList());
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

    public AIP(AIP src) {
        informationObjects = Lists.newArrayList(src.informationObjects);
        ipId = String.valueOf(src.ipId);
        lastEvent = new Event(String.valueOf(src.lastEvent.getComment()), LocalDateTime.from(src.lastEvent.getDate()));
        sipId = String.valueOf(src.sipId);
        state = src.state;
        submissionDate = LocalDateTime.from(src.submissionDate);
        tags = Lists.newArrayList(src.tags);
        type = src.type;
        checksum = String.valueOf(src.checksum);
    }

    @Override
    public boolean equals(Object pOther) {
        return (pOther instanceof AIP) && ipId.equals(((AIP) pOther).ipId) && checksum.equals(((AIP) pOther).checksum);
    }

}
