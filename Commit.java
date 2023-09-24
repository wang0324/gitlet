package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class Commit implements Serializable {

    /**
     * msg.
     */
    private String message;

    /**
     * time.
     */
    private String timestamp;

    /**
     * blobs.
     */
    private HashMap<String, String> blobs;

    /**
     * parent.
     */
    private String parent;

    /**
     * second parent.
     */
    private String secondParent = "";

    /**
     * sha1 name.
     */
    private String name;

    /**
     * date.
     */
    private Date date;


    public Commit(String m, String pN, HashMap<String, String> b) {
        this.message = m;
        this.parent = pN;
        this.name = Utils.sha1(Utils.serialize(this));
        this.blobs = b;
        this.date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(
                "EE MMM dd kk:mm:ss YYYY Z");
        this.timestamp = "Date: " + sdf.format(date);

    }

    public Commit(String m, String pN, HashMap<String, String> b, boolean i) {
        if (i) {
            this.message = m;
            this.parent = pN;
            this.name = Utils.sha1(Utils.serialize(this));
            this.blobs = b;
            this.date = new Date(0);
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "EE MMM dd kk:mm:ss YYYY Z");
            this.timestamp = "Date: " + sdf.format(date);
        }

    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String m) {
        this.message = m;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String ts) {
        this.timestamp = ts;
    }

    public HashMap<String, String> getBlobs() {
        return blobs;
    }

    public void setBlobs(HashMap<String, String> b) {
        this.blobs = b;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String p) {
        this.parent = p;
    }

    public void setName(String n) {
        this.name = n;
    }

    public String getSecondParent() {
        return this.secondParent;
    }

    public void setSecondParent(String sP) {
        this.secondParent = sP;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String toString() {
        String s = "";
        s += "===\n";
        s += "commit " + name + "\n";
        s += timestamp + "\n";
        s += message + "\n";

        return s;
    }

}
