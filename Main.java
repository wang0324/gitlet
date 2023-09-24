package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Kevin Wang
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        GitletRepo repo = new GitletRepo();
        if (args.length <= 0) {
            System.out.println("Please enter a command.");
        } else if (args[0].equals("init")) {
            repo.init();
        } else if (args[0].equals("commit")) {
            repo.commit(args[1]);
        } else if (args[0].equals("add")) {
            repo.add(args[1]);
        } else if (args[0].equals("log")) {
            repo.log();
        } else if (args[0].equals("checkout")) {
            if (args.length == 2) {
                repo.checkoutBranch(args[1]);
            } else if (args.length == 3) {
                repo.checkoutFile(args[2]);
            } else if (args.length == 4) {
                if (args[2].equals("--")) {
                    repo.checkoutCommit(args[1], args[3]);
                } else {
                    System.out.println("Incorrect operands.");
                }
            }
        } else if (args[0].equals("rm")) {
            repo.rm(args[1]);
        } else if (args[0].equals("global-log")) {
            repo.globalLog();
        } else if (args[0].equals("find")) {
            repo.find(args[1]);
        } else if (args[0].equals("status")) {
            repo.status();
        } else if (args[0].equals("branch")) {
            repo.createBranch(args[1]);
        } else if (args[0].equals("rm-branch")) {
            repo.removeBranch(args[1]);
        } else if (args[0].equals("reset")) {
            repo.reset(args[1]);
        } else if (args[0].equals("merge")) {
            repo.merge(args[1]);
        } else {
            System.out.println("No command with that name exists.");
        }
    }

}
