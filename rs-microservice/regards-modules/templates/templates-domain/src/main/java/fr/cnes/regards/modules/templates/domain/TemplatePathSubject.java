package fr.cnes.regards.modules.templates.domain;

/**
 * Convenient bean to store both template path and subject
 * @author Sylvain VISSIERE-GUERINET
 */
public class TemplatePathSubject {

    /**
     * The template path
     */
    private String templatePath;

    /**
     * The email subject
     */
    private String emailSubject;

    /**
     * Constructor setting the parameters as attribute
     */
    public TemplatePathSubject(String templatePath, String emailSubject) {
        this.templatePath = templatePath;
        this.emailSubject = emailSubject;
    }

    /**
     * @return the template path
     */
    public String getTemplatePath() {
        return templatePath;
    }

    /**
     * Set the template path
     */
    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    /**
     * @return the email subject
     */
    public String getEmailSubject() {
        return emailSubject;
    }

    /**
     * Set the subject of the email
     */
    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }
}
