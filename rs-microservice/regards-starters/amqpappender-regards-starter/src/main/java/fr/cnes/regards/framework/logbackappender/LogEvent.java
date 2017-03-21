/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.logbackappender;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * 
 * @author Christophe Mertz
 *
 */
@Event(target = Target.ALL)
public class LogEvent implements IPollable {

    /**
     * The message send with the event 
     */
    private String msg;

    /**
     * The microservice name that send the event
     */
    private String microService;

    /**
     * The class name that send the event
     */
    private String caller;

    /**
     * The method name that send the event
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

    public LogEvent() {
        super();
    }

    /**
     * Default constructor with all members
     * 
     * @param msg
     * @param microServiceName
     * @param caller
     * @param method
     * @param date
     * @param level
     */
    public LogEvent(String msg, String microServiceName, String caller, String method, String date, String level) {
        super();
        this.msg = msg;
        this.microService = microServiceName;
        this.caller = caller;
        this.method = method;
        this.date = date;
        this.level = level;
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

}
