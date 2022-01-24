package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Wenhao Pan
 */
public class Main {

    /** The .gitlet repository. */
    private static final File GITLET =
            Utils.join(System.getProperty("user.dir"), ".gitlet");

    /** All posible commands. */
    private static final ArrayList<String> ALLCOMMANDS = new ArrayList<>(
            Arrays.asList("checkout", "merge", "init", "add", "rm", "log",
                    "global-log", "status", "branch", "rm-branch", "reset",
                    "commit", "find", "pull", "fetch", "push", "rm-remote",
                    "add-remote"));

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        try {
            if (args.length == 0) {
                throw new GitletException("Please enter a command.");
            } else if (!ALLCOMMANDS.contains(args[0])) {
                throw new GitletException("No command with that name exists.");
            } else if (!args[0].equals("init") && !GITLET.exists()) {
                throw new GitletException("Not in an initialized "
                        + "Gitlet directory.");
            }
            Commands command = new Commands(args);
            command.processCommand();
        } catch (GitletException exception) {
            System.err.println(exception.getMessage());
            System.exit(0);
        }
    }
}
