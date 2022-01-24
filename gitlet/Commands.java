package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/** Commands class is where we are actually handling all commands
 *  from Main class.
 *  @author Wenhao Pan
 */
public class Commands {

    /** A Commands object takes in the specific command message.
     *  @param command the command passed in in Main class.
     */
    public Commands(String... command) {
        _command = command;
    }

    /** According to different commands passed in Main, we
     *  handle it differently. */
    public void processCommand() {
        try {
            switch (_command[0]) {
            case "init":
                initCommand();
                break;
            case "add":
                addCommand();
                break;
            case "commit":
                commitCommand();
                break;
            case "rm":
                rmCommand();
                break;
            case "log":
                logCommand();
                break;
            case "global-log":
                gLogCommand();
                break;
            case "find":
                findCommand();
                break;
            case "status":
                statusCommand();
                break;
            case "checkout":
                checkoutCommand();
                break;
            case "branch":
                branchCommand();
                break;
            case "rm-branch":
                rmBranchCommand();
                break;
            case "reset":
                resetCommand();
                break;
            case "merge":
                mergeCommand();
                break;
            default:
                remoteCommand();
            }
        } catch (GitletException | IOException exception) {
            System.err.println(exception.getMessage());
            System.exit(0);
        }
    }

    /** Process all remote commands. */
    public void remoteCommand() throws IOException {
        try {
            switch (_command[0]) {
            case "add-remote":
                addRemoteCommand();
                break;
            case "rm-remote":
                rmRemoteCommand();
                break;
            case "push":
                pushCommand();
                break;
            case "fetch":
                fetchCommand();
                break;
            case "pull":
                pullCommand();
                break;
            default:
                throw new GitletException("No command with that name exists.");
            }
        } catch (GitletException | IOException exception) {
            System.err.println(exception.getMessage());
            System.exit(0);
        }
    }

    /** Saves the given login information under the given remote name. */
    public void addRemoteCommand() {
        readAll();
        checkOperands(3);
        String name = _command[1];
        String path = _command[2];
        if (_remoteInfo.containsKey(name)) {
            throw new GitletException("A remote with"
                    + " that name already exists.");
        }
        path = path.replace("/", S);
        _remoteInfo.put(name, new File(path));
        saveAll();
    }

    /** Remove information associated with the given remote name. */
    public void rmRemoteCommand() {
        readAll();
        checkOperands(2);
        String name = _command[1];
        if (!_remoteInfo.containsKey(name)) {
            throw new GitletException(" A remote with"
                    + " that name does not exist.");
        }
        _remoteInfo.remove(name);
        saveAll();
    }

    /** Attempts to append the current branch's commits to the end of the given
     *  branch at the given remote. */
    public void pushCommand() throws IOException {
        readAll(); readAllRemote(); checkOperands(3);
        if (!_rDir.exists()) {
            throw new GitletException("Remote directory not found.");
        }
        if (!_rBranches.containsKey(_rBranch)) {
            _rBranches.put(_rBranch, _rHead);
        }
        ArrayList<String> futureCommits = new ArrayList<>();
        Commit last = _currCommit;
        Commit curr = Utils.readObject(Utils.join(COMMITTEDDATA,
                _currCommit.getFirstParent()), Commit.class);
        String message = curr.getMessage();
        Boolean found = false;
        while (!message.equals("initial commit")) {
            if (curr.getId().equals(_rBranchHead.getId())) {
                futureCommits.add(last.getId());
                found = true;
                break;
            }
            futureCommits.add(curr.getId());
            String parent = curr.getFirstParent();
            last = curr;
            curr = Utils.readObject(Utils.join(COMMITTEDDATA,
                    parent), Commit.class);
            message = curr.getMessage();
        }
        if (!found) {
            throw new GitletException("Please pull down remote"
                    + " changes before pushing.");
        }
        for (String commit: futureCommits) {
            Commit each = Utils.readObject(Utils.join(COMMITTEDDATA,
                    commit), Commit.class);
            HashMap<String, String> blobs = each.getBlobs();
            for (String id: blobs.values()) {
                File localBlob = Utils.join(BLOBSDATA, id);
                Blob lB = Utils.readObject(localBlob, Blob.class);
                File remoteBlob = Utils.join(_rBlobs, id);
                if (!remoteBlob.exists()) {
                    remoteBlob.createNewFile();
                }
                Utils.writeObject(remoteBlob, lB);
            }
            File remoteCommit = Utils.join(_rCommits, each.getId());
            Commit lCommit = Utils.readObject(Utils.join(COMMITTEDDATA,
                    commit), Commit.class);
            if (!remoteCommit.exists()) {
                remoteCommit.createNewFile();
            }
            Utils.writeObject(remoteCommit, lCommit);
        }
        reset(_head);
        saveAll();
        saveAllRemote();
    }

    /** Checks out all the files tracked by the given commit
     *  ID for Remote command. */
    public void reset(String id) throws IOException {
        Commit target = findCommit(id, _rCommits);
        checkUntracked(target, _rDir, _rBranchHead);
        removeTracked(target, _rDir, _rBranchHead);
        for (String fileName: target.getBlobs().keySet()) {
            Blob b = Utils.readObject(Utils.join(_rBlobs,
                    target.getBlobId(fileName)), Blob.class);
            File workingFile = Utils.join(_rDir, fileName);
            if (!workingFile.exists()) {
                workingFile.createNewFile();
            }
            Utils.writeContents(workingFile, b.getContent());
        }
        _rHead = target.getId();
        _rBranches.replace(_rCurrBranch, target.getId());
        _rStageRemove.clear();
        _rStageAdd.clear();
    }

