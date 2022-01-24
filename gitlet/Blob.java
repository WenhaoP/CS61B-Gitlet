package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/** A Blob object is a wrapper of a working file.
 *  @author Wenhao Pan
 */
public class Blob implements Serializable {

    /** The constructor of a Blob object.
     * @param workingFile The file we currently working on.
     */
    public Blob(File workingFile) {
        _workingFile = workingFile;
        _content = Utils.readContentsAsString(workingFile);
        _id = Utils.sha1(_content);
        if (!BLOBSDATA.exists()) {
            BLOBSDATA.mkdir();
        }
    }

    /** Save this Blob to blobsData directory. */
    public void save() throws IOException {
        File blob = Utils.join(BLOBSDATA, _id);
        if (!blob.exists()) {
            blob.createNewFile();
        }
        Utils.writeObject(blob, this);
    }

    /** Return _content. */
    public String getContent() {
        return _content;
    }
    /** Return _id. */
    public String getId() {
        return _id;
    }

    /** The working directory. */
    private static final File CWD = new File(System.getProperty("user.dir"));

    /** The repository of all Blobs. */
    private static final File BLOBSDATA = Utils.join(CWD,
            ".gitlet", "blobsData");

    /** the content of _workingFile as a String. */
    private String _content;

    /** the SHA1 id of the blob according to _content. */
    private String _id;

    /** The working file in CWD referenced by this Blob. */
    private File _workingFile;
}
