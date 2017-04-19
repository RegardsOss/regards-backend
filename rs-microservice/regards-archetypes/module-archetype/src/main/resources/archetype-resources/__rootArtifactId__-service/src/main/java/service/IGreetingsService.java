#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * LICENSE_PLACEHOLDER
 */
package ${package}.service;

import ${package}.domain.Greeting;

/**
 * 
 * TODO Description
 * @author TODO
 *
 */
public interface IGreetingsService {

    /**
     *
     * Get greetings
     *
     * @return {@link Greeting}
     * @since 1.0.0
     */
    Greeting getGreeting(String pName);

}