    /** Brings down commits from the remote Gitlet repository
     *  into the local Gitlet repository. */
    public void fetchCommand() throws IOException {
        readAll();
        readAllRemote();
        checkOperands(3);
        String newBranch = _rName + "/" + _rBranch;
        if (!_branches.containsKey(newBranch)) {
            _branches.put(newBranch, _initCommit);
        }
        String message = _rBranchHead.getMessage();
        Commit curr = _rBranchHead;
        List<String> allCommits = Utils.plainFilenamesIn(COMMITTEDDATA);
        while (!message.equals("initial commit")) {
            if (allCommits.contains(curr.getId())) {
                break;
            }
            File newC = Utils.join(COMMITTEDDATA, curr.getId());
            newC.createNewFile();
            Utils.writeObject(newC, curr);
            for (String x: curr.getBlobs().keySet()) {
                File remoteB = Utils.join(_rBlobs, curr.getBlobId(x));
                Blob rB = Utils.readObject(remoteB, Blob.class);
                File newB = Utils.join(BLOBSDATA, curr.getBlobId(x));
                if (!newB.exists()) {
                    newB.createNewFile();
                }
                Utils.writeObject(newB, rB);
            }
            curr = Utils.readObject(Utils.join(_rCommits,
                    curr.getFirstParent()), Commit.class);
            message = curr.getMessage();
        }
        _branches.replace(newBranch, _rBranchHead.getId());
        saveAll();
        saveAllRemote();
    }

    /** Fetches branch [remote name]/[remote branch name] as for the fetch
     *  command, and then merges that fetch into the current branch. */
    public void pullCommand() throws IOException {
        fetchCommand();
        mergeC(_rName + "/" + _rBranch);
        saveAll();
        saveAllRemote();
    }

    /** Remove the file either in stage for addition or the current commit. */
    public void rmCommand() {
        checkOperands(2);
        readAll();
        removeFile(_command[1]);
        saveAll();
    }

    /** Remove FILENAME in the current branch or commit. */
    public void removeFile(String fileName) {
        File workingFile = Utils.join(CWD, fileName);
        if (_stageAdd.containsKey(fileName)) {
            _stageAdd.remove(fileName);
        } else if (_currCommit.checkTrackedName(fileName)) {
            _stageRemove.add(fileName);
            if (workingFile.exists()) {
                workingFile.delete();
            }
        } else {
            throw new GitletException("No reason to remove the file.");
        }
    }

    /** Deletes the branch with the given name. */
    public void rmBranchCommand() {
        checkOperands(2);
        readAll();
        String rmBranch = _command[1];
        if (!_branches.containsKey(rmBranch)) {
            throw new GitletException("A branch with that "
                    + "name does not exist.");
        } else if (_currBranch.equals(rmBranch)) {
            throw new GitletException("Cannot remove the current branch.");
        }
        _branches.remove(rmBranch);
        saveAll();
    }

    /** Starting at the current head commit, display information about
     *  each commit backwards along the commit tree until the initial commit,
     *  following the first parent commit links, ignoring any second parents
     *  found in merge commits. */
    public void logCommand() {
        checkOperands(1);
        readAll();
        String message = _currCommit.getMessage();
        while (!message.equals("initial commit")) {
            System.out.println(_currCommit);
            _currCommit = Utils.readObject(Utils.join(COMMITTEDDATA,
                    _currCommit.getFirstParent()), Commit.class);
            message = _currCommit.getMessage();
        }
        System.out.println(_currCommit);
    }

    /** Like log, except displays information about all commits ever made.
     *  The order of the commits does not matter. */
    public void gLogCommand() {
        checkOperands(1);
        List<String> allCommits = Utils.plainFilenamesIn(COMMITTEDDATA);
        for (String file: allCommits) {
            Commit eachCommit = Utils.readObject(
                    Utils.join(COMMITTEDDATA, file), Commit.class);
            System.out.println(eachCommit);
        }
    }

    /**  Merges files from the given branch into the current branch. */
    public void mergeCommand() throws IOException {
        readAll();
        String givenBranch = _command[1];
        mergeC(givenBranch);
        saveAll();
    }

    /**  Merges files from the GIVENBRANCH into the current branch. */
    public void mergeC(String givenBranch) throws IOException {
        checkExceptMerge(givenBranch);
        Commit splitPoint = findSplit(givenBranch);
        Commit cBranchHead = Utils.readObject(Utils.join(COMMITTEDDATA,
                _branches.get(_currBranch)), Commit.class);
        Commit gBranchHead = Utils.readObject(Utils.join(COMMITTEDDATA,
                _branches.get(givenBranch)), Commit.class);
        if (splitPoint.getId().equals(gBranchHead.getId())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
        } else if (splitPoint.getId().equals(cBranchHead.getId())) {
            checkOut3(givenBranch);
            System.out.println("Current branch fast-forwarded.");
        } else {
            merge(givenBranch);
        }
    }

