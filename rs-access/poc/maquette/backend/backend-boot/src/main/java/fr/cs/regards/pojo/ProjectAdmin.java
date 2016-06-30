package fr.cs.regards.pojo;

import org.springframework.hateoas.ResourceSupport;

public class ProjectAdmin extends ResourceSupport {
	private String name;

	public ProjectAdmin(String name) {
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
