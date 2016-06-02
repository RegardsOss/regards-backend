package fr.cs.regards.pojo;

import java.util.ArrayList;
import java.util.List;

public class Plugin {
	
	private String name_;
	
	private List<String> paths_ = new ArrayList<>();

	public String getName() {
		return name_;
	}

	public void setName(String name_) {
		this.name_ = name_;
	}

	public List<String> getPaths() {
		return paths_;
	}

	public void setPaths(List<String> pPaths) {
		this.paths_ = pPaths;
	}

	public Plugin(String pName, List<String> pPaths) {
		super();
		this.name_ = pName;
		this.paths_ = pPaths;
	}
	
	
	public Plugin(String pName, String pPath) {
		super();
		this.name_ = pName;
		this.paths_.add(pPath);
	}
	

}
