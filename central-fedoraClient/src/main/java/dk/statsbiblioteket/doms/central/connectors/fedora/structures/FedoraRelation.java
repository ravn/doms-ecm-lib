package dk.statsbiblioteket.doms.central.connectors.fedora.structures;

/**
 * Created by IntelliJ IDEA. User: abr Date: 3/15/11 Time: 10:25 AM To change this template use File | Settings | File
 * Templates.
 */
public class FedoraRelation {
    private String subject;
    private String predicate;
    private String object;

    private boolean literal;

    public FedoraRelation(String subject,
                          String predicate,
                          String object) {

        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    public String getSubject() {
        return subject;
    }

    public String getPredicate() {
        return predicate;
    }

    public String getObject() {
        return object;
    }

    public boolean isLiteral() {
        return literal;
    }

    public void setLiteral(boolean literal) {
        this.literal = literal;
    }
}
