/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.metrics.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import fr.cnes.regards.framework.logbackappender.domain.LogEvent;

/**
 * This class allows to persists a {@link LogEvent}.
 * 
 * @author Christophe Mertz
 *
 */
@Entity
@Table(name = "t_event_log", indexes = { @Index(name = "idx_log_event", columnList = "id") })
@SequenceGenerator(name = "logEventSequence", initialValue = 1, sequenceName = "seq_log_event")
public class LogEventJpa extends LogEvent {

    /**
     * A constant used to define a {@link String} constraint with length 2048
     */
    private static final int MAX_STRING_LENGTH = 2048;

    /**
     * A constant used to define a {@link String} constraint with length 255
     */
    private static final int MEDIUM_STRING_LENGTH = 255;

    /**
     * A constant used to define a small {@link String} constraint with length 32 
     */
    private static final int SMALL_STRING_LENGTH = 32;

    /**
     * Unique id
     */
    private Long id;

    public LogEventJpa() {
        super();
    }

    public LogEventJpa(LogEvent logEvent) {
        this.msg = logEvent.getMsg();
        this.microservice = logEvent.getMicroservice();
        this.caller = logEvent.getCaller();
        this.method = logEvent.getMethod();
        this.date = logEvent.getDate();
        this.level = logEvent.getLevel();
        this.userName = logEvent.getUserName();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "logEventSequence")
    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        this.id = pId;
    }

    @Column(nullable = false, updatable = false, length = MAX_STRING_LENGTH)
    public String getMsg() {
        return msg;
    }

    public void setMsg(String pMsg) {
        this.msg = pMsg;
    }

    @Column(nullable = false, updatable = false, length = SMALL_STRING_LENGTH)
    public String getMicroService() {
        return microservice;
    }

    public void setMicroService(String pMicroService) {
        this.microservice = pMicroService;
    }

    @Column(nullable = false, updatable = false, length = MEDIUM_STRING_LENGTH)
    public String getCaller() {
        return caller;
    }

    public void setCaller(String pCaller) {
        this.caller = pCaller;
    }

    @Column(nullable = false, updatable = false, length = MEDIUM_STRING_LENGTH)
    public String getMethod() {
        return method;
    }

    public void setMethod(String pMethod) {
        this.method = pMethod;
    }

    @Column(nullable = false, updatable = false, length = SMALL_STRING_LENGTH)
    public String getDate() {
        return date;
    }

    public void setDate(String pDate) {
        this.date = pDate;
    }

    @Column(nullable = false, updatable = false, length = SMALL_STRING_LENGTH)
    public String getLevel() {
        return level;
    }

    public void setLevel(String pLevel) {
        this.level = pLevel;
    }

    @Column(nullable = false, updatable = false, length = MEDIUM_STRING_LENGTH)
    public String getUserName() {
        return userName;
    }

    public void setUserName(String pUsername) {
        this.userName = pUsername;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = microservice != null ? microservice.hashCode() : 0;
        result = prime * result + (caller != null ? caller.hashCode() : 0);
        result = prime * result + (method != null ? method.hashCode() : 0);
        result = prime * result + (userName != null ? userName.hashCode() : 0);
        result = prime * result + (date != null ? date.hashCode() : 0);
        result = prime * result + (msg != null ? msg.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LogEvent other = (LogEvent) obj;
        if (getMethod() == null) {
            if (other.getMethod() != null) {
                return false;
            }
        } else if (!getMethod().equals(other.getMsg())) {
            return false;
        }
        if (getMicroService() == null) {
            if (other.getMicroservice() != null) {
                return false;
            }
        } else if (!getMicroService().equals(other.getMicroservice())) {
            return false;
        }
        if (getCaller() == null) {
            if (other.getCaller() != null) {
                return false;
            }
        } else if (!getCaller().equals(other.getCaller())) {
            return false;
        }
        if (getMethod() == null) {
            if (other.getMethod() != null) {
                return false;
            }
        } else if (!getMethod().equals(other.getMethod())) {
            return false;
        }
        if (getDate() == null) {
            if (other.getDate() != null) {
                return false;
            }
        } else if (!getDate().equals(other.getDate())) {
            return false;
        }
        if (getLevel() == null) {
            if (other.getLevel() != null) {
                return false;
            }
        } else if (!getLevel().equals(other.getLevel())) {
            return false;
        }
        if (getUserName() == null) {
            if (other.getUserName() != null) {
                return false;
            }
        } else if (!getUserName().equals(other.getUserName())) {
            return false;
        }

        return true;
    }

}
