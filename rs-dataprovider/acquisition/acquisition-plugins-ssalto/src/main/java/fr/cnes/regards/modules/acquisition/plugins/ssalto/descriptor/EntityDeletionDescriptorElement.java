package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christophe Mertz
 */

public class EntityDeletionDescriptorElement extends EntityDescriptorElement {

    private final List<String> dataObjectList = new ArrayList<>();

    private final List<String> dataStorageObjectList = new ArrayList<>();

    @Override
    public String getEntityId() {
        return null;
    }

    @Override
    public ElementType getElementType() {
        // INUTILE
        return ElementType.UPDATE_ELEMENT_TYPE;
    }

    /**
     * renvoie un ordre pour pouvoir etre trie dans les fichiers descripteurs
     */
    @Override
    protected int getOrder() {
        return 5;
    }

    public void addProductId(String pDOId) {
        dataObjectList.add(pDOId);
    }

    public void addFileId(String pDSOId) {
        dataStorageObjectList.add(pDSOId);
    }

    public List<String> getDataObjectList() {
        return dataObjectList;
    }

    public List<String> getDataStorageObjectList() {
        return dataStorageObjectList;
    }
}
