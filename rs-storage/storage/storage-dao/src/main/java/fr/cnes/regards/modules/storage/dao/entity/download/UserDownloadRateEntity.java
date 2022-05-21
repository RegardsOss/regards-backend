package fr.cnes.regards.modules.storage.dao.entity.download;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "t_user_download_rate_gauge",
    uniqueConstraints = @UniqueConstraint(name = UserDownloadRateEntity.UK_DOWNLOAD_RATE_GAUGE_INSTANCE_EMAIL,
        columnNames = { "instance_id", "email" }))
@SequenceGenerator(name = UserDownloadRateEntity.DOWNLOAD_RATE_SEQUENCE, initialValue = 1,
    sequenceName = "seq_download_rate_gauge")
public class UserDownloadRateEntity {

    public static final String UK_DOWNLOAD_RATE_GAUGE_INSTANCE_EMAIL = "uk_download_rate_gauge_instance_email";

    public static final String DOWNLOAD_RATE_SEQUENCE = "downloadRateSequence";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = DOWNLOAD_RATE_SEQUENCE)
    @Column(name = "id")
    private Long id;

    @Column(name = "instance_id", nullable = false)
    private String instance;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "gauge", nullable = false)
    private Long gauge;

    @Column(name = "expiry", nullable = false)
    private LocalDateTime expiry;

    public UserDownloadRateEntity() {
        super();
    }

    public UserDownloadRateEntity(String instance, String email, Long gauge, LocalDateTime expiry) {
        this.instance = instance;
        this.email = email;
        this.gauge = gauge;
        this.expiry = expiry;
    }

    public UserDownloadRateEntity(Long id, String instance, String email, Long gauge, LocalDateTime expiry) {
        this.id = id;
        this.instance = instance;
        this.email = email;
        this.gauge = gauge;
        this.expiry = expiry;
    }

    public UserDownloadRateEntity(UserDownloadRateEntity other) {
        this.instance = other.instance;
        this.email = other.email;
        this.gauge = other.gauge;
        this.expiry = other.expiry;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getGauge() {
        return gauge;
    }

    public void setGauge(Long gauge) {
        this.gauge = gauge;
    }

    public LocalDateTime getExpiry() {
        return expiry;
    }

    public void setExpiry(LocalDateTime expiry) {
        this.expiry = expiry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserDownloadRateEntity that = (UserDownloadRateEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(instance, that.instance) && Objects.equals(email,
                                                                                                        that.email)
            && Objects.equals(gauge, that.gauge) && Objects.equals(expiry, that.expiry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, instance, email, gauge, expiry);
    }
}
