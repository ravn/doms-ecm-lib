package dk.statsbiblioteket.doms.central.connectors.fedora.structures;

/**
 * Created by IntelliJ IDEA. User: abr Date: 8/26/11 Time: 2:18 PM To change this template use File | Settings | File
 * Templates.
 */
public class SearchResult {


    private String pid;
    private String label;
    private String state;
    private long cDate;
    private long mDate;

    public SearchResult(String pid,
                        String label,
                        String state,
                        long cDate,
                        long mDate) {
        //To change body of created methods use File | Settings | File Templates.
        this.pid = pid;
        this.label = label;
        this.state = state;
        this.cDate = cDate;
        this.mDate = mDate;
    }

    public String getPid() {
        return pid;
    }

    public String getLabel() {
        return label;
    }

    public String getState() {
        return state;
    }

    public long getcDate() {
        return cDate;
    }

    public long getmDate() {
        return mDate;
    }
}
