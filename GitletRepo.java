package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class GitletRepo {

    /**
     * current branch.
     */
    private String currentBranch;
    /**
     * head.
     */
    private String head;
    /**
     * commits.
     */
    private HashMap<String, Commit> commits;
    /**
     * blobs.
     */
    private HashMap<String, Blob> blobs;
    /**
     * addition.
     */
    private HashMap<String, String> addition;
    /**
     * removal.
     */
    private HashMap<String, String> removal;
    /**
     * branches.
     */
    private ArrayList<Branch> branches;

    /**
     * cwd.
     */
    private final File cwd = new File(
            System.getProperty("user.dir"));
    /**
     * gitlet.
     */
    private final File gitlet = new File(
            cwd.getPath() + "/.gitlet/");
    /**
     * staging.
     */
    private final File stagingAreaFile = new File(
            gitlet.getPath() + "/staging");
    /**
     * adding.
     */
    private final File addingStageFile = new File(
            stagingAreaFile.getPath() + "/addition");
    /**
     * removing.
     */
    private final File removingStageFile = new File(
            stagingAreaFile.getPath() + "/removal");
    /**
     * blob.
     */
    private final File blobsDir = new File(
            gitlet.getPath() + "/blobs/");
    /**
     * commit.
     */
    private final File commitsDir = new File(
            gitlet.getPath() + "/commits/");
    /**
     * branch.
     */
    private final File branchesDir = new File(
            gitlet.getPath() + "/branches/");
    /**
     * currb.
     */
    private final File currBranchFile = new File(
            gitlet.getPath() + "/currBranch");
    /**
     * head.
     */
    private final File headFile = new File(
            gitlet.getPath() + "/HEAD");

    @SuppressWarnings("unchecked")
    public GitletRepo() {

        if (new File(cwd.getPath() + "/.gitlet").exists()) {
            currentBranch = Utils.readObject(
                    currBranchFile, Branch.class).getName();
            head = Utils.readContentsAsString(headFile);
            commits = new HashMap<>();
            blobs = new HashMap<>();

            addition = (HashMap<String, String>)
                    (Utils.readObject(addingStageFile, HashMap.class));
            removal = (HashMap<String, String>)
                    (Utils.readObject(removingStageFile, HashMap.class));
            branches = new ArrayList<>();

            for (String bFileName : Utils.plainFilenamesIn(commitsDir)) {
                File f = Utils.join(commitsDir, bFileName);
                commits.put(
                        Utils.readObject(f, Commit.class).getName(),
                        Utils.readObject(f, Commit.class));
            }

            for (String bFileName : Utils.plainFilenamesIn(blobsDir)) {

                File f = Utils.join(blobsDir, bFileName);
                blobs.put(
                        Utils.readObject(f, Blob.class).getSha1(),
                        Utils.readObject(f, Blob.class));
            }

            for (String bFileName : Utils.plainFilenamesIn(branchesDir)) {
                File f = Utils.join(
                        branchesDir, bFileName);
                branches.add(
                        Utils.readObject(f, Branch.class));
            }
        }
    }

    public void init() throws IOException {
        if (new File(
                cwd.getPath() + "/.gitlet/").exists()) {
            System.out.println(
                    "A Gitlet version-control system"
                    + " already exists in the current directory.");
            return;
        }

        gitlet.mkdir();
        stagingAreaFile.mkdir();
        addingStageFile.createNewFile();
        removingStageFile.createNewFile();
        blobsDir.mkdir();
        commitsDir.mkdir();
        branchesDir.mkdir();
        currBranchFile.createNewFile();
        headFile.createNewFile();

        Commit initial = new Commit(
                "initial commit",
                "", new HashMap<>(),
                true);
        serializeCommitToFile(initial);

        Branch master = new Branch(
                "master", initial.getName());
        Utils.writeObject(
                currBranchFile, master);
        Utils.writeObject(
                Utils.join(branchesDir, master.getName()), master);

        blobs = new HashMap<>();
        head = initial.getName();
        Utils.writeContents(headFile, head);

        addition = new HashMap<>();
        removal = new HashMap<>();
        Utils.writeObject(addingStageFile, addition);
        Utils.writeObject(removingStageFile, removal);

        branches = new ArrayList<>();
        branches.add(master);
    }

    private void serializeCommitToFile(Commit commit) {
        String sha1 = commit.getName();
        Utils.writeObject(
                new File(".gitlet/commits/" + sha1),
                commit);
    }

    public void add(String filename) {

        if (!new File(filename).exists()) {
            System.out.println("File does not exist.");
            return;
        }

        String content = Utils.readContentsAsString(new File(filename));
        String sha1 = Utils.sha1(content);

        if (removal.getOrDefault(filename, "troll").equals(sha1)) {
            removal.remove(filename);
            Utils.writeObject(removingStageFile, removal);
        }

        boolean altered = false;
        Commit c = getCommit(head);
        if (c.getBlobs().getOrDefault(filename, "troll").equals(sha1)) {
            altered = false;
        } else if (addition.containsKey(filename)) {
            if (!addition.get(filename).equals(sha1)) {
                altered = true;
            }
        } else {
            altered = true;
        }

        if (altered) {
            Blob newBlob = new Blob(filename, content, sha1);
            addition.put(filename, newBlob.getSha1());
            blobs.put(newBlob.getSha1(), newBlob);

            Utils.writeObject(Utils.join(blobsDir, sha1), newBlob);
            Utils.writeObject(addingStageFile, addition);
        } else {
            addition.remove(filename);
        }
    }

    public void rm(String filename) {
        Commit c = getCommit(head);
        if (c.getBlobs().containsKey(filename)) {
            Utils.restrictedDelete(filename);
            removal.put(filename, c.getBlobs().get(filename));
            Utils.writeObject(removingStageFile, removal);
        } else if (addition.containsKey(filename)) {
            addition.remove(filename);
            Utils.writeObject(addingStageFile, addition);
        } else {
            System.out.println("No reason to remove the file.");
        }
    }

    public void commit(String message) {
        if (addition.size() == 0 && removal.size() == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }

        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }

        Commit oldCommit = getCommit(head);
        HashMap<String, String> currentBlobs = oldCommit.getBlobs();

        for (String key : addition.keySet()) {
            currentBlobs.put(key, addition.get(key));
        }

        for (String key : removal.keySet()) {
            currentBlobs.remove(key);
        }

        Commit newCommit = new Commit(message, head, currentBlobs);
        serializeCommitToFile(newCommit);

        addition = new HashMap<>();
        Utils.writeObject(addingStageFile, addition);

        removal = new HashMap<>();
        Utils.writeObject(removingStageFile, removal);

        head = newCommit.getName();
        Utils.writeContents(headFile, head);

        commits.put(newCommit.getName(), newCommit);

        Branch b = getBranch(currentBranch);
        b.setLastCommit(head);
        Utils.writeObject(Utils.join(branchesDir, b.getName()), b);
    }

    private Commit getCommit(String name) {
        return Utils.readObject(
                new File(".gitlet/commits/" + name),
                Commit.class);
    }

    public void log() {
        String curr = head;
        while (curr != "") {
            Commit c = getCommit(curr);
            System.out.println(c);
            curr = c.getParent();
        }
    }

    public void globalLog() {
        for (String sha1 : commits.keySet()) {
            System.out.println(getCommit(sha1));
        }
    }

    public void find(String message) {
        boolean found = false;
        for (String sha1 : commits.keySet()) {
            Commit c = getCommit(sha1);
            if (c.getMessage().equals(message)) {
                System.out.println(c.getName());
                found = true;
            }
        }

        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        if (!gitlet.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        ArrayList<String> branchNames = new ArrayList<>();
        for (Branch b : branches) {
            branchNames.add(b.getName());
        }
        Collections.sort(branchNames);

        System.out.println("=== Branches ===");
        for (String s : branchNames) {
            if (s.equals(currentBranch)) {
                System.out.println("*" + s);
            } else {
                System.out.println(s);
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        ArrayList<String> stagedFiles = new ArrayList<>(addition.keySet());
        Collections.sort(stagedFiles);
        for (String s : stagedFiles) {
            System.out.println(s);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        ArrayList<String> removedFiles = new ArrayList<>(removal.keySet());
        Collections.sort(removedFiles);
        for (String s : removedFiles) {
            System.out.println(s);
        }
        System.out.println();

        displayExtraCredit();
    }

    private void displayExtraCredit() {
        Commit c = getCommit(head);
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String f : c.getBlobs().keySet()) {
            if (!addition.containsKey(f) && !removal.containsKey(f)) {
                boolean inCWD = Utils.plainFilenamesIn(cwd).contains(f);

                if (!inCWD) {
                    System.out.println(f + " (deleted)");
                } else {
                    for (String s : Utils.plainFilenamesIn(cwd)) {
                        if (s.equals(f)) {
                            String currFC = Utils.readContentsAsString(
                                    Utils.join(cwd, s));
                            if (!Utils.sha1(currFC)
                                    .equals(c.getBlobs()
                                            .get(f))) {
                                if (!addition.containsKey(f)) {
                                    System.out.println(f + " (modified)");
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println();

        System.out.println("=== Untracked Files ===");
        ArrayList<String> untracked = new ArrayList<>();

        for (String f : Utils.plainFilenamesIn(cwd)) {
            if (!c.getBlobs().containsKey(f)
                    && !addition.containsKey(f)
                    && !removal.containsKey(f)) {
                untracked.add(f);
            }
        }
        Collections.sort(untracked);
        for (String f : untracked) {
            System.out.println(f);
        }
        System.out.println();
    }

    public void checkoutFile(String filename) {
        Commit c = getCommit(head);
        HashMap<String, String> cBlobs = c.getBlobs();

        boolean fileExists = false;
        for (Map.Entry entry : cBlobs.entrySet()) {
            String key = (String) entry.getKey();
            String sha1 = cBlobs.get(key);

            if (key.equals(filename)) {
                Blob b = blobs.get(sha1);
                Utils.writeContents(Utils.join(cwd, filename), b.getContents());
                fileExists = true;
            }
        }

        if (!fileExists) {
            System.out.println("File does not exist in that commit.");
        }
    }

    public void checkoutCommit(String commitID, String filename) {
        boolean found = false;
        for (String sha1 : commits.keySet()) {
            if (sha1.contains(commitID)) {
                commitID = sha1;
                found = true;
            }
        }

        if (!found) {
            System.out.println("No commit with that id exists.");
            return;
        }


        Commit c = getCommit(commitID);
        if (!c.getBlobs().containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        String sha1 = c.getBlobs().get(filename);
        Utils.writeContents(new File(filename), blobs.get(sha1).getContents());
    }

    private boolean commitExists(String commitID) {
        for (String fname : Utils.plainFilenamesIn(commitsDir)) {
            if (fname.contains(commitID)) {
                return true;
            }
        }

        return false;
    }

    public void checkoutBranch(String branch) {
        if (!branchExists(branch)) {
            System.out.println("No such branch exists.");
            return;
        }

        Branch b = getBranch(branch);

        if (b.getName().equals(currentBranch)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        Commit newHead = getCommit(b.getLastCommit());
        Commit currHead = getCommit(head);

        HashMap<String, String> newBlobs = newHead.getBlobs();

        for (String filename : Utils.plainFilenamesIn(cwd)) {
            if (newBlobs.containsKey(filename)
                    && !currHead.getBlobs().containsKey(filename)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                return;
            }
        }

        for (String filename : Utils.plainFilenamesIn(cwd)) {
            if (currHead.getBlobs().containsKey(filename)
                    && !newBlobs.containsKey(filename)) {
                Utils.restrictedDelete(
                        Utils.join(cwd, filename));
            }
        }

        for (String filename : newBlobs.keySet()) {
            Utils.writeContents(
                    new File(filename),
                    blobs.get(newBlobs.get(filename)).getContents());
        }

        addition = new HashMap<>();
        Utils.writeObject(addingStageFile, addition);

        removal = new HashMap<>();
        Utils.writeObject(removingStageFile, removal);


        currentBranch = branch;
        head = newHead.getName();
        Utils.writeContents(headFile, head);
        Utils.writeObject(currBranchFile, b);
    }

    private Branch getBranch(String branch) {
        for (Branch b : branches) {
            if (b.getName().equals(branch)) {
                return b;
            }
        }
        return null;
    }

    private boolean branchExists(String branch) {
        for (Branch b : branches) {
            if (b.getName().equals(branch)) {
                return true;
            }
        }
        return false;
    }

    public void createBranch(String name) {
        for (Branch b : branches) {
            if (b.getName().equals(name)) {
                System.out.println("A branch with that name already exists.");
                return;
            }
        }

        Branch b = new Branch(name, head);
        branches.add(b);
        Utils.writeObject(Utils.join(branchesDir, b.getName()), b);
    }

    public void removeBranch(String name) {
        if (name.equals(currentBranch)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }

        for (Branch b : branches) {
            if (b.getName().equals(name)) {
                Utils.join(branchesDir, name).delete();
                return;
            }
        }

        System.out.println("A branch with that name does not exist.");

    }

    public void reset(String commitID) {
        if (!commitExists(commitID)) {
            System.out.println("No commit with that id exists.");
            return;
        }

        Commit currHead = getCommit(head);
        Commit newHead = getCommit(commitID);

        for (String filename : Utils.plainFilenamesIn(cwd)) {
            if (newHead.getBlobs().containsKey(filename)
                    && !currHead.getBlobs().containsKey(filename)) {
                System.out.println("There is an untracked file"
                        + " in the way; delete it, "
                        + "or add and commit it first.");
                return;
            }
        }

        for (String filename : Utils.plainFilenamesIn(cwd)) {
            if (currHead.getBlobs().containsKey(filename)
                    && !newHead.getBlobs().containsKey(filename)) {
                Utils.restrictedDelete(
                        Utils.join(cwd, filename));
            }
        }

        for (String filename : newHead.getBlobs().keySet()) {
            Utils.writeContents(
                    new File(filename),
                    blobs.get(newHead.getBlobs()
                            .get(filename)).getContents());
        }


        addition = new HashMap<>();
        Utils.writeObject(addingStageFile, addition);

        removal = new HashMap<>();
        Utils.writeObject(removingStageFile, removal);

        head = newHead.getName();
        Utils.writeContents(headFile, head);

        Branch b = getBranch(currentBranch);
        b.setLastCommit(head);
        Utils.writeObject(Utils.join(branchesDir, b.getName()), b);
    }

    public void merge(String branchName) {
        if (initialMergeChecks(branchName)) {
            return;
        }
        Branch givenBr = getBranch(branchName);
        Branch currBr = getBranch(currentBranch);
        Commit currH = getCommit(head);
        Commit givenH = getCommit(givenBr.getLastCommit());
        HashMap<String, String> newBlobs = new HashMap<>();
        HashMap<String, String> currBlobs = currH.getBlobs();
        HashMap<String, String> givenBlobs = givenH.getBlobs();
        for (String filename : Utils.plainFilenamesIn(cwd)) {
            if (checkUntracked(givenBlobs, currBlobs, filename)) {
                return;
            }
        }
        Commit splitPoint = findSplitPoint(branchName);
        HashMap<String, String> splitBlobs = splitPoint.getBlobs();
        boolean mergeConflict = false;
        if (splitPoint.getName()
                .equals(givenH.getName())) {
            newBlobs = currH.getBlobs();
            System.out.println("Given branch is"
                    + " an ancestor of the current branch.");
        } else if (speedrunMerge(splitPoint,
                givenH, currH,
                newBlobs, currBr)) {
            head = currH.getName();
        } else {
            for (String filename : Utils.plainFilenamesIn(cwd)) {
                String splitB = splitBlobs.getOrDefault
                        (filename, "s");
                String currB = currBlobs.getOrDefault
                        (filename, "c");
                String givenB = givenBlobs.getOrDefault(
                        filename, "g");
                if (causesMergeConflict(splitB, currB, givenB)) {
                    mergeConflict = true;
                    displayMergeConflict(givenB, currB, filename);
                    newBlobs.put(filename,
                            Utils.sha1(
                                    Utils.readContentsAsString(
                                            new File(filename))));
                } else {
                    updateNewBlobs(splitB, currB,
                            givenB, filename, newBlobs);
                }

            }
            addMissing(givenBlobs, splitBlobs, newBlobs);
        }
        Commit newCommit = createNewMCommit(givenBr, currBr, currH, newBlobs);
        newCommit.setSecondParent(givenH.getName());
        head = newCommit.getName();
        if (mergeConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        serializeCommitToFile(newCommit);
        Utils.writeContents(headFile, head);
    }

    private Commit createNewMCommit(Branch givenBr, Branch currBr,
                                    Commit currH,
                                    HashMap<String, String> newBlobs) {
        return new Commit("Merged " + givenBr.getName()
                + " into " + currBr.getName()
                + ".", currH.getName(),
                newBlobs);
    }

    private boolean checkUntracked(HashMap<String, String> givenBlobs,
                                   HashMap<String, String> currBlobs,
                                   String filename) {
        if (givenBlobs.containsKey(filename)
                && !currBlobs.containsKey(filename)) {
            System.out.println("There is an untracked file"
                    + " in the way; delete it,"
                    + " or add and commit it first.");
            return true;
        }
        return false;
    }

    private void updateNewBlobs(String splitB, String currB,
                                String givenB, String filename,
                                HashMap<String, String> newBlobs) {
        if (splitB.equals("s")) {
            if (givenB.equals("g") && !currB.equals("c")) {
                newBlobs.put(filename, currB);
            } else if (!givenB.equals("g") && currB.equals("c")) {
                newBlobs.put(filename, givenB);
            } else if (!givenB.equals("g") && !currB.equals("c")) {
                if (givenB.equals(currB)) {
                    newBlobs.put(filename, givenB);
                }
            }
        } else if (!splitB.equals(givenB) && !givenB.equals("g")) {
            if (splitB.equals(currB)) {
                newBlobs.put(filename, givenB);
                Utils.writeContents(
                        Utils.join(cwd, filename),
                        blobs.get(givenB).getContents());
            } else if (!splitB.equals(currB)) {
                if (currB.equals(givenB)) {
                    newBlobs.put(filename, currB);
                }
            }
        } else if (!splitB.equals(currB) && !currB.equals("c")) {
            if (splitB.equals(givenB)) {
                newBlobs.put(filename, currB);
            }
        } else if (!splitB.equals("s")) {
            if (givenB.equals("g")) {
                Utils.restrictedDelete(Utils.join(cwd, filename));
            }
        }
    }

    private void addMissing(HashMap<String, String> givenBlobs,
                            HashMap<String, String> splitBlobs,
                            HashMap<String, String> newBlobs) {
        for (String filename : givenBlobs.keySet()) {
            String splitB = splitBlobs.getOrDefault(filename, "s");
            String givenB = givenBlobs.get(filename);

            if (splitB.equals("s")
                    && !Utils.plainFilenamesIn(cwd)
                    .contains(filename)) {
                newBlobs.put(filename, givenB);
                Utils.writeContents(
                        Utils.join(
                                cwd, filename),
                        blobs.get(givenB).getContents());
            }
        }
    }

    private void displayMergeConflict(String givenB, String currB,
                                      String filename) {
        if (!givenB.equals("g")
                && !currB.equals("c")) {
            String s = "";
            s += "<<<<<<< HEAD" + "\n";
            s += Utils.readObject(
                    Utils.join(
                            blobsDir, currB),
                    Blob.class).getContents();
            s += "=======" + "\n";
            s += Utils.readObject(
                    Utils.join(
                            blobsDir, givenB),
                    Blob.class).getContents();
            s += ">>>>>>>" + "\n";
            Utils.writeContents(Utils.join(cwd, filename), s);
        } else if (!currB.equals("c")) {
            String s = "";
            s += "<<<<<<< HEAD" + "\n";
            s += Utils.readObject(
                    Utils.join(blobsDir,
                            currB), Blob.class).getContents();
            s += "=======" + "\n";
            s += ">>>>>>>" + "\n";
            Utils.writeContents(
                    Utils.join(cwd, filename), s);
        } else {
            String s = "";
            s += "<<<<<<< HEAD" + "\n";
            s += "=======" + "\n";
            s += Utils.readObject(
                    Utils.join(blobsDir, givenB), Blob.class).getContents();
            s += ">>>>>>>" + "\n";
            Utils.writeContents(
                    Utils.join(cwd, filename), s);
        }
    }

    private boolean speedrunMerge(Commit splitPoint,
                                  Commit givenHead, Commit currHead,
                                  HashMap<String, String> newBlobs,
                                  Branch currBranch) {
        if (splitPoint.getName().equals(head)) {
            head = givenHead.getName();
            currBranch.setLastCommit(head);
            newBlobs = givenHead.getBlobs();

            for (String filename : Utils.plainFilenamesIn(cwd)) {
                if (!newBlobs.containsKey(filename)) {
                    Utils.restrictedDelete(Utils.join(cwd, filename));
                }
            }

            System.out.println("Current branch fast-forwarded.");
            return true;
        }
        return false;
    }

    private boolean initialMergeChecks(String branchName) {
        if (!branchExists(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return true;
        }

        if (addition.size() > 0 || removal.size() > 0) {
            System.out.println("You have uncommitted changes.");
            return true;
        }

        if (currentBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }

        return false;
    }

    private boolean causesMergeConflict(
            String splitB, String currB,
            String givenB) {
        return (((!splitB.equals("s") && (!currB.equals(givenB))
                && (!splitB.equals(currB)) && (!splitB.equals(givenB)))
                || ((!splitB.equals("s") && currB.equals("c")
                && !givenB.equals("g") && !splitB.equals(givenB))
                || (!splitB.equals("s") && !currB.equals("c")
                && givenB.equals("g") && !splitB.equals(currB)))
                || (splitB.equals("s") && !currB.equals(givenB)
                && !currB.equals("c") && !givenB.equals("g"))));
    }

    private Commit findSplitPoint(String branchName) {
        Branch givenB = getBranch(branchName);
        Commit currHead = getCommit(head);
        Commit givenHead = getCommit(givenB.getLastCommit());

        HashSet<String> currentParents = new HashSet<>();
        String currCommitID = currHead.getName();
        ArrayList<String> Q = new ArrayList<>();
        Q.add(currCommitID);
        while (!Q.isEmpty()) {
            currCommitID = Q.remove(0);
            if (currCommitID.equals("")) {
                continue;
            }
            currentParents.add(currCommitID);

            Commit tempCommit = getCommit(currCommitID);
            Q.add(tempCommit.getParent());
            Q.add(tempCommit.getSecondParent());
        }

        String givenCommitID = givenHead.getName();
        Q = new ArrayList<>();
        Q.add(givenCommitID);
        String latestCommonAncestor = "";
        while (!Q.isEmpty()) {
            givenCommitID = Q.remove(0);
            if (givenCommitID.equals("")) {
                continue;
            }
            if (currentParents.contains(givenCommitID)) {
                Commit tempCommit = getCommit(givenCommitID);
                if (latestCommonAncestor.equals("")) {
                    latestCommonAncestor = givenCommitID;
                } else if (tempCommit.getDate()
                        .compareTo(getCommit(latestCommonAncestor)
                                .getDate()) > 0) {
                    latestCommonAncestor = givenCommitID;
                }
            }

            Commit tempCommit = getCommit(givenCommitID);
            Q.add(tempCommit.getParent());
            Q.add(tempCommit.getSecondParent());
        }

        if (!latestCommonAncestor.equals("")) {
            return getCommit(latestCommonAncestor);
        } else {
            return null;
        }
    }


}