    /** A helper function for the third case of merge command,
     *  when the split point is neither the GIVENBRANCH head
     *  nor current branch head. */
    public void merge(String givenBranch) throws IOException {
        Commit split = findSplit(givenBranch);
        Commit cBranchHead = Utils.readObject(Utils.join(COMMITTEDDATA,
                _branches.get(_currBranch)), Commit.class);
        Commit gBranchHead = Utils.readObject(Utils.join(COMMITTEDDATA,
                _branches.get(givenBranch)), Commit.class);
        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        for (String fileName: allFiles) {
            File workingFile = Utils.join(CWD, fileName);
            String content = Utils.readContentsAsString(workingFile);
            if (split.checkTrackedName(fileName)
                    && !gBranchHead.checkTrackedName(fileName)) {
                Blob splitFile = Utils.readObject(Utils.join(BLOBSDATA,
                        split.getBlobId(fileName)), Blob.class);
                if (content.equals(splitFile.getContent())) {
                    removeFile(fileName);
                }
            }
            if (split.checkTrackedName(fileName)
                    && gBranchHead.checkTrackedName(fileName)
                    && cBranchHead.checkTrackedName(fileName)) {
                Blob vSplit = Utils.readObject(Utils.join(BLOBSDATA,
                        split.getBlobId(fileName)), Blob.class);
                Blob vGiven = Utils.readObject(Utils.join(BLOBSDATA,
                        gBranchHead.getBlobId(fileName)), Blob.class);
                Blob vCurr = Utils.readObject(Utils.join(BLOBSDATA,
                        cBranchHead.getBlobId(fileName)), Blob.class);
                if (vCurr.getContent().equals(vSplit.getContent())
                        && !vGiven.getContent().equals(vSplit.getContent())) {
                    checkOut2(gBranchHead.getId(), fileName);
                    add(fileName);
                }
            }
        }
        for (String blob: gBranchHead.getBlobs().keySet()) {
            File workingFile = Utils.join(CWD, blob);
            if (!split.checkTrackedName(blob) && !workingFile.exists()
                    && !cBranchHead.checkTrackedName(blob)) {
                checkOut2(gBranchHead.getId(), blob);
                add(blob);
            }
        }
        Boolean conflict = conflict1(givenBranch)
                || conflict2(givenBranch) || conflict3(givenBranch);
        _command = new String[]{"commit", String.format("Merged %s into %s.",
                givenBranch, _currBranch)};
        _secondParent = _branches.get(givenBranch);
        commit();
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** conflict case 1: the contents in the current and GIVENBRANCH
     *  are changed and different from other.
     *  Return whether meet a conflict. */
    public boolean conflict1(String givenBranch) throws IOException {
        boolean conflict = false;
        Commit split = findSplit(givenBranch);
        Commit cBranchHead = Utils.readObject(Utils.join(COMMITTEDDATA,
                _branches.get(_currBranch)), Commit.class);
        Commit gBranchHead = Utils.readObject(Utils.join(COMMITTEDDATA,
                _branches.get(givenBranch)), Commit.class);
        for (String fileName: split.getBlobs().keySet()) {
            if (cBranchHead.checkTrackedName(fileName)
                    && gBranchHead.checkTrackedName(fileName)) {
                Blob inCurrent = Utils.readObject(Utils.join(BLOBSDATA,
                        cBranchHead.getBlobId(fileName)), Blob.class);
                Blob inGiven = Utils.readObject(Utils.join(BLOBSDATA,
                        gBranchHead.getBlobId(fileName)), Blob.class);
                Blob inSplit = Utils.readObject(Utils.join(BLOBSDATA,
                        split.getBlobId(fileName)), Blob.class);
                String contInC = inCurrent.getContent();
                String contInG = inGiven.getContent();
                String contInS = inSplit.getContent();
                if (!contInC.equals(contInS) && !contInG.equals(contInS)
                        && !contInC.equals(contInG)) {
                    conflict = true;
                    File workingFile = Utils.join(CWD, fileName);
                    String result = "<<<<<<< HEAD" + N + contInC + "======="
                            + N + contInG + ">>>>>>>" + N;
                    Utils.writeContents(workingFile, result);
                    add(fileName);
                }
            }
        }
        return conflict;
    }

    /** conflict case 2: the contents of one are changed
     *  and the other file is deleted. According to GIVENBRANCH.
     *  Return whether meet a conflict. */
    public boolean conflict2(String givenBranch) throws IOException {
        boolean conflict = false;
        Commit split = findSplit(givenBranch);
        Commit cBranchHead = Utils.readObject(Utils.join(COMMITTEDDATA,
                _branches.get(_currBranch)), Commit.class);
        Commit gBranchHead = Utils.readObject(Utils.join(COMMITTEDDATA,
                _branches.get(givenBranch)), Commit.class);
        for (String fileName: split.getBlobs().keySet()) {
            File workingFile = Utils.join(CWD, fileName);
            String contInC = "";
            String contInG = "";
            if (cBranchHead.checkTrackedName(fileName)
                    && !gBranchHead.checkTrackedName(fileName)) {
                Blob inCurrent = Utils.readObject(Utils.join(BLOBSDATA,
                        cBranchHead.getBlobId(fileName)), Blob.class);
                Blob inSplit = Utils.readObject(Utils.join(BLOBSDATA,
                        split.getBlobId(fileName)), Blob.class);
                if (!inCurrent.getContent().equals(inSplit.getContent())) {
                    conflict = true;
                    contInC = Utils.readObject(Utils.join(BLOBSDATA,
                            cBranchHead.getBlobId(fileName)),
                            Blob.class).getContent();
                    String result = "<<<<<<< HEAD" + N + contInC + "======="
                            + N + contInG + ">>>>>>>" + N;
                    Utils.writeContents(workingFile, result);
                    add(fileName);
                }
            }
            if (gBranchHead.checkTrackedName(fileName)
                    && !cBranchHead.checkTrackedName(fileName)) {
                Blob inGiven = Utils.readObject(Utils.join(BLOBSDATA,
                        gBranchHead.getBlobId(fileName)), Blob.class);
                Blob inSplit = Utils.readObject(Utils.join(BLOBSDATA,
                        split.getBlobId(fileName)), Blob.class);
                if (!inGiven.getContent().equals(inSplit.getContent())) {
                    conflict = true;
                    workingFile.createNewFile();
                    contInG = Utils.readObject(Utils.join(BLOBSDATA,
                            gBranchHead.getBlobId(fileName)),
                            Blob.class).getContent();
                    String result = "<<<<<<< HEAD" + N + contInC + "======="
                            + N + contInG + ">>>>>>>" + N;
                    Utils.writeContents(workingFile, result);
                    add(fileName);
                }
            }
        }
        return conflict;
    }

    /** conflict case 3: the file was absent at the split point
     *  and has different contents in the GIVENBRANCH and current branches.
     *  Return whether meet a conflict.*/
    public boolean conflict3(String givenBranch) throws IOException {
        boolean conflict = false;
        Commit split = findSplit(givenBranch);
        Commit cBranchHead = Utils.readObject(Utils.join(COMMITTEDDATA,
                _branches.get(_currBranch)), Commit.class);
        Commit gBranchHead = Utils.readObject(Utils.join(COMMITTEDDATA,
                _branches.get(givenBranch)), Commit.class);
        for (String fileName: cBranchHead.getBlobs().keySet()) {
            File workingFile = Utils.join(CWD, fileName);
            if (gBranchHead.checkTrackedName(fileName)
                    && !split.checkTrackedName(fileName)) {
                Blob inCurrent = Utils.readObject(Utils.join(BLOBSDATA,
                        cBranchHead.getBlobId(fileName)), Blob.class);
                Blob inGiven = Utils.readObject(Utils.join(BLOBSDATA,
                        gBranchHead.getBlobId(fileName)), Blob.class);
                String contInC = inCurrent.getContent();
                String contInG = inGiven.getContent();
                if (!contInC.equals(contInG)) {
                    conflict = true;
                    String result = "<<<<<<< HEAD" + N + contInC + "======="
                            + N + contInG + ">>>>>>>" + N;
                    Utils.writeContents(workingFile, result);
                    add(fileName);
                }
            }
        }
        return conflict;
    }

    /** Check all the failure cases for merge command according to
     *  GIVENBRANCH. */
    public void checkExceptMerge(String givenBranch) {
        if (_command[0].equals("pull")) {
            checkOperands(3);
        } else {
            checkOperands(2);
        }
        if (!_stageAdd.isEmpty() || !_stageRemove.isEmpty()) {
            throw new GitletException("You have uncommitted changes.");
        } else if (!_branches.containsKey(givenBranch)) {
            throw new GitletException("A branch with that "
                    + "name does not exist.");
        } else if (_currBranch.equals(givenBranch)) {
            throw new GitletException("Cannot merge a branch with itself.");
        }
        Commit gCommit = Utils.readObject(Utils.join(COMMITTEDDATA,
                _branches.get(givenBranch)), Commit.class);
        checkUntracked(gCommit, CWD, _currCommit);
    }

    /** Return the id of point or latest common ancestor with GIVENBRANCH. */
    public Commit findSplit(String givenBranch) {
        HashMap<String, Integer> currAncestors =
                allAncestors(_branches.get(_currBranch), 0);
        HashMap<String, Integer> givenAncestors =
                allAncestors(_branches.get(givenBranch), 0);
        Set<String> commonAncestors = currAncestors.keySet();
        commonAncestors.retainAll(givenAncestors.keySet());
        String split = "";
        int distance = Integer.MAX_VALUE;
        for (Object ancestor: commonAncestors) {
            if (currAncestors.get(ancestor) <= distance) {
                distance = currAncestors.get(ancestor);
                split = (String) ancestor;
            }
        }
        Commit splitPoint = Utils.readObject(Utils.join(COMMITTEDDATA, split),
                Commit.class);
        return splitPoint;
    }

    /** Return all ancestors of BRANCH commit id as a HashMap<String, Integer>
     *  which has commit id as key and shortest distance to BRANCH as value.
     *  K is the count carrier to record the accumulative distance.
     */
    public HashMap<String, Integer> allAncestors(String branch, int k) {
        HashMap<String, Integer> result = new HashMap<>();
        if (branch == null) {
            return result;
        }
        Commit curr = Utils.readObject(Utils.join(COMMITTEDDATA, branch),
                Commit.class);
        if (curr.getMessage().equals("initial commit")) {
            result.put(branch, k);
            return result;
        } else {
            HashMap<String, Integer> fAncestors =
                    allAncestors(curr.getFirstParent(), k + 1);
            HashMap<String, Integer> sAncestors =
                    allAncestors(curr.getSecondParent(), k + 1);
            fAncestors.forEach(
                (key, value) -> sAncestors.merge(key, value,
                    (v1, v2) -> v1 >= v2 ? v2 : v1));
            sAncestors.put(branch, k);
            return sAncestors;
        }
    }

    /** Checks out all the files tracked by the given commit. */
    public void resetCommand() throws IOException {
        readAll();
        checkOperands(2);
        String id = _command[1];
        Commit target = findCommit(id, COMMITTEDDATA);
        checkUntracked(target, CWD, _currCommit);
        removeTracked(target, CWD, _currCommit);
        for (String fileName: target.getBlobs().keySet()) {
            Blob b = Utils.readObject(Utils.join(BLOBSDATA,
                    target.getBlobId(fileName)), Blob.class);
            File workingFile = Utils.join(CWD, fileName);
            if (!workingFile.exists()) {
                workingFile.createNewFile();
            }
            Utils.writeContents(workingFile, b.getContent());
        }
        _head = target.getId();
        _branches.replace(_currBranch, target.getId());
        _stageRemove.clear();
        _stageAdd.clear();
        saveAll();
    }



    /** Displays what branches currently exist, and marks the current branch
     *  with a *. Also displays what files have been staged for addition or
     *  removal.
     *  "Modified but not staged" and "Untracked Files". */
    public void statusCommand() {
        checkOperands(1); readAll();
        System.out.println("=== Branches ===");
        String[] branches = _branches.keySet().toArray(new String[]{});
        Arrays.sort(branches);
        for (String branch: branches) {
            if (branch.equals(_currBranch)) {
                System.out.println(String.format("*%s", branch));
            } else {
                System.out.println(branch);
            }
        }
        System.out.println("\n" + "=== Staged Files ===");
        String[] stageAd =  _stageAdd.keySet().toArray(new String[]{});
        Arrays.sort(stageAd);
        for (String fileName: stageAd) {
            System.out.println(fileName);
        }
        System.out.println("\n" + "=== Removed Files ===");
        String[] stageRe = _stageRemove.toArray(new String[]{});
        Arrays.sort(stageRe);
        for (String fileName: stageRe) {
            System.out.println(fileName);
        }
        extraStatus();
        saveAll();
    }

    /** Extra credits part of the status command. */
    public void extraStatus() {
        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        System.out.println("\n" + "=== Modifications "
                + "Not Staged For Commit ===");
        ArrayList<String> modifiedFiles = new ArrayList<>();
        for (String fileName: allFiles) {
            String cont = Utils.readContentsAsString(Utils.join(CWD, fileName));
            if (_currCommit.checkTrackedName(fileName)) {
                File blobInCommit = Utils.join(BLOBSDATA,
                        _currCommit.getBlobId(fileName));
                if (!Utils.readObject(blobInCommit,
                        Blob.class).getContent().equals(cont)
                        && !_stageAdd.containsKey(fileName)) {
                    modifiedFiles.add(fileName + " (modified)");
                }
            } else if (_stageAdd.containsKey(fileName)) {
                Blob blobInAdd = Utils.readObject(Utils.join(BLOBSDATA,
                        _stageAdd.get(fileName)), Blob.class);
                if (!blobInAdd.getContent().equals(cont)) {
                    modifiedFiles.add(fileName + " (modified)");
                }
            }
        }
        for (String fileName: _stageAdd.keySet()) {
            if (!Utils.join(CWD, fileName).exists()) {
                modifiedFiles.add(fileName + " (deleted)");
            }
        }
        for (String fileName: _currCommit.getBlobs().keySet()) {
            if (!_stageRemove.contains(fileName)
                    && !Utils.join(CWD, fileName).exists()) {
                modifiedFiles.add(fileName + " (deleted)");
            }
        }
        String[] sorted = modifiedFiles.toArray(new String[]{});
        Arrays.sort(sorted);
        for (String elem: sorted) {
            System.out.println(elem);
        }
        System.out.println("\n" + "=== Untracked Files ===");
        for (String fileName: allFiles) {
            if (!_stageAdd.containsKey(fileName)
                    && !_currCommit.checkTrackedName(fileName)) {
                System.out.println(fileName);
            }
        }
    }

    /** Prints out the ids of all commits that have the given commit message,
     *  one per line. If there are multiple such commits, it prints the ids
     *  out on separate lines. */
    public void findCommand() {
        checkOperands(2);
        readAll();
        Boolean flag = false;
        String message = _command[1];
        List<String> allCommits = Utils.plainFilenamesIn(COMMITTEDDATA);
        for (String file: allCommits) {
            Commit eachCommit = Utils.readObject(
                    Utils.join(COMMITTEDDATA, file), Commit.class);
            if (eachCommit.getMessage().equals(message)) {
                System.out.println(eachCommit.getId());
                flag = true;
            }
        }
        if (!flag) {
            throw new GitletException("Found no commit with that message.");
        }
    }

    /** Checkout is a kind of general command that can do a few different things
     *  depending on what its arguments are. There are 3 possible use cases. */
    public void checkoutCommand() throws IOException {
        readAll();
        if (_command.length == 3) {
            checkOperands(3);
            checkOut1(_command[2]);
        } else if (_command.length == 4) {
            checkOperands(4);
            checkOut2(_command[1], _command[3]);
        } else if (_command.length == 2) {
            checkOperands(2);
            checkOut3(_command[1]);
        } else {
            throw new GitletException("Incorrect operands.");
        }
        saveAll();
    }

    /** Takes the version of the FILENAME as it exists in the head commit,
     *  and puts it in the working directory. */
    public void checkOut1(String fileName) throws IOException {
        if (!_currCommit.checkTrackedName(fileName)) {
            throw new GitletException("File does not exist in that commit.");
        }
        String blobName = _currCommit.getBlobId(fileName);
        Blob fileInCommit = Utils.readObject(
                Utils.join(BLOBSDATA, blobName), Blob.class);
        File workingFile = Utils.join(CWD, fileName);
        if (!workingFile.exists()) {
            workingFile.createNewFile();
        }
        Utils.writeContents(workingFile, fileInCommit.getContent());
    }

    /** Takes the version of the FILENAME as it exists in the commit with
     *  the given ID, and puts it in the working directory. */
    public void checkOut2(String id, String fileName) throws IOException {
        Commit target = findCommit(id, COMMITTEDDATA);
        if (!target.checkTrackedName(fileName)) {
            throw new GitletException("File does not exist in that commit.");
        }
        String blobName = target.getBlobId(fileName);
        Blob fileInCommit = Utils.readObject(
                Utils.join(BLOBSDATA, blobName), Blob.class);
        File workingFile = Utils.join(CWD, fileName);
        if (!workingFile.exists()) {
            workingFile.createNewFile();
        }
        Utils.writeContents(workingFile, fileInCommit.getContent());
    }

    /** Takes all files in the commit at the head of TARGETBRANCH,
     *  and puts them in the working directory, overwriting the versions
     *  of the files that are already there if they exist. */
    public void checkOut3(String targetBranch) throws IOException {
        if (!_branches.containsKey(targetBranch)) {
            throw new GitletException("No such branch exists.");
        } else if (targetBranch.equals(_currBranch)) {
            throw new GitletException("No need to "
                    + "checkout the current branch.");
        }
        Commit target = Utils.readObject(Utils.join(COMMITTEDDATA,
                _branches.get(targetBranch)), Commit.class);
        checkUntracked(target, CWD, _currCommit);
        removeTracked(target, CWD, _currCommit);
        _currBranch = targetBranch;
        _head = _branches.get(targetBranch);
        _stageAdd.clear();
        _stageRemove.clear();
        for (String fileName: target.getBlobs().keySet()) {
            Blob b = Utils.readObject(Utils.join(BLOBSDATA,
                    target.getBlobId(fileName)), Blob.class);
            File workingFile = Utils.join(CWD, fileName);
            if (!workingFile.exists()) {
                workingFile.createNewFile();
            }
            Utils.writeContents(workingFile, b.getContent());
        }
    }

    /** Creates a new branch with the given name, and points it
     *  at the current head node. */
    public void branchCommand() {
        checkOperands(2);
        readAll();
        String newBranch = _command[1];
        if (_branches.containsKey(newBranch)) {
            throw new GitletException("A branch with "
                    + "that name already exists.");
        }
        _branches.put(newBranch, _head);
        saveAll();
    }
    /** Saves a snapshot of certain files in the current commit and staging area
     *  so they can be restored at a later time, creating a new commit. */
    public void commitCommand() throws IOException {
        checkOperands(2);
        readAll();
        commit();
        saveAll();
    }


    /** Saves a snapshot of certain files in the current commit and staging area
     *  so they can be restored at a later time, creating a new commit. */
    public void commit() throws IOException {
        if (_stageAdd.isEmpty() && _stageRemove.isEmpty()) {
            throw new GitletException("No changes added to the commit.");
        }
        HashMap<String, String> currTracked = _currCommit.getBlobs();
        currTracked.putAll(_stageAdd.getMap());
        for (String fileName: _stageRemove.getArrayList()) {
            currTracked.remove(fileName);
        }
        for (String fileName: currTracked.keySet()) {
            File workingFile = Utils.join(CWD, fileName);
            if (!workingFile.exists()) {
                currTracked.remove(fileName);
            }
            Blob fileInCommit = Utils.readObject(Utils.join(BLOBSDATA,
                    currTracked.get(fileName)), Blob.class);
            if (!Utils.readContentsAsString(workingFile).equals(
                    fileInCommit.getContent())) {
                Blob newFile = new Blob(workingFile);
                newFile.save();
                currTracked.replace(fileName, newFile.getId());
            }
        }
        _stageRemove.clear();
        _stageAdd.clear();
        Commit newCommit = new Commit(_command[1],
                currTracked, _head, _secondParent);
        newCommit.save();
        _head = newCommit.getId();
        _branches.replace(_currBranch, _head);
    }

    /** Add the working file to staging area so that we can track it by next
     *  new commit. */
    public void addCommand() throws IOException {
        checkOperands(2);
        readAll();
        String fileName = _command[1];
        add(fileName);
        saveAll();
    }

    /** Add FILENAME to staging area so that we can track it by next
     *  new commit. */
    public void add(String fileName) throws IOException {
        File addFile = Utils.join(CWD, fileName);
        if (!addFile.exists()) {
            throw new GitletException("File does not exist.");
        }
        Blob addBlob = new Blob(addFile);
        if (_stageAdd.containsKey(fileName)) {
            _stageAdd.replace(fileName, addBlob.getId());
        } else if (_currCommit.checkTrackedName(fileName)) {
            File workingFile = Utils.join(BLOBSDATA,
                    _currCommit.getBlobId(fileName));
            Blob fileInCommit = Utils.readObject(workingFile, Blob.class);
            if (fileInCommit.getId().equals(addBlob.getId())) {
                if (_stageAdd.containsKey(fileName)) {
                    _stageAdd.remove(fileName);
                } else if (_stageRemove.contains(fileName)) {
                    _stageRemove.remove(fileName);
                }
            } else {
                Blob newFile = new Blob(workingFile);
                newFile.save();
                _stageAdd.put(fileName, newFile.getId());
            }
        } else {
            _stageAdd.put(fileName, addBlob.getId());
        }
        if (_stageRemove.contains(fileName)) {
            _stageRemove.remove(fileName);
        }
        addBlob.save();
    }

    /** Creates a new Gitlet version-control system in the current directory. */
    public void initCommand() throws IOException {
        checkOperands(1);
        if (GITLET.exists()) {
            throw new GitletException("Gitlet version-control system "
                    + "already exists in the current directory.");
        }
        initialize();
        Commit initial = new Commit("initial commit",
                new HashMap<>(), null, null);
        initial.initTime(); initial.save();
        _initCommit = initial.getId();
        _head = initial.getId();
        _branches = new HHHashMap(new HashMap<>());
        _stageRemove = new AAArrayList(new ArrayList<>());
        _stageAdd = new HHHashMap(new HashMap<>());
        _remoteInfo = new RemoteMap(new HashMap<>());
        _branches.put("master", _head);
        _currBranch = "master";
        saveAll();
    }

    /** Return the target commit according to commit ID in COMMITS. */
    public Commit findCommit(String id, File commits) {
        List<String> allCommits = Utils.plainFilenamesIn(commits);
        Boolean found = false;
        String target = "";
        for (String commit: allCommits) {
            if (commit.indexOf(id) == 0) {
                if (found) {
                    throw new GitletException("Repeated commit id.");
                }
                found = true;
                target = commit;
            }
        }
        if (!found) {
            throw new GitletException("No commit with that id exists.");
        }
        Commit fCommit = Utils.readObject(Utils.join(commits,
                target), Commit.class);
        return fCommit;
    }

    /** Check if a file in DIR is untracked in the CURRENT
     *  and would be overwritten by the given commit TARGET. */
    public void checkUntracked(Commit target, File dir, Commit current) {
        List<String> allFiles = Utils.plainFilenamesIn(dir);
        for (String fileName: allFiles) {
            if (!current.checkTrackedName(fileName)
                    && target.checkTrackedName(fileName)) {
                throw new GitletException("There is an untracked file"
                        + "in the way delete it, or add and commit it first.");
            }
        }
    }

    /** Removes tracked files that are not present in TARGET commit
     *  but tracked in CURRENT commit in DIR directory. */
    public void removeTracked(Commit target, File dir, Commit current) {
        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        for (String fileName: allFiles) {
            if (current.checkTrackedName(fileName)
                    && !target.checkTrackedName(fileName)) {
                Utils.join(dir, fileName).delete();
            }
        }
    }

    /** Check if the number of operands in _command equals to K. */
    public void checkOperands(int k) {
        if (_command.length != k) {
            if (_command[0].equals("commit")) {
                throw new GitletException("Please enter a commit message.");
            } else {
                throw new GitletException("Incorrect operands.");
            }
        } else if (_command[0].equals("commit") && _command[1].length() == 0) {
            throw new GitletException("Please enter a commit message.");
        } else if (_command[0].equals("checkout")
                && k == 4 && !_command[2].equals("--")) {
            throw new GitletException("Incorrect operands.");
        }
    }

    /** Initialize all necessary files and directories. */
    public static void initialize() throws IOException {
        GITLET.mkdir();
        CONFIG.mkdir();
        STAGEAREA.mkdir();
        COMMITTEDDATA.mkdir();
        BLOBSDATA.mkdir();
        Utils.join(CONFIG, "head").createNewFile();
        Utils.join(CONFIG, "currBranch").createNewFile();
        Utils.join(CONFIG, "branches").createNewFile();
        Utils.join(CONFIG, "remoteInfo").createNewFile();
        Utils.join(CONFIG, "initCommit").createNewFile();
        Utils.join(STAGEAREA, "stageAdd").createNewFile();
        Utils.join(STAGEAREA, "stageRemove").createNewFile();
    }

    /** Read in all configuration files in remote directory. */
    public void readAllRemote() {
        _rName = _command[1];
        _rBranch = _command[2];
        _rDir = _remoteInfo.get(_rName);
        _rConfig = Utils.join(_rDir, "config");
        _rCommits = Utils.join(_rDir, "committedData");
        _rBlobs = Utils.join(_rDir, "blobsData");
        _rStage = Utils.join(_rConfig, "stageArea");
        if (!_rDir.exists()) {
            throw new GitletException("Remote directory not found.");
        }
        _rBranches = Utils.readObject(Utils.join(_rConfig, "branches"),
                HHHashMap.class);
        if (!_rBranches.containsKey(_rBranch)) {
            throw new GitletException("That remote does not have that branch.");
        }
        _rBranchHead = Utils.readObject(Utils.join(_rCommits,
                _rBranches.get(_rBranch)), Commit.class);
        _rHead = Utils.readObject(Utils.join(_rConfig, "head"), String.class);
        _rCurrCommit = Utils.readObject(Utils.join(_rCommits, _rHead),
                Commit.class);
        _rCurrBranch = Utils.readObject(Utils.join(_rConfig, "currBranch"),
                String.class);
        _rStageAdd = Utils.readObject(Utils.join(_rStage, "stageAdd"),
                HHHashMap.class);
        _rStageRemove = Utils.readObject(Utils.join(_rStage, "stageRemove"),
                AAArrayList.class);

    }
    /** Read in all configuration files. */
    public void readAll() {
        _head = Utils.readObject(Utils.join(CONFIG, "head"), String.class);
        _currCommit = Utils.readObject(Utils.join(COMMITTEDDATA, _head),
                Commit.class);
        _currBranch = Utils.readObject(Utils.join(CONFIG, "currBranch"),
                String.class);
        _branches = Utils.readObject(Utils.join(CONFIG, "branches"),
                HHHashMap.class);
        _remoteInfo = Utils.readObject(Utils.join(CONFIG, "remoteInfo"),
                RemoteMap.class);
        _stageAdd = Utils.readObject(Utils.join(STAGEAREA, "stageAdd"),
                HHHashMap.class);
        _stageRemove = Utils.readObject(Utils.join(STAGEAREA, "stageRemove"),
               AAArrayList.class);
        _initCommit = Utils.readObject(Utils.join(CONFIG, "initCommit"),
                String.class);
    }

    /** Save all configuration files. */
    public void saveAll() {
        Utils.writeObject(Utils.join(CONFIG, "head"), _head);
        Utils.writeObject(Utils.join(CONFIG, "currBranch"), _currBranch);
        Utils.writeObject(Utils.join(CONFIG, "branches"), _branches);
        Utils.writeObject(Utils.join(CONFIG, "remoteInfo"), _remoteInfo);
        Utils.writeObject(Utils.join(CONFIG, "initCommit"), _initCommit);
        Utils.writeObject(Utils.join(STAGEAREA, "stageAdd"), _stageAdd);
        Utils.writeObject(Utils.join(STAGEAREA, "stageRemove"), _stageRemove);
    }

    /** Save all configuration files in Remote. */
    public void saveAllRemote() {
        Utils.writeObject(Utils.join(_rConfig, "head"), _rHead);
        Utils.writeObject(Utils.join(_rConfig, "currBranch"), _rCurrBranch);
        Utils.writeObject(Utils.join(_rConfig, "branches"), _rBranches);
        Utils.writeObject(Utils.join(_rStage, "stageAdd"), _rStageAdd);
        Utils.writeObject(Utils.join(_rStage, "stageRemove"), _rStageRemove);
    }

    /** The working directory. */
    private static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet repository. */
    private static final File GITLET = Utils.join(CWD, ".gitlet");

    /** The repository of all configurations like head, currBranch. */
    private static final File CONFIG = Utils.join(GITLET, "config");

    /** The repository of all configurations like head, currBranch. */
    private static final File STAGEAREA = Utils.join(CONFIG, "stageArea");

    /** The directory where stores all Commit objects. */
    private static final File COMMITTEDDATA = Utils.join(GITLET,
            "committedData");

    /** The repository of all Blobs. */
    private static final File BLOBSDATA = Utils.join(GITLET, "blobsData");

    /** head commit of remote branch. */
    private Commit _rBranchHead;

    /** Stores remote login information.
     *  Remote names are keys. Remote path are values. */
    private RemoteMap _remoteInfo;

    /** The current commit. */
    private Commit _currCommit;

    /** The string SHA1 id of the commit that currently we are. */
    private String _head;

    /** The HashMap of a branch name and the SHA1 of the current head of
     *  each branch. */
    private HHHashMap _branches;

    /** The current branch we are. Also the branch of head. */
    private String _currBranch;

    /** A HashMap of the file name and the SHA1 of blobs
     *  that should be tracked in a new commit. */
    private HHHashMap _stageAdd;

    /** A HashMap that stores the file name and the SHA1 of blobs
     *  that should not be tracked in new commits as values. */
    private AAArrayList _stageRemove;

    /** Return _command. */
    public String[] getCommand() {
        return _command;
    }

    /** remote stage area. */
    private File _rStage;

    /** Remote name. */
    private String _rName;

    /** Remote branch name. */
    private String _rBranch;

    /** Remote working directory. */
    private File _rDir;

    /** Remote configuation directory. */
    private File _rConfig;

    /** Remote commits directory. */
    private File _rCommits;

    /** Remote blobs directory. */
    private File _rBlobs;

    /** Remote current branch. */
    private String _rCurrBranch;

    /** Remote current commit. */
    private Commit _rCurrCommit;

    /** Remote stage for removal. */
    private AAArrayList _rStageRemove;

    /** Remote stage for addition. */
    private HHHashMap _rStageAdd;

    /** Remote branches. */
    private HHHashMap _rBranches;

    /** Remote head. */
    private String _rHead;

    /** The command passed in Main class. */
    private String[] _command;

    /** Start a new line. */
    private static final String N = System.lineSeparator();

    /** A file separator in the path. */
    private static final String S = System.getProperty("file.separator");

    /** The id of second parent for merge. */
    private String _secondParent;

    /** The id of the inital commit. */
    private static String _initCommit;
}
