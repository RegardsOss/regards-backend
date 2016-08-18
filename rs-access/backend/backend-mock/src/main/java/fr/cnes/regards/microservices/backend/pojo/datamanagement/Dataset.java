package fr.cnes.regards.microservices.backend.pojo.datamanagement;

import fr.cnes.regards.microservices.backend.pojo.administration.ProjectUser;
import org.springframework.hateoas.ResourceSupport;

import java.util.List;

public class Dataset extends ResourceSupport {
    private Long datasetId;


    // Entity
    private long creationDate;
    private long lastModificationDate;
    private String sip_id;

    // Collection
    private String name;
    private String description;

    // Dataset
    private String licence;
    private List<String> quotations;
    private int score;
    private List<ProjectUser> subscribers;

    public Long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public long getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(long lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    public String getSip_id() {
        return sip_id;
    }

    public void setSip_id(String sip_id) {
        this.sip_id = sip_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLicence() {
        return licence;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    public List<String> getQuotations() {
        return quotations;
    }

    public void setQuotations(List<String> quotations) {
        this.quotations = quotations;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<ProjectUser> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(List<ProjectUser> subscribers) {
        this.subscribers = subscribers;
    }
}
