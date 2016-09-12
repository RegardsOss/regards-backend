/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.${moduleName}.domain;

/**
 * 
 * TODO Description
 * @author TODO
 *
 */
public class Greeting {

	private final long id_;
	private final String content_;

	public Greeting(long pId, String pContent) {
		this.id_ = pId;
		this.content_ = pContent;
	}

	public long getId() {
		return id_;
	}

	public String getContent() {
		return content_;
	}
}
