package dk.statsbiblioteket.doms.central.connectors.fedora.structures;


import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: abr Date: 9/9/11 Time: 2:25 PM To change this template use File | Settings | File
 * Templates.
 */
public class ObjectProfile {


    private Date objectCreatedDate;
    private Date objectLastModifiedDate;
    private String label;
    private String ownerID;
    private String state;
    private String pid;
    private ObjectType type;
    private List<String> contentModels;
    private List<FedoraRelation> relations;
    private List<DatastreamProfile> datastreams;

    public ObjectType getType() {
        return type;
    }

    public void setType(ObjectType type) {
        this.type = type;
    }

    public List<FedoraRelation> getRelations() {
        return relations;
    }

    public void setRelations(List<FedoraRelation> relations) {
        this.relations = relations;
    }

    public List<DatastreamProfile> getDatastreams() {
        return datastreams;
    }

    public void setDatastreams(List<DatastreamProfile> datastreams) {
        this.datastreams = datastreams;
    }

    public void setObjectCreatedDate(Date objectCreatedDate) {
        this.objectCreatedDate = objectCreatedDate;
    }

    public void setObjectLastModifiedDate(Date objectLastModifiedDate) {
        this.objectLastModifiedDate = objectLastModifiedDate;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setOwnerID(String ownerID) {
        this.ownerID = ownerID;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public void setContentModels(List<String> contentModels) {
        this.contentModels = contentModels;
    }

    public Date getObjectCreatedDate() {
        return objectCreatedDate;
    }

    public Date getObjectLastModifiedDate() {
        return objectLastModifiedDate;
    }

    public String getLabel() {
        return label;
    }

    public String getOwnerID() {
        return ownerID;
    }

    public String getState() {
        return state;
    }

    public String getPid() {
        return pid;
    }

    public List<String> getContentModels() {
        return contentModels;
    }
}
