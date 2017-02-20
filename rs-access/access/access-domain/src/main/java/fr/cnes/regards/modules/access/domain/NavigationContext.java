/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * 
 * @author Christophe Mertz
 *
 */
@Entity
@Table(name = "T_NAVIGATION_CONTEXT")
@SequenceGenerator(name = "navCtxSequence", initialValue = 1, sequenceName = "SEQ_NAV_CTX")
public class NavigationContext implements IIdentifiable<Long> {

    /**
     * {@link String} min size
     */
    public static final int STR_MIN_SIZE = 8;

    /**
     * {@link String} max size
     */
    public static final int STR_MAX_SIZE = 1024;

    /**
     * Unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "navCtxSequence")
    private Long id;

    /**
     * 
     */
    @NotNull
    @Size(min = STR_MIN_SIZE, max = STR_MAX_SIZE,
            message = "Tiny URL must be between " + STR_MIN_SIZE + " and " + STR_MAX_SIZE + " length.")
    @Column(nullable = false, updatable = true, unique = true, length = STR_MAX_SIZE)
    private String tinyUrl;

    /**
     * 
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", referencedColumnName = "id",
            foreignKey = @javax.persistence.ForeignKey(name = "FK_PROJECT_ID"))
    private Project project;

    /**
     * A list of {@link ConfigParameter}
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ConfigParameter> queryParameters;

    /**
     * 
     */
    @NotNull
    @Size(min = STR_MIN_SIZE, max = STR_MAX_SIZE,
            message = "Route must be between " + STR_MIN_SIZE + " and " + STR_MAX_SIZE + " length.")
    @Column(nullable = false, updatable = true, unique = true, length = STR_MAX_SIZE)
    private String route;

    /**
     * 
     */
    @NotNull
    @Column(nullable = false, updatable = true, unique = false)
    private Integer store;

    /**
     * Default constructor
     */
    public NavigationContext() {
        super();
        tinyUrl = "";
        route = "";
        store = 0;
    }

    /**
     * 
     * @param pTinyUrl
     *            a tiny URL
     * @param pProject
     *            the project
     * @param pQueryParameters
     *            a list of {@link ConfigParameter}
     * @param pRoute
     *            the route
     * @param pStore
     *            the Store}
     */
    public NavigationContext(String pTinyUrl, Project pProject, List<ConfigParameter> pQueryParameters, String pRoute,
            Integer pStore) {
        super();
        tinyUrl = pTinyUrl;
        project = pProject;
        queryParameters = pQueryParameters;
        route = pRoute;
        store = pStore;
    }

    /**
     * 
     * @param pProject
     *            the project
     * @param pQueryParameters
     *            a list of {@link ConfigParameter}
     * @param pRoute
     *            the route
     * @param pStore
     *            the store
     */
    public NavigationContext(Project pProject, List<ConfigParameter> pQueryParameters, String pRoute, Integer pStore) {
        super();
        project = pProject;
        queryParameters = pQueryParameters;
        route = pRoute;
        store = pStore;
        tinyUrl = "";
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        this.id = pId;
    }

    public String getTinyUrl() {
        return tinyUrl;
    }

    public void setTinyUrl(String pTinyUrl) {
        this.tinyUrl = pTinyUrl;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project pProject) {
        project = pProject;
    }

    public List<ConfigParameter> getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(List<ConfigParameter> pQueryParameters) {
        queryParameters = pQueryParameters;
    }

    public void addQueryParameters(ConfigParameter pQueryParameters) {
        if (queryParameters == null) {
            queryParameters = new ArrayList<>();
        }
        queryParameters.add(pQueryParameters);
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String pRoute) {
        route = pRoute;
    }

    public Integer getStore() {
        return store;
    }

    public void setStore(Integer pStore) {
        store = pStore;
    }

}
