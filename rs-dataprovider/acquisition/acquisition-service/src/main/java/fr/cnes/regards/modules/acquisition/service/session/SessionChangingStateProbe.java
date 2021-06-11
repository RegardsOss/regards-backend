package fr.cnes.regards.modules.acquisition.service.session;

import com.google.common.base.Strings;

import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.ingest.domain.sip.ISipState;

public class SessionChangingStateProbe {

    // Gathers info about initial product
    private String initialSession;

    private String initialSessionOwner;

    private String productName;

    private ProductState initialProductState;

    private ISipState initialProductSIPState;

    private long initalNbAcquiredFiles = 0L;

    private long initialNbAcquiredFilesSuperseded = 0L;

    // Gathers info about updated product
    private String ingestionChain;

    private String session;

    private String sessionOwner;

    private ProductState productState;

    private ISipState productSIPState;

    public void addUpdatedProduct(Product updatedProduct) {
        ingestionChain = updatedProduct.getProcessingChain().getLabel();
        session = updatedProduct.getSession();
        sessionOwner = updatedProduct.getProcessingChain().getLabel();
        productState = updatedProduct.getState();
        productSIPState = updatedProduct.getSipState();
    }

    public static SessionChangingStateProbe build(Product initialProduct) {
        SessionChangingStateProbe sessionChangingStateProbe = new SessionChangingStateProbe();
        if (initialProduct != null) {
            sessionChangingStateProbe.productName = initialProduct.getProductName();
            sessionChangingStateProbe.initialSession = initialProduct.getSession();
            sessionChangingStateProbe.initialSessionOwner = initialProduct.getProcessingChain().getLabel();
            sessionChangingStateProbe.initialProductState = initialProduct.getState();
            sessionChangingStateProbe.initialProductSIPState = initialProduct.getSipState();
            // In case product changed from session we have to calculate number of files scanned in the previous session.
            // This count is used after to decrement files acquired in the old session.
            sessionChangingStateProbe.initalNbAcquiredFiles = initialProduct.getActiveAcquisitionFiles().size();
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

    public String getInitialSessionOwner() {
        return initialSessionOwner;
    }

    public String getProductName() {
        return productName;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public long getInitialNbAcquiredFilesSuperseded() {
        return initialNbAcquiredFilesSuperseded;
    }

    public void setInitialNbAcquiredFilesSuperseded(long initialNbAcquiredFilesSuperseded) {
        this.initialNbAcquiredFilesSuperseded = initialNbAcquiredFilesSuperseded;
    }

    public void incInitialNbAcquiredFilesSuperseded() {
        this.initialNbAcquiredFilesSuperseded++;
    }

}
