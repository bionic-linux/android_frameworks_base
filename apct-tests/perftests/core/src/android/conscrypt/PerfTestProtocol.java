package android.conscrypt;

public enum PerfTestProtocol {

    TLSv13("TLSv1.3"),
    TLSv12("TLSv1.2");

    private final String[] protocols;

    PerfTestProtocol(String... protocols) {
        this.protocols = protocols;
    }

    public String[] getProtocols() {
        return protocols.clone();
    }
}