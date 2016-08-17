package fr.cnes.regards.microservices.backend.pojo;

import org.springframework.hateoas.ResourceSupport;

public class Project extends ResourceSupport {
	private String name;

	public Project(String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
