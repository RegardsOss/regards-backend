/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;

@Entity
public class AIP {

    @Column
    private UniformResourceName sipId;

    @Column
    private UniformResourceName ipId;

    @Enumerated(EnumType.STRING)
    private AipType type;

    @Transient
    private List<String> tags;

    @Transient
    private List<InformationObject> informationObjects;

    private AIPState state;

    public AIP(AipType pType) {
        tags = new ArrayList<>();
        informationObjects = new ArrayList<>();
    }

    public AIP generateAIP() throws NoSuchAlgorithmException {
        sipId = (new UniformResourceName()).generateUnifiedResourceName();
        ipId = (new UniformResourceName()).generateUnifiedResourceName();
        tags = generateRandomTags();
        informationObjects = generateRandomInformationObjects();
        return this;
    }

    private List<InformationObject> generateRandomInformationObjects() throws NoSuchAlgorithmException {
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

    public UniformResourceName getSipId() {
        return sipId;
    }

    public void setSipId(UniformResourceName pSipId) {
        sipId = pSipId;
    }

    public UniformResourceName getIpId() {
        return ipId;
    }

    public void setIpId(UniformResourceName pIpId) {
        ipId = pIpId;
    }

    public AipType getType() {
        return type;
    }

    public void setType(AipType pType) {
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

    public AIPState getState() {
        return state;
    }

    public void setState(AIPState pState) {
        state = pState;
    }

}
