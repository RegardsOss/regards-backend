/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

import java.time.LocalDate;

/**
 *
 * @author Christophe Mertz
 *
 */
public class News {

    /**
     *
     */
    private Long id;

    /**
     *
     */
    private String title;

    /**
     *
     */
    private String content;

    /**
     *
     */
    private LocalDate creationDate;

    /**
     *
     */
    private Project project;

    /**
     *
     */
    private NewsType newsType;

    /**
     * Default constructor
     */
    public News() {
        super();
    }

    /**
     * A constructor using fields.
     * 
     * @param pTitle
     *            the title
     * @param pContent
     *            the content description
     * @param pCreationDate
     *            the creation date
     * @param pProject
     *            the project
     * @param pNewsType
     *            a {@link NewsType}
     */
    public News(String pTitle, String pContent, LocalDate pCreationDate, Project pProject, NewsType pNewsType) {
        super();
        this.title = pTitle;
        this.content = pContent;
        this.creationDate = pCreationDate;
        this.project = pProject;
        this.newsType = pNewsType;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String pTitle) {
        title = pTitle;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String pContent) {
        content = pContent;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate pCreationDate) {
        creationDate = pCreationDate;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project pProject) {
        project = pProject;
    }

    public NewsType getNewsType() {
        return newsType;
    }

    public void setNewsType(NewsType pNewsType) {
        newsType = pNewsType;
    }

}
