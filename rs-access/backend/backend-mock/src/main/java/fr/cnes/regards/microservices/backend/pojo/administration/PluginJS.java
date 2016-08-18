package fr.cnes.regards.microservices.backend.pojo.administration;

import java.util.ArrayList;
import java.util.List;

public class PluginJS {

    private String name;

    private List<String> paths = new ArrayList<>();

    public PluginJS(String pName, List<String> pPaths) {
        super();
        this.name = pName;
        this.paths = pPaths;
    }


    public PluginJS(String pName, String pPath) {
        super();
        this.name = pName;
        this.paths.add(pPath);
    }


}
