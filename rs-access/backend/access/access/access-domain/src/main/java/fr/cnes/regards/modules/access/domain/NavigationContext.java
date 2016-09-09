package fr.cnes.regards.modules.access.domain;

import java.util.List;

public class NavigationContext {

    private Long id_;

    private Project project_;

    private List<ConfigParameter> queryParameters_;

    private Integer route_;

    private Integer store_;


    public Long getId() {
        return id_;
    }


    public void setId(Long id) {
        id_ = id;
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


    public Integer getRoute() {
        return route_;
    }


    public void setRoute(Integer route) {
        route_ = route;
    }


    public Integer getStore() {
        return store_;
    }


    public void setStore(Integer store) {
        store_ = store;
    }

}
