package fr.cnes.regards.plugins.utils;

import java.util.Date;
import java.util.List;

public class TestPojo {

    private String value;

    private List<String> values;

    private Date date;

    public String getValue() {
        return value;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValue(String pValue) {
        value = pValue;
    }

    public void setValues(List<String> pValues) {
        values = pValues;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date pDate) {
        date = pDate;
    }

}