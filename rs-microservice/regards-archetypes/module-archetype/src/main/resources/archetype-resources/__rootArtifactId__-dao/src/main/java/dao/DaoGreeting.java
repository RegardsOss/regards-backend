#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * LICENSE_PLACEHOLDER
 */
package ${package}.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import ${package}.domain.Greeting;
/**
 * 
 * TODO Description
 * @author TODO
 *
 */
public interface DaoGreeting extends JpaRepository<Greeting, Long> {


}
