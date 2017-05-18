/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.person;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * {@link Person} test repository
 * @author Marc Sordi
 *
 */
public interface PersonRepository extends JpaRepository<Person, Long> {

}
