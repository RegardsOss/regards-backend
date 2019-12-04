package fr.cnes.regards.modules.acquisition.service.session;

import com.google.common.base.Strings;

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

    public static SessionChangingStateProbe build(Product initialProduct) {
        SessionChangingStateProbe sessionChangingStateProbe = new SessionChangingStateProbe();
        if (initialProduct != null) {
            sessionChangingStateProbe.initialSession = initialProduct.getSession();
            sessionChangingStateProbe.initialProductState = initialProduct.getState();
            sessionChangingStateProbe.initialProductSIPState = initialProduct.getSipState();
            sessionChangingStateProbe.setInitalNbAcquiredFiles(initialProduct.getAcquisitionFiles().size());
        }
        return sessionChangingStateProbe;
    }

    public boolean isSessionChanged() {
        return !Strings.isNullOrEmpty(initialSession) && !session.equals(initialSession);
    }

    public boolean shouldUpdateState() {
        return isSessionChanged() || ((getInitialProductState() != getProductState()) && !(
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
