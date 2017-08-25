package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christophe Mertz
 */

public class EntityDeletionDescriptorElement extends EntityDescriptorElement {

    private final List<String> dataObjectList_ = new ArrayList<>();

    private final List<String> dataStorageObjectList_ = new ArrayList<>();

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
     * renvoie un ordre pour pouvoir etre trie dans les fichiers descripteurs. Methode surchargee
     * 
     * @see ssalto.domain.data.descriptor.EntityDescriptorElement#getOrder()
     */
    @Override
    protected int getOrder() {
        return 5;
    }

    public void addProductId(String pDOId) {
        dataObjectList_.add(pDOId);
    }

    public void addFileId(String pDSOId) {
        dataStorageObjectList_.add(pDSOId);
    }

    public List<String> getDataObjectList() {
        return dataObjectList_;
    }

    public List<String> getDataStorageObjectList() {
        return dataStorageObjectList_;
    }
}
