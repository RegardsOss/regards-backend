package fr.cnes.regards.modules.acquisition.service.session;

import java.util.Collection;

import com.google.common.base.Strings;

import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.ingest.domain.sip.ISipState;

public class SessionChangingStateProbe {

    // Gathers info about initial product
    private String initialSession;

    private ProductState initialProductState;

    private ISipState initialProductSIPState;

    private long initalNbAcquiredFiles = 0L;

    // Gathers info about updated product
    private String ingestionChain;

    private String session;

    private ProductState productState;

    private ISipState productSIPState;

    public void addUpdatedProduct(Product updatedProduct) {
        ingestionChain = updatedProduct.getProcessingChain().getLabel();
        session = updatedProduct.getSession();
        productState = updatedProduct.getState();
        productSIPState = updatedProduct.getSipState();
    }

    public static SessionChangingStateProbe build(Product initialProduct, Collection<AcquisitionFile> newProductFiles) {
        SessionChangingStateProbe sessionChangingStateProbe = new SessionChangingStateProbe();
        if (initialProduct != null) {
            sessionChangingStateProbe.initialSession = initialProduct.getSession();
            sessionChangingStateProbe.initialProductState = initialProduct.getState();
            sessionChangingStateProbe.initialProductSIPState = initialProduct.getSipState();
            // In case product changed from session we have to calculate number of files scanned in the previous session.
            // This count is used after to decrement files acquired in the old session.
            long nbFilesInInitialProduct = 0L;
            if (newProductFiles.size() < initialProduct.getAcquisitionFiles().size()) {
                nbFilesInInitialProduct = initialProduct.getAcquisitionFiles().size() - newProductFiles.size();
            }
            sessionChangingStateProbe.setInitalNbAcquiredFiles(nbFilesInInitialProduct);
        }
        return sessionChangingStateProbe;
    }

    public boolean isSessionChanged() {
        return !Strings.isNullOrEmpty(initialSession) && !session.equals(initialSession);
    }

    public boolean shouldUpdateState() {
        return ((getInitialProductState() != getProductState()) && !(
        // Ignore FINISHED -> COMPLETED state change
        (getProductState() == ProductState.FINISHED) && (getInitialProductState() == ProductState.COMPLETED)));
    }

    public String getInitialSession() {
        return initialSession;
    }

    public ProductState getInitialProductState() {
        return initialProductState;
    }

    public ISipState getInitialProductSIPState() {
        return initialProductSIPState;
    }

    public String getIngestionChain() {
        return ingestionChain;
    }

    public String getSession() {
        return session;
    }

    public ProductState getProductState() {
        return productState;
    }

    public ISipState getProductSIPState() {
        return productSIPState;
    }

    public long getInitalNbAcquiredFiles() {
        return initalNbAcquiredFiles;
    }

    public void setInitalNbAcquiredFiles(long initalNbAcquiredFiles) {
        this.initalNbAcquiredFiles = initalNbAcquiredFiles;
    }
}
