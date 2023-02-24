package fr.cnes.regards.modules.search.service.engine.plugin.opensearch;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

public class EngineConfiguration {

    @PluginParameter(name = "searchTitle",
                     label = "Title of responses of this search engine",
                     description = "Search title for response metadatas. Used to construct metadatas for atom+xml and geo+json responses.",
                     defaultValue = "Open search engine",
                     optional = false)
    private String searchTitle;

    @PluginParameter(name = "searchDescription",
                     label = "Description of responses of this search engine",
                     description = "Description for response metadatas. Used to construct metadatas for atom+xml and geo+json responses.",
                     defaultValue = "Open search engine",
                     optional = false)
    private String searchDescription;

    @PluginParameter(name = "contact",
                     label = "Contact email",
                     description = "Description for response metadatas. Used to construct metadatas for atom+xml and geo+json responses.",
                     optional = true)
    private String contact;

    @PluginParameter(name = "tags",
                     label = "Optional tags to add in opensearch descriptor xml file",
                     description = "Optional tags to add in opensearch descriptor xml file. Each tag must be separated by a blank white space caracter.",
                     optional = true)
    private String tags;

    @PluginParameter(name = "shortName", label = "Engine short name", defaultValue = "Opensearch", optional = false)
    private String shortName;

    @PluginParameter(name = "longName",
                     label = "Engine long name",
                     defaultValue = "Open search engine",
                     optional = true)
    private String longName;

    @PluginParameter(name = "image", label = "Optional image (icon or image) URL", optional = true)
    private String image;

    @PluginParameter(name = "attribution",
                     label = "Attribution",
                     defaultValue = "Created by RegardsOss framework (CNES)")
    private String attribution;

    public String getSearchTitle() {
        return searchTitle;
    }

    public void setSearchTitle(String searchTitle) {
        this.searchTitle = searchTitle;
    }

    public String getSearchDescription() {
        return searchDescription;
    }

    public void setSearchDescription(String searchDescription) {
        this.searchDescription = searchDescription;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getAttribution() {
        return attribution;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

}
