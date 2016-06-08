package fr.cs.regards.pojo;

import org.springframework.hateoas.ResourceSupport;

public class Project extends ResourceSupport{
	
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name_) {
		this.name = name_;
	}

	public Project(String name_) {
		super();
		this.name = name_;
	}
	

}
