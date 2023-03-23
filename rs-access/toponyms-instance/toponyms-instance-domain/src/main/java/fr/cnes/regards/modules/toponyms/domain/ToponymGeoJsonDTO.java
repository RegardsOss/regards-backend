package fr.cnes.regards.modules.toponyms.domain;

/**
 * POJO to transfer the geojson received in backend-for-frontend ENDPOINT to {@link ToponymGeoJson}
 *
 * @author Iliana
 */
public class ToponymGeoJsonDTO {

    private String toponym;

    public ToponymGeoJsonDTO(String toponym) {
        this.toponym = toponym;
    }

    public String getToponym() {
        return toponym;
    }

    public void setToponym(String toponym) {
        this.toponym = toponym;
    }
}
