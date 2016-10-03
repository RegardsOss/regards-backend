/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

import java.util.List;

/**
 * 
 * @author cmertz
 *
 */
public class NavigationContext {

    private Long id_;

    private String tinyUrl_;

    private Project project_;

    private List<ConfigParameter> queryParameters_;

    private String route_;

    private Integer store_;

    public NavigationContext() {
        super();
    }

    public NavigationContext(String pTinyUrl, Project pProject, List<ConfigParameter> pQueryParameters, String pRoute,
            Integer pStore) {
        super();
        this.tinyUrl_ = pTinyUrl;
        this.project_ = pProject;
        this.queryParameters_ = pQueryParameters;
        this.route_ = pRoute;
        this.store_ = pStore;
    }

    public NavigationContext(Project pProject, List<ConfigParameter> pQueryParameters, String pRoute, Integer pStore) {
        super();
        this.project_ = pProject;
        this.queryParameters_ = pQueryParameters;
        this.route_ = pRoute;
        this.store_ = pStore;
    }

    public Long getId() {
        return id_;
    }

    public void setId(Long id) {
        id_ = id;
    }

    public String getTinyUrl() {
        return tinyUrl_;
    }

    public void setTinyUrl(String tinyUrl) {
        this.tinyUrl_ = tinyUrl;
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
        return route_;
    }

    public void setRoute(String route) {
        route_ = route;
    }

    public Integer getStore() {
        return store_;
    }

    public void setStore(Integer store) {
        store_ = store;
    }

}
