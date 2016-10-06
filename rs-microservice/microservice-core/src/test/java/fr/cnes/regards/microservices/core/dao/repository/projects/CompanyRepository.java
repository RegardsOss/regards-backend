package fr.cnes.regards.microservices.core.dao.repository.projects;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.microservices.core.dao.pojo.projects.Company;

public interface CompanyRepository extends CrudRepository<Company, Long> {

}
