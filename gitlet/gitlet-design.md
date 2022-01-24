# Gitlet Design Document
**Name**: Wenhao Pan  
**SID**: 3034946058
## Classes and Data Structures
###Objects:
#### *Commit* -
+ The *commit* object contains file (blob) references of its files, parent reference, log message, and commit time. 
(A commit, therefore, will consist of a log message, timestamp, a mapping of file names to blob references, 
a parent reference, and (for merges) a second parent reference.)
+ Each commit is identified by its SHA-1 id. Then we use the hash string map to build the tree of the commits.
+ Implement *Serializable*.
+ Including all metadata and references when hashing a commit.
+ Distinguishing somehow between hashes for commits and hashes for blobs. 
A good way to do so is to hash in an extra word for each object that has one value for blobs and another for commits.

***Instance fields***:
1. HashMap *blobs*. A HashMap with key of the blobs name, and values of SHA1 (pointer) to the blobs.
2. String *firstParent*. The first parent commit in SHA1 string.
3. String *SecondParent*. The second parent commit in SHA1 string. (For merging)
4. String *message*. The log message.
5. String *time*. new Timestamp()
6. String *id*. The SHA1 id of the *commit*. 

***Instance method***:
1. toString(): to print the commit in the correct from for *log* command. Watch for 
*java.util.Date* and *java.util.Formatter*. If *secondParent* is not null(means it was merged, 
then we should add another line under the first one like "Merge: 4975af1 2c1ead1". the first seven digits of 
commits' id. 
2. Save(): Save the commit to *committedData*.
3. A series of getter().
4. getBlobId(String name): Return the id or the file name of the blob according to NAME.
5. checkTracked(String name): Return if the file NAME is tracked by the commit.
***
#### *blob* -
+ a blob object contains the content of a file. (readContent()).

**Instance fields**:
1. String *content*: the content of the file as a string.
2. String *id*: the SHA1 id of a *blob* according to *content*.

**Instance methods**:
1. A series of getter().
2. Save()
*******
###Classes:
#### Commands -
+ The class where all commands are processed. *vals* are the command passed by the main method.

***Instance fields***:
#####head-
+ The *string* SHA1 id of the commit that currently we are.
#####branches-
+ The HashMap with keys of a branch name (String) and values of the SHA1 of *commit* which is the head of each branch.
#####currBranch-
+ The string of the branch of the head commit.
#####currCommit-
+ The current Commit that we are working on.
##### *CWD* -
+ File. The current working directory, which should be represented by "**.**".
##### *GITLET* -
+ The *file* or repository that stores all the data and copies. 
##### *STAGEAREA* -
+ The *file* or repository that stores *stageAdd* and *stageRemove*.
##### *STAGEREMOVE* -
+ A Arraylist<String> that stores the file name(String) of the removed file.
+ If we **add [file name]** back, we should remove it from the field.
##### *STAGEADD* -
+ A HashMap that stores the file name(String) as keys, and the SHA1 of blobs should be tracked
in a new commit.
##### *committedData* -
+ The *file* or repository that stores all files referenced by a *commit* object.
##### *blobsData* -
+ The *file* or repository that stores all the blobs.

***Methods***:
##### void merge()
+ A helper function for the third case of merge command, when the split point is neither the given branch head
nor current branch head.
##### HashMap<String, Integer>  allAncestors()
+  Return all ancestors of BRANCH commit id as a HashMap<String, Integer>
       which has commit id as key and shortest distance to BRANCH as value.
       K is the count carrier to record the accumulative distance.
##### Commit findSplit()
+ Return the id of point or latest common ancestor.
##### void checkOperands(int k)
+ Check if the number of operands in _command equals to K.
##### static void initialize()
+ Initialize all necessary files and directories.
##### checkUntracked(Commit target)
+ Check if a working file is untracked in the current branch and would be overwritten by the given commit TARGET.
Removes tracked files that are not present in that commit.
##### removeTracked(Commit target)
+ Removes tracked files that are not present in TARGET commit.
##### void findCommit(String id)
+ Find the target commit according to commit ID.
##### Commit findSplit()
+ Find the latest common ancestor.
##### void saveAll() -
+ Save all configuration files to CONFIG. 
##### void readAll() -
+ Read in all the configuration files.
##### void processCommand() -
+ We call different methods to handle different commands.
##### void remoteCommand() -
+ We call different methods to handle different remote commands.
## Algorithms (for commands)
###init -
+ Create the first commit of "initial commit", with "master" branch, and the timestamp of 00:00:00 UTC, 
Thursday, 1 January 1970. Parent commits are *null*.
+ Create new *.gitlet*, *stageArea*, *committedData* repositories.
+ Initialize *head*, *branches*(with default *master*), *currBranch* (*master*)and *commitTree*.
+ save *head*, *branches*, *currBranch*, *commitTree*.
+ **Handling exception**: check if ".gitlet" already exists.
***
### add [file name] -
+ Create a *blob* object of *[file name]*. Read *stageAdd*.
+ Add a pair of *[file name]* (key) and SHA1 id of the *blob* we just created (value).
+ If in *stageAdd*, there is already a file with same name, overwrite the new content (writeContents()). 
+ If the working file is same as the file in current commit(name and content identity), do not stage and remove it
 from *stageAdd* if it is already existed in *stageAdd*. 
+ We save blob and *stageAdd* to *blobsData*
+ **Handling exception**: check if "file" already exists.
+ check if *file* is in *stageRemove*. If so, we remove it from *stageRemove*.
***
### commit [message] -
+ First create a new *commit*. Read *stageAdd*, *stageRemove*, and *branches*.
+ Match each instance fields of a commit object to files or data in the current commit or *stageAdd*.
+ If current commit(*head*) contains *blob* in *stageRemove*, we do not track this blob any more.
+ In the end, we clear *stageAdd* and *stageRemove*.
+ Change the *head* pointing to the new commit.
+ Update the val of *branches* with key of *currBranch*. 
+ Save *stageAdd*, *stageRemove*, and *branches*, *head*, *commitTree*.
+ **Handling exception**: check if *stage* is empty; check if *[message]* is *null*.
***
### rm [file name] -
+ Read *stageAdd*, *stageRemove*.
+ Delete the pair of value in *stageAdd* if it exists.
+ If current commit contains the *file*, put it into *stageRemove*. At last, 
we delete this file from the *CWD*. 
+ **Handling exception**: check if file exists in *stage* or if the head commit.blobs have the key of *[file name]*.
***
### log -
+ print each commit from current head commit to the "initial commit". We ignore the commits of other branches.
***
### global-log -
+ just like log, but we print all commits, including ones in other branch.
***
### find [commit message] -
+ local variable *findFlag*: *True* if we find at least one matched commit. *False* if none is found.
+ print out the id of all commits that share the same *[commit message]*.
+ **Handling exception**: check if *findFlag* is True or not.
+ **Note**: possible repeated code with **global-log**.
***
### status -
(print in lexicographic order)
first we read in everything.
1. Print out keys of branches. Add a "*" before *currBranch*.
2. Print out names of file in *stageAdd*.
3. Print out names of file in *stageRemove*.
4. If the file in the *CWD* do not share the same SHA1 id with the blob of the same name in the
*committedData* directory, we print out its name with "(modified)".
5. If we find a file in *stage* but not in *CWD*, we print its name out with "(deleted)".
6. If a *blob* (file) in current commit is not found neither in *CWD* nor *stageRemove*, 
we print its name out with "(deleted)".
7. If a file in *CWD* is neither in *stage* nor in current *commit* as a blob, we print its name out 
under "Untracked files".
8. If a file in *stageRemove* is also in *CWD* we print it out under "Untracked files".
***
### checkout -- [file name] -
1. Read in *head*.
1. Take out the *blob*(file) in the *head commit* and put it in *CWD*. If file is already exist, overwrite it.
2. **Handling exception**: check if *file name* exists in the *head commit*
### checkout [commit id] -- [file name] -
1. Find target commit and read it in.
1. First find the commit with the same id (might be abbreviated, and if we find several commits with same
abbreviated id, we throw the error). Then we create a new file or overwrite the existed file. 
2. **Handling exception**: Check if the commit id exists; check if file name exists in the commit.
### checkout [branch name] -
local variable: *commit* furthestC - the furthest commit in *[branch mame]*.
1. First put everything in commit of *[branche name]*(as a key) in *branches* in *CWD*. If file already
exists, we overwrite it.
2. assign *[branch name]* to *currBranch*. assign furthest commit of *[branch name]* to *head*.
3. Any file in *CWD* that is not tracked in *furthestC*, we delete them. 
4. We clear the *stage* and *stageRemove* in the end if *[branche name]* not equals to *currbranch*.
5. Save *currBranch*  
5. **Handling exception**: check if *[branch name]* exists; check if *[branche name]* equals to *currbranch*;
check if a file in *CWD* is not tracked in(by) *head* but is in *furthestC*.
***
### branch [branch name] -
+ read *branches* in 
+ add a new pair into *branches*. *[branch name]* is the new key. *head* is the new value. 
+ save *branches*, *head*.
+ **Handling exception**: check if *[branch name]* already exists in keys of *branches*.
***
### rm-branch [branch name] -
+ read in *branches*, *currBranch*
+ delete a pair of keys and value according to *[branch name]*.
+ save *branches*
+ **Handling exception**: check if *[branch name]* exists in *branch*; check if *[branch name]* equals to *currBranch*.
***
### reset [commit id] -
+ Find the target commit and read it in.
+ Check out all the blobs in this commit.
+ Clear *stageAdd* and *stageRemove*
+ change head to this commit.
+ **Handling exception**: check if *[commit id] is found or not; check if a working file in *[commit id]* is in *head*
commit.
***
### merge [branch name] -
1. Find the lastest common ancestor (split point), and check if it is the same commit as the [branch name] or head of 
*_currbranch*.
2. If a working file in the given branch is different from that in the split, but it's verison in curr branch is same with
that in split, we check out that file from the head of given branch. Then we stage the file.
3. If a file is not in the split point, CWD, and current Branch, but in the given Branch should be checked out
and staged.(not a working file.)
4. If a file in the split, but same in the current branch, not in the given branch should be removed and untracked.
(a working file.)
***
### push [remote name] [remote branch name] -
1. Check if the remote branch's head is in the history of the current local head. details: First 
take out the head commit of [remote branch name]. Second traverse the current commit's history to find if there is 
a matched commit with remote branch head.
2. Then, we copy all the future commits and corresponding blobs to remote branch.
3. We reset the remote to the front of the appended commits. (reset [commit id])

### fetch [remote name] [remote branch name] -
1. Check if [remote name]/[remote branch name] branch exit in the local. If it doesn't, we create a new branch pointing to 
inital commit. Then copy all commits and blobs from the [remote branch name] to local which doesn't already exist. Then change the 
[remote name]/[remote branch name] head to the head commit
## Persistence
After the first "init" initialization of gitlet, we need to create all necessary repositories 
and save the first "initial commit".
Many other saving and loading details are written in the *Algorithm*.

