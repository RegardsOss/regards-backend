import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * TODO Description
 *
 * @author TODO
 */
@Entity
@Table(name = "t_greeting")
public class Greeting {

    /**
     * Id is datasource id (see table pluginConf)
     */
    @Id
    @Column(name = "ds_id")
    private Long id;

    @Column(name = "t_content")
    private String content_;

    public Greeting() {
        super();
    }

    public Greeting(String pName) {
        this.content_ = "Hello " + pName;
    }

    public String getContent() {
        return content_;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the content_
     */
    public String getContent_() {
        return content_;
    }

    /**
     * @param content_ the content_ to set
     */
    public void setContent_(String content_) {
        this.content_ = content_;
    }

}
