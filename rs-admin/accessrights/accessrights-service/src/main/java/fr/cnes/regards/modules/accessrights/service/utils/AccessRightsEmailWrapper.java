package fr.cnes.regards.modules.accessrights.service.utils;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AccessRightsEmailWrapper {

    private ProjectUser projectUser;
    private String subject;
    private String from;
    private Set<String> to;
    private String template;
    private Map<String, Object> data = new HashMap<>();
    private String defaultMessage;

    public ProjectUser getProjectUser() {
        return projectUser;
    }

    public AccessRightsEmailWrapper setProjectUser(ProjectUser projectUser) {
        this.projectUser = projectUser;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public AccessRightsEmailWrapper setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public AccessRightsEmailWrapper setFrom(String from) {
        this.from = from;
        return this;
    }

    public Set<String> getTo() {
        return to;
    }

    public AccessRightsEmailWrapper setTo(Set<String> to) {
        this.to = to;
        return this;
    }

    public String getTemplate() {
        return template;
    }

    public AccessRightsEmailWrapper setTemplate(String template) {
        this.template = template;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public AccessRightsEmailWrapper setData(Map<String, Object> data) {
        this.data = data;
        return this;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public AccessRightsEmailWrapper setDefaultMessage(String defaultMessage) {
        this.defaultMessage = defaultMessage;
        return this;
    }

}
