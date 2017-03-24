/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.logbackappender.domain;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * This class allows to represent an event send for the monitoring of REGARDS microservices.
 * 
 * @author Christophe Mertz
 *
 */
@Event(target = Target.ALL)
public class LogEvent implements ISubscribable {

    /**
     * The message sends with the event
     */
    private String msg;

    /**
     * The microservice name that send the event
     */
    private String microService;

    /**
     * The class name that sends the event
     */
    private String caller;

    /**
     * The method name that sends the event
     */
    private String method;

    /**
     * The event's date
     */
    private String date;

    /**
     * The log level
     */
    private String level;

    private String user;

    public LogEvent() {
        super();
    }

    /**
     * Default constructor with all members
     * 
     * @param msg
     *            The message sends with the event
     * @param microServiceName
     *            The microservice name that send the event
     * @param caller
     *            The class name that sends the event
     * @param method
     *            The method name that sends the event
     * @param date
     *            The event's date
     * @param level
     *            The log level
     */
    public LogEvent(String msg, String microServiceName, String caller, String method, String date, String level,
            String user) {
        super();
        this.msg = msg;
        this.microService = microServiceName;
        this.caller = caller;
        this.method = method;
        this.date = date;
        this.level = level;
        this.user = user;
    }

    public String getMsg() {
        return msg;
    }

    public String getMicroService() {
        return microService;
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

    public String getUser() {
        return user;
    }

}
