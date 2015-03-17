package dk.statsbiblioteket.doms.central.connectors.fedora.structures;

/**
 * Created by IntelliJ IDEA. User: abr Date: 9/14/11 Time: 11:51 AM To change this template use File | Settings | File
 * Templates.
 */
public class DatastreamProfile {
    private String ID;

    private String label;

    private String mimeType;
    private String state;
    private String checksum;
    private String checksumType;
    private long created;
    private String formatURI;

    private boolean internal;
    private String url;


    public void setID(String ID) {
        this.ID = ID;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getID() {
        return ID;
    }

    public String getLabel() {
        return label;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setChecksumType(String checksumType) {
        this.checksumType = checksumType;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public void setFormatURI(String formatURI) {
        this.formatURI = formatURI;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getState() {
        return state;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getChecksumType() {
        return checksumType;
    }

    public long getCreated() {
        return created;
    }

    public String getFormatURI() {
        return formatURI;
    }
}
