package fr.cnes.regards.modules.access.domain;

public class Project {

    private Long id_;

    private String name_;

    private Theme theme_;

    public Project(String name, Theme theme) {
        super();
        name_ = name;
        theme_ = theme;
    }

    public Long getId() {
        return id_;
    }

    public void setId(Long id) {
        id_ = id;
    }

    public String getName() {
        return name_;
    }

    public void setName(String name) {
        name_ = name;
    }

    public Theme getTheme() {
        return theme_;
    }

    public void setTheme(Theme theme) {
        theme_ = theme;
    }

}
