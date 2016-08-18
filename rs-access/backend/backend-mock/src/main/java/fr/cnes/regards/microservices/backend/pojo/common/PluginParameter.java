package fr.cnes.regards.microservices.backend.pojo.common;

import org.springframework.hateoas.ResourceSupport;

import java.util.List;

public class PluginParameter extends ResourceSupport {
    private List<Object> dynamicValues;
    private boolean isDynamic;
    private String name;
    private Object value;

    public PluginParameter(List<Object> dynamicValues, boolean isDynamic, String name, Object value) {
        this.dynamicValues = dynamicValues;
        this.isDynamic = isDynamic;
        this.name = name;
        this.value = value;
    }

    public List<Object> getDynamicValues() {
        return dynamicValues;
    }

    public void setDynamicValues(List<Object> dynamicValues) {
        this.dynamicValues = dynamicValues;
    }

    public boolean isDynamic() {
        return isDynamic;
    }

    public void setDynamic(boolean dynamic) {
        isDynamic = dynamic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
