/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * 
 * @author Christophe Mertz
 *
 */
@Entity
@Table(name = "T_NAVCTX_PROJECT")
@SequenceGenerator(name = "navCtxProjectSequence", initialValue = 1, sequenceName = "SEQ_NAVCTX_PROJECT")
public class Project implements IIdentifiable<Long> {

    /**
     * Unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "navCtxProjectSequence")
    private Long id;

    /**
     * The name of the project
     */
    private String name;

    /**
     * A {@link Theme} to use for this project
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "theme_id", referencedColumnName = "id",
            foreignKey = @javax.persistence.ForeignKey(name = "FK_THEME_ID"))
    private Theme theme;

    /**
     * Default constructor
     */
    public Project() {
        super();
    }

    /**
     * A constructor with
     * 
     * @param pName
     *            a project name
     * @param pTheme
     *            a {@link Theme}
     */
    public Project(String pName, Theme pTheme) {
        super();
        name = pName;
        theme = pTheme;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme pTheme) {
        theme = pTheme;
    }

}
