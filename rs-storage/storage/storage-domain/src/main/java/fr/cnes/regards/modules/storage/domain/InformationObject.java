/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;

import com.google.gson.JsonElement;

public class InformationObject {

    private ContentInformation contentInformation;

    private JsonElement pdi;

    public InformationObject() {

    }

    public ContentInformation getContentInformation() {
        return contentInformation;
    }

    public void setContentInformation(ContentInformation pContentInformation) {
        contentInformation = pContentInformation;
    }

    public JsonElement getPdi() {
        return pdi;
    }

    public void setPdi(JsonElement pPdi) {
        pdi = pPdi;
    }

    public InformationObject generateRandomInformationObject() throws NoSuchAlgorithmException, MalformedURLException {
        contentInformation = new ContentInformation().generate();
        pdi = null;
        return this;
    }

}
