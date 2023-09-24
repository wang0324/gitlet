package gitlet;

import java.io.Serializable;

public class Branch implements Serializable {
    /**
     * last commit.
     */
    private String lastCommit;

    /**
     * name.
     */
    private String name;

    public Branch(String n, String c) {
        this.name = n;
        this.lastCommit = c;
    }

    public String getName() {
        return this.name;
    }

    public String getLastCommit() {
        return lastCommit;
    }

    public void setLastCommit(String lc) {
        this.lastCommit = lc;
    }
}
