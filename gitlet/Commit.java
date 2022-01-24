package gitlet;


import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Date;
import java.io.File;

/** A Commit object consists of a log message, timestamp, a mapping of
 *  file names to blob references, a parent reference, and (for merges)
 *  a second parent reference.
 *  A Commit is saved to a file in name of the its SHA1 id.
 *  @author Wenhao Pan
 */
public class Commit implements Serializable {

    /** The constructor that takes in the metadata of a commit.
     * @param message the log message.
     * @param blobs key is the name of the file, val is the SHA1 id of the blob.
     * @param fParent SHA1 id of the first parent commit.
     * @param sParent SHA1 id of the second parent commit
     */
    public Commit(String message, HashMap<String, String> blobs, String
                  fParent, String sParent) {
        _message = message;
        _firstParent = fParent;
        _secondParent = sParent;
        _blobs = blobs;
        _time = new Date();
        if (!COMMITTEDDATA.exists()) {
            COMMITTEDDATA.mkdir();
        }
        _id = Utils.sha1(Utils.serialize(_blobs), Utils.serialize(_firstParent),
                Utils.serialize(_secondParent), Utils.serialize(_message),
                Utils.serialize(_time));
    }

    /** Return the id or the file name of the blob according to NAME. */
    public String getBlobId(String name) {
        if (!checkTrackedName(name)) {
            throw new GitletException("No such tracked file in the commit.");
        }
        return _blobs.get(name);
    }

    /** Return if the file NAME is tracked by the commit. */
    public boolean checkTrackedName(String name) {
        return _blobs.containsKey(name);
    }

    /** Return if the blob ID is tracked by the commit. */
    public boolean checkTrackedId(String id) {
        return _blobs.containsValue(id);
    }

    /** Save this commit to committedData directory. */
    public void save() throws IOException {
        File commit = Utils.join(COMMITTEDDATA, _id);
        commit.createNewFile();
        Utils.writeObject(commit, this);
    }

    @Override
    public String toString() {
        String merge = "";
        String date = String.format("Date: %1$ta %1$tb %1$te %1$tT "
                + "%1$tY %1$tz", _time);
        if (_secondParent != null) {
            merge = String.format("Merge: %s %s", _firstParent.substring(0, 7),
                    _secondParent.substring(0, 7));
            return String.format("===" + N + "commit %s" + N + "%s"
                    + N + "%s" + N + "%s" + N, _id, merge, date, _message);
        }
        return String.format("===" + N + "commit %s" + N
                + "%s" + N + "%s" + N, _id, date, _message);
    }
    /** Start a new line. */
    private static final String N = System.lineSeparator();

    /** Set the _time to January 1, 1970 00:00:00 GMT for initial commit. */
    public void initTime() {
        _time.setTime(0);
        _id = Utils.sha1(Utils.serialize(_blobs), Utils.serialize(_firstParent),
                Utils.serialize(_secondParent), Utils.serialize(_message),
                Utils.serialize(_time));
    }

    /** A getter method that returns _blobs. */
    public HashMap<String, String> getBlobs() {
        return _blobs;
    }

    /** A getter method that returns _firstParent. */
    public String getFirstParent() {
        return _firstParent;
    }

    /** A getter method that returns _secondParent. */
    public String getSecondParent() {
        return _secondParent;
    }

    /** A getter method that returns __message. */
    public String getMessage() {
        return _message;
    }

    /** A getter method that returns _id. */
    public String getId() {
        return _id;
    }

    /** A getter method that returns _time. */
    public Date getTime() {
        return _time;
    }

    /** The working directory. */
    private static final File CWD = new File(System.getProperty("user.dir"));

    /** The directory where stores all Commit objects. */
    private static final File COMMITTEDDATA = Utils.join(CWD,
            ".gitlet", "committedData");

    /** A HashMap with key of the file name,
     * and values of SHA1 (pointer) to the blobs.*/
    private HashMap<String, String> _blobs;

    /** The first parent commit. */
    private String _firstParent;

    /** The second parent commit. */
    private String _secondParent;

    /** The log message. */
    private String _message;

    /** The SHA1 id of the Commit. */
    private String _id;

    /** An Date object represents the time when the commit is created. */
    private Date _time;
}
