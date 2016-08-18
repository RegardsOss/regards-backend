package fr.cnes.regards.microservices.backend.pojo.datamanagement;

import org.springframework.hateoas.ResourceSupport;

public class Datasource extends ResourceSupport {
    private Long projectId;
    private String name;
    private String description;
    private boolean isPublic;
    private String icon;

    public Datasource(String name) {
        super();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
