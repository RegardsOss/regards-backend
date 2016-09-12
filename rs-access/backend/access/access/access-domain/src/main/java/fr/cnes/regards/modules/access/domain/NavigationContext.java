package fr.cnes.regards.modules.access.domain;

import java.util.List;

public class NavigationContext {

    private Long id_;

    private String tinyUrl;

    private Project project_;

    private List<ConfigParameter> queryParameters_;

    private String url_;

    private Integer store_;

    public NavigationContext(Project pProject, List<ConfigParameter> pQueryParameters, String pUrl, Integer pStore) {
        super();
        project_ = pProject;
        queryParameters_ = pQueryParameters;
        url_ = pUrl;
        store_ = pStore;
    }

    public Long getId() {
        return id_;
    }

    public void setId(Long id) {
        id_ = id;
    }

    public String getTinyUrl() {
        return tinyUrl;
    }

    public void setTinyUrl(String tinyUrl) {
        this.tinyUrl = tinyUrl;
    }

    public Project getProject() {
        return project_;
    }

    public void setProject(Project project) {
        project_ = project;
    }

    public List<ConfigParameter> getQueryParameters() {
        return queryParameters_;
    }

    public void setQueryParameters(List<ConfigParameter> queryParameters) {
        queryParameters_ = queryParameters;
    }

    public String getRoute() {
        return url_;
    }

    public void setRoute(String route) {
        url_ = route;
    }

    public Integer getStore() {
        return store_;
    }

    public void setStore(Integer store) {
        store_ = store;
    }

}
