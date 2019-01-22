package fr.cnes.regards.framework.modules.locks.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.OffsetDateTime;

import org.hibernate.annotations.Type;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_lock",
        uniqueConstraints = @UniqueConstraint(name = "uk_lock", columnNames = { "lock_name", "locking_class_name" }))
public class Lock {

    @Id
    @SequenceGenerator(name = "lockSequence", initialValue = 1, sequenceName = "seq_lock")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lockSequence")
    private Long id;

    @Column(name = "lock_name", columnDefinition = "text", nullable = false)
    @Type(type = "text")
    private String lockName;

    @Column(name = "locking_class_name", columnDefinition = "text", nullable = false)
    @Type(type = "text")
    private String lockingClassName;

    @Column(name = "expiration_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime expirationDate;

    /**
     * Constructor
     * @param lockName method for which lock is being acquired
     * @param callerClazz class for which lock is being acquired
     */
    public Lock(String lockName, Class callerClazz) {
        this.lockName = lockName;
        this.lockingClassName = callerClazz.getName();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }

    public String getLockingClassName() {
        return lockingClassName;
    }

    public void setLockingClassName(String lockingClassName) {
        this.lockingClassName = lockingClassName;
    }

    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(OffsetDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void expiresIn(long seconds) {
        this.expirationDate = OffsetDateTime.now().plusSeconds(seconds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Lock lock = (Lock) o;

        if (!lockName.equals(lock.lockName)) {
            return false;
        }
        if (!lockingClassName.equals(lock.lockingClassName)) {
            return false;
        }
        return expirationDate != null ? expirationDate.equals(lock.expirationDate) : lock.expirationDate == null;
    }

    @Override
    public int hashCode() {
        int result = lockName.hashCode();
        result = 31 * result + lockingClassName.hashCode();
        result = 31 * result + (expirationDate != null ? expirationDate.hashCode() : 0);
        return result;
    }
}
