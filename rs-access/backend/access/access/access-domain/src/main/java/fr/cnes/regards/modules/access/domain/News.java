package fr.cnes.regards.modules.access.domain;

import java.util.Date;

/**
 *
 * @author christophe
 *
 */
public class News {

    /**
     *
     */
    private Long id_;

    /**
     *
     */
    private String title_;

    /**
     *
     */
    private String content_;

    /**
     *
     */
    private Date creationDate_;

    /**
     *
     */
    private Project project_;

    /**
     *
     */
    private NewsType newsType_;

    public Long getId() {
        return id_;
    }

    public void setId(Long pId) {
        id_ = pId;
    }

    public String getTitle() {
        return title_;
    }

    public void setTitle(String pTitle) {
        title_ = pTitle;
    }

    public String getContent() {
        return content_;
    }

    public void setContent(String pContent) {
        content_ = pContent;
    }

    public Date getCreationDate() {
        return creationDate_;
    }

    public void setCreationDate(Date pCreationDate) {
        creationDate_ = pCreationDate;
    }

    public Project getProject() {
        return project_;
    }

    public void setProject(Project pProject) {
        project_ = pProject;
    }

    public NewsType getNewsType() {
        return newsType_;
    }

    public void setNewsType(NewsType pNewsType) {
        newsType_ = pNewsType;
    }

}
