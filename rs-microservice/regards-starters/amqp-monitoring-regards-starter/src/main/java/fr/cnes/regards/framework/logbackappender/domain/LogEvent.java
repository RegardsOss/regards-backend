/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.logbackappender.domain;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * This class allows to represent an event send for the monitoring of REGARDS microservices.
 * @author Christophe Mertz
 */
@Event(target = Target.ALL)
public class LogEvent implements ISubscribable {

    /**
     * The message sends with the event
     */
    protected String msg;

    /**
     * The microservice name that send the event
     */
    protected String microservice;

    /**
     * The class name that sends the event
     */
    protected String caller;

    /**
     * The method name that sends the event
     */
    protected String method;

    /**
     * The event's date
     */
    protected String date;

    /**
     * The log level
     */
    protected String level;

    /**
     * The user
     */
    protected String userName;

    public LogEvent() {
        super();
    }

    /**
     * Default constructor with all members
     * @param msg The message sends with the event
     * @param microServiceName The microservice name that send the event
     * @param caller The class name that sends the event
     * @param method The method name that sends the event
     * @param date The event's date
     * @param level The log level
     */
    public LogEvent(String msg, String microserviceName, String caller, String method, String date, String level,
            String user) {
        super();
        this.msg = msg;
        this.microservice = microserviceName;
        this.caller = caller;
        this.method = method;
        this.date = date;
        this.level = level;
        this.userName = user;
    }

    public String getMsg() {
        return msg;
    }

    public String getMicroservice() {
        return microservice;
    }

    public String getCaller() {
        return caller;
    }

    public String getMethod() {
        return method;
    }

    public String getDate() {
        return date;
    }

    public String getLevel() {
        return level;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setMicroservice(String microservice) {
        this.microservice = microservice;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return "LogEvent : [" + date + "] microservice=" + getMicroservice() + ", user=" + getUserName() + ", msg="
                + getMsg();
    }

}
