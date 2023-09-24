package gitlet;

import java.io.Serializable;

public class Blob implements Serializable {
    /**
     * fname.
     */
    private String filename;

    /**
     * contents.
     */
    private String contents;

    /**
     * sha1.
     */
    private String sha1;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String fname) {
        this.filename = fname;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String c) {
        this.contents = c;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String s) {
        this.sha1 = s;
    }

    public Blob(String fname, String c, String s) {
        this.filename = fname;
        this.contents = c;
        this.sha1 = s;
    }
}
