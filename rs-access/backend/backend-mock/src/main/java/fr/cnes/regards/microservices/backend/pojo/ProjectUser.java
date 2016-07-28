package fr.cnes.regards.microservices.backend.pojo;

import org.springframework.hateoas.ResourceSupport;

public class ProjectUser extends ResourceSupport {
	private String name;

	public ProjectUser(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
