package git4idea.commands;
/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 *
 * Copyright 2007 Decentrix Inc
 * Copyright 2007 Aspiro AS
 * Copyright 2008 MQSoftware
 * Authors: gevession, Erlend Simonsen & Mark Scott
 *
 * This code was originally derived from the MKS & Mercurial IDEA VCS plugins
 */

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.EnvironmentUtil;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.GitBranch;
import git4idea.GitContentRevision;
import git4idea.GitFileAnnotation;
import git4idea.GitFileRevision;
import git4idea.GitRevisionNumber;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.GitVcsSettings;
import git4idea.GitVirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Worker class for executing Git system commands.
 */
@SuppressWarnings({"ResultOfMethodCallIgnored"})
public class GitCommand {
    public final static boolean DEBUG = false;
    public static final int BUF_SIZE = 16 * 1024;  // 16KB
    public static final int MAX_BUF_ALLOWED = 128 * 1024 * 1024; //128MB (who'll ever need to edit a file that big??? :-)
    public static final String EMPTY_STRING = "";
    /* Git/VCS commands */
    private static final String ADD_CMD = "add";
    private static final String ANNOTATE_CMD = "annotate";
    private static final String BRANCH_CMD = "branch";
    public static final String CHECKOUT_CMD = "checkout";
    public static final String CLONE_CMD = "clone";
    private static final String COMMIT_CMD = "commit";
    private static final String CONFIG_CMD = "config";
    private static final String DELETE_CMD = "rm";
    private static final String DIFF_CMD = "diff";
    public static final String FETCH_CMD = "fetch";
    private static final String GC_CMD = "gc";
    private static final String LOG_CMD = "log";
    public static final String MERGE_CMD = "merge";
    public static final String MOVE_CMD = "mv";
    public static final String PULL_CMD = "pull";
    public static final String PUSH_CMD = "push";
    private static final String REBASE_CMD = "rebase";
    private static final String REVERT_CMD = "checkout";
    private static final String SHOW_CMD = "show";
    public static final String TAG_CMD = "tag";
    private static final String VERSION_CMD = "version";
    public static final String STASH_CMD = "stash";
    public static final String MERGETOOL_CMD = "mergetool";
    public static final String STATUS_CMD = "ls-files";
    private static final String DIFF_TREE_CMD = "diff-tree";

    /* Misc Git constants */
    private static final String HEAD = "HEAD";
    private static final Lock gitWriteLock = new ReentrantLock();

    /* Git command env stuff */
    private Project project;
    private final GitVcsSettings settings;
    private VirtualFile vcsRoot;
    private final static String line_sep = "\n";

    private String cmd;
    private String[] opts;
    private String[] args;
    private Process proc;

    public GitCommand(@NotNull final Project project, @NotNull GitVcsSettings settings, @NotNull VirtualFile vcsRoot) {
        this.vcsRoot = vcsRoot;
        this.project = project;
        this.settings = settings;
    }

    public GitCommand(@NotNull final Project project, @NotNull GitVcsSettings settings, @NotNull VirtualFile vcsRoot,
                      String cmd, String[] opts, String[] args) {
        this.vcsRoot = vcsRoot;
        this.project = project;
        this.settings = settings;
        this.cmd = cmd;
        this.opts = opts;
        this.args = args;
    }

    public void setCommand(String cmd) {
        this.cmd = cmd;
    }

    public void setOptions(String[] opts) {
        this.opts = opts;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // General public methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the current Git version.
     *
     * @return The version string
     * @throws VcsException If an error occurs
     */
    public String version() throws VcsException {
        return execute(VERSION_CMD);
    }

    /**
     * Returns a list of all local branches
     *
     * @return A list of all the branches
     * @throws VcsException If an error occurs
     */
    public List<GitBranch> branchList() throws VcsException {
        return branchList(false);
    }

    /**
     * Returns a list of all the current branches.
     *
     * @param remoteOnly True if only remote branches should be included
     * @return A list of all the branches
     * @throws VcsException If an error occurs
     */
    public List<GitBranch> branchList(boolean remoteOnly) throws VcsException {
        ArrayList<String> args = new ArrayList<String>();
        if (remoteOnly)
            args.add("-r");
        String result = execute("branch", args, true);
        List<GitBranch> branches = new ArrayList<GitBranch>();

        BufferedReader in = new BufferedReader(new StringReader(result));
        String line;
        try {
            while ((line = in.readLine()) != null) {
                String branchName = line.trim();

                boolean active = false;
                if (branchName.startsWith("* ")) {
                    branchName = branchName.substring(2);
                    active = true;
                }

                boolean remote = branchName.contains("/");
                GitBranch branch = new GitBranch(
                        project,
                        branchName,
                        active,
                        remote);
                branches.add(branch);
            }
        }
        catch (IOException e) {
            throw new VcsException(e);
        }
        return branches;
    }

    /**
     * Returns the name of the currently active branch
     *
     * @return The branch name
     * @throws VcsException If an error occurs
     */
    public String currentBranch() throws VcsException {
        String output = execute(BRANCH_CMD, true);
        StringTokenizer lines = new StringTokenizer(output, line_sep);
        while (lines.hasMoreTokens()) {
            String line = lines.nextToken();
            if (line != null && line.startsWith("*")) {
                return line.substring(2);
            }
        }

        return "master";
    }

    /**
     * Returns the remote repository URL, that a specified remote branch comes from.
     *
     * @param branch The branch to query
     * @return The remote repository URL
     * @throws VcsException if an error occurs
     */
    public String remoteRepoURL(GitBranch branch) throws VcsException {
        String bname = branch.getName();
        if (!branch.isRemote()) return null;
        String remoteAlias = bname.substring(0, bname.indexOf("/"));

        List<String> args = new ArrayList<String>();

        args.add("--get");
        args.add("remote." + remoteAlias + ".url");

        return execute(CONFIG_CMD, args, true);
    }

    /**
     * Returns a set of all changed files under this VCS root.
     *
     * @return The set of all changed files
     * @throws VcsException If an error occurs
     */
    public Set<GitVirtualFile> changedFiles() throws VcsException {
        Set<GitVirtualFile> files = new HashSet<GitVirtualFile>();
        String output;
        List<String> args = new ArrayList<String>();
        args.add("--cached");
        args.add("--name-status");
        args.add("--diff-filter=ADMRUX");
        args.add("--");
        output = execute(DIFF_CMD, args, true);

        //TODO: tie to user config option for "Always keep Git index in sync ("git add") with every file save?"
        // Look for files not cached in the Git index yet
        // args.remove(0);
        // output += execute(DIFF_CMD, args, true);

        StringTokenizer tokenizer;
        if (output != null && output.length() > 0) {
            if (output.startsWith("null"))
                output = output.substring(4);
            tokenizer = new StringTokenizer(output, "\n");
            while (tokenizer.hasMoreTokens()) {
                final String s = tokenizer.nextToken();
                String[] larr = s.split("\t");
                if (larr.length == 2) {
                    GitVirtualFile file = new GitVirtualFile(project, getBasePath() + File.separator + larr[1], convertStatus(larr[0]));
                    files.add(file);
                }
            }
        }

        //TODO: add user config option for tracking UNVERSIONED files - much slower...
//        args.clear();
//        args.add("--others");
//        output = execute("ls-files", args, true);
//        if (output != null && output.length() > 0) {
//            tokenizer = new StringTokenizer(output, line_sep);
//            while (tokenizer.hasMoreTokens()) {
//                final String s = tokenizer.nextToken();
//                GitVirtualFile file = new GitVirtualFile(project, getBasePath() + File.separator + s.trim(), GitVirtualFile.Status.UNVERSIONED);
//                files.add(file);
//            }
//        }

        return files;
    }


    /**
     * Loads the specified revision of a file from Git.
     *
     * @param path     The path to the file.
     * @param revision The revision to load. If the revision is null, then HEAD will be loaded.
     * @return The contents of the revision as a String.
     * @throws VcsException If the load of the file fails.
     */
    public String getContents(@NotNull String path, String revision) {
        StringBuffer revCmd = new StringBuffer();
        if (revision != null) {
            if (revision.length() > 40)       // this is the date & revision-id encoded string
                revCmd.append(revision.substring(revision.indexOf("[") + 1, 40));
            else
                revCmd.append(revision);     // either 40 char revision-id or "HEAD", either way just use it
            revCmd.append(":");
        } else {
            revCmd.append(HEAD + ":");
        }

        String vcsPath = revCmd.append(getRelativeFilePath(path, vcsRoot)).toString();
        try {
            return execute(SHOW_CMD, Collections.singletonList(vcsPath), true);
        } catch (VcsException e) {
            return "";
        }
    }

    /**
     * Builds the revision history for the specifid file.
     *
     * @param filePath The path to the file.
     * @return The list.
     * @throws com.intellij.openapi.vcs.VcsException
     *          If it fails...
     */
    public List<VcsFileRevision> log(FilePath filePath) throws VcsException {
        String[] options = new String[]
                {
                        "-n25",
                        "--pretty=format:%H@@@%an <%ae>@@@%ct@@@%s",
                        "--"
                };

        String[] args = new String[]
                {
                        getRelativeFilePath(filePath.getPath(), vcsRoot)
                };

        String result = execute(LOG_CMD, options, args);

        List<VcsFileRevision> revisions = new ArrayList<VcsFileRevision>();

        // Pull the result apart...
        BufferedReader in = new BufferedReader(new StringReader(result));
        String line;
        //SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        try {
            while ((line = in.readLine()) != null) {
                if (line.length() == 0) continue;
                String[] values = line.split("@@@");
                Date commitDate = new Date(Long.valueOf(values[2]) * 1000);
                //String revstr = df.format(commitDate) + " [" + values[0] + "]";
                String revstr = values[0];
                GitFileRevision revision = new GitFileRevision(
                        project,
                        filePath,
                        new GitRevisionNumber(revstr, commitDate),// git revision id
                        values[1],                // user realname & email
                        values[3],                // commit description
                        null);                    // TODO: find branch name for the commit & pass it here
                revisions.add(revision);
            }

        }
        catch (IOException e) {
            throw new VcsException(e);
        }
        return revisions;
    }


    public Set<GitVirtualFile> virtualFiles(Set<FilePath> fpaths) throws VcsException {
        Set<GitVirtualFile> files = new HashSet<GitVirtualFile>();
        List<String> args = new ArrayList<String>();
        args.add("--name-status");
        args.add("--");

        for (FilePath fpath : fpaths) {
            args.add(getRelativeFilePath(fpath.getPath(), vcsRoot).replace("\\", "/"));
        }

        String output = execute(DIFF_CMD, args, true);

        StringTokenizer tokenizer;
        if (output != null && output.length() > 0) {
            tokenizer = new StringTokenizer(output, line_sep);
            while (tokenizer.hasMoreTokens()) {
                final String s = tokenizer.nextToken();
                String[] larr = s.split("\t");
                if (larr.length == 2) {
                    GitVirtualFile file = new GitVirtualFile(project, getBasePath() + File.separator + larr[1], convertStatus(larr[0]));
                    files.add(file);
                }
            }
        }

        return files;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Public command/action execution methods
    //////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Add the specified files to the repository
     *
     * @param files The files to add
     * @throws VcsException If an error occurs
     */
    public void add(VirtualFile[] files) throws VcsException {
        gitWriteLock.lock();
        try {
            String[] args = new String[files.length];
            int count = 0;
            for (VirtualFile file : files) {
                if (file instanceof GitVirtualFile) {   // don't try to add already deleted files...
                    GitVirtualFile gvf = (GitVirtualFile) file;
                    if (gvf.getStatus() == GitVirtualFile.Status.DELETED)
                        continue;
                }
                if (file != null)
                    args[count++] = getRelativeFilePath(file, vcsRoot);
            }

            String result = execute(ADD_CMD, (String[]) null, args);
            GitVcs.getInstance(project).showMessages(result);
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Commit the specified files to the repository
     *
     * @param files   The files to commit
     * @param message The commit message description to use
     * @throws VcsException If an error occurs
     */
    @SuppressWarnings({"EmptyCatchBlock"})
    public void commit(VirtualFile[] files, String message) throws VcsException {
        gitWriteLock.lock();
        try {
            StringBuffer commitMessage = new StringBuffer();
            StringTokenizer tok = new StringTokenizer(message, "\n");
            while (tok.hasMoreTokens()) {
                String line = tok.nextToken();
                if (line == null || line.startsWith("#")) // eat all comment lines
                    continue;
                commitMessage.append(line).append("\n");
            }

            String[] options = null;

            try {
                File temp = File.createTempFile("git-commit-msg", ".txt");
                temp.deleteOnExit();
                BufferedWriter out = new BufferedWriter(new FileWriter(temp));
                out.write(commitMessage.toString());
                out.close();
                options = new String[]{"-F", temp.getAbsolutePath()};
            } catch (IOException e) {
            }

            String[] args = new String[files.length];
            int count = 0;
            for (VirtualFile file : files) {
                if (file != null)
                    args[count++] = getRelativeFilePath(file, vcsRoot);
            }

            add(files); // add current snapshot to index first..
            String result = execute(COMMIT_CMD, options, args);  // now commit the files
            GitVcs.getInstance(project).showMessages(result);

            VcsDirtyScopeManager mgr = VcsDirtyScopeManager.getInstance(project);
            for (VirtualFile file : files) {
                if (file != null) {
                    mgr.fileDirty(file);
                    file.refresh(true, true);
                }
            }
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Delete the specified files from the repostory
     *
     * @param files The files to delete
     * @throws VcsException If an error occurs
     */
    public void delete(VirtualFile[] files) throws VcsException {
        gitWriteLock.lock();
        try {
            String[] args = new String[files.length];
            String[] opts = {"-f"};
            int count = 0;
            for (VirtualFile file : files) {
                if (file != null)
                    args[count++] = getRelativeFilePath(file, vcsRoot);
            }

            String result = execute(DELETE_CMD, opts, args);
            GitVcs.getInstance(project).showMessages(result);
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Checkout the specified branch & create branch if necessary.
     *
     * @param selectedBranch The branch to checkout
     * @param createBranch   True if the branch should be created
     * @throws VcsException If an error occurs
     */
    public void checkout(String selectedBranch, boolean createBranch) throws VcsException {
        gitWriteLock.lock();
        try {
            ArrayList<String> args = new ArrayList<String>();
            if (createBranch) {
                args.add("--track");
                args.add("-b");
            }
            args.add(selectedBranch);

            String result = execute(CHECKOUT_CMD, args);
            GitVcs.getInstance(project).showMessages(result);
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Clones the repository to the specified path.
     *
     * @param src    The src repository. May be a URL or a path.
     * @param target The target directory
     * @throws VcsException If an error occurs
     */
    public void cloneRepository(String src, String target) throws VcsException {
        gitWriteLock.lock();
        try {
            String[] args = new String[]{src, target};
            String result = execute(CLONE_CMD, (String) null, args);
            GitVcs.getInstance(project).showMessages(result);
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Merge the current branch
     *
     * @throws VcsException If an error occurs
     */
    public void merge() throws VcsException {
        gitWriteLock.lock();
        try {
            String result = execute(MERGE_CMD);
            GitVcs.getInstance(project).showMessages(result);
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Move/rename a file
     *
     * @param oldFile the old file path
     * @param newFile the new file path
     * @throws VcsException If an error occurs
     */
    public void move(@NotNull VirtualFile oldFile, @NotNull VirtualFile newFile) throws VcsException {
        gitWriteLock.lock();
        try {
            String[] files = new String[]{getRelativeFilePath(oldFile.getPath(), vcsRoot),
                    getRelativeFilePath(newFile.getPath(), vcsRoot)};
            String result = execute(MOVE_CMD, files, false);
            GitVcs.getInstance(project).showMessages(result);
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Cleanup indexes & garbage collect repository
     *
     * @throws VcsException If an error occurs
     */
    public void gc() throws VcsException {
        gitWriteLock.lock();
        try {
            String result = execute(GC_CMD);
            GitVcs.getInstance(project).showMessages(result);
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Merge in the specified branch
     *
     * @param branch Teh branch to merge ito the current branch
     * @throws VcsException If an error occurs
     */
    public void merge(GitBranch branch) throws VcsException {
        gitWriteLock.lock();
        try {
            String result = execute(MERGE_CMD, branch.getName());
            GitVcs.getInstance(project).showMessages(result);
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Rebase the current repository.
     *
     * @throws VcsException If an error occurs
     */
    public void rebase() throws VcsException {
        gitWriteLock.lock();
        try {
            String result = execute(REBASE_CMD);
            GitVcs.getInstance(project).showMessages(result);
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Pull from the specified repository
     *
     * @param repoURL The repository to pull from
     * @param merge   True if the changes should be merged into the current branch
     * @throws VcsException If an error occurs
     */
    public void pull(String repoURL, boolean merge) throws VcsException {
        gitWriteLock.lock();
        try {
            String cmd;
            if (merge)
                cmd = PULL_CMD;
            else
                cmd = FETCH_CMD;

            String result = execute(cmd, repoURL);
            GitVcs.getInstance(project).showMessages(result);
            result = execute(cmd, "--tags", repoURL);
            GitVcs.getInstance(project).showMessages(result);
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Push the current branch
     *
     * @throws VcsException If an error occurs
     */
    public void push() throws VcsException {
        String result = execute(PUSH_CMD);
        GitVcs.getInstance(project).showMessages(result);
        result = execute(PUSH_CMD, "--tags");
        GitVcs.getInstance(project).showMessages(result);
    }

    /**
     * Reverts the list of files we are passed.
     *
     * @param files The array of files to revert.
     * @throws VcsException Id it breaks.
     */
    public void revert(VirtualFile[] files) throws VcsException {
        gitWriteLock.lock();
        try {
            String[] args = new String[files.length];
            String[] options = new String[]{HEAD, "--"};
            int count = 0;
            for (VirtualFile file : files) {
                if (file != null)
                    args[count++] = getRelativeFilePath(file, vcsRoot);
            }

            String result = execute(REVERT_CMD, options, args);
            GitVcs.getInstance(project).showMessages(result);
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Reverts the list of files we are passed.
     *
     * @param files The list of files to revert.
     * @throws VcsException Id it breaks.
     */
    public void revert(List<VirtualFile> files) throws VcsException {
        revert(files.toArray(new VirtualFile[files.size()]));
    }

    /**
     * Tags the current files with the specified tag.
     *
     * @param tagName The tag to use.
     * @throws VcsException If an error occurs
     */
    public void tag(String tagName) throws VcsException {
        gitWriteLock.lock();
        try {
            String result = execute(TAG_CMD, tagName);
            GitVcs.getInstance(project).showMessages(result);
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Stash all changes under the specified stash-name
     *
     * @param stashName The name of the stash
     * @throws VcsException If an error occurs
     */
    public void stash(String stashName) throws VcsException {
        gitWriteLock.lock();
        try {
            String result = execute(STASH_CMD, stashName);
            GitVcs.getInstance(project).showMessages(result);
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Un-Stash (restore) all changes under the specified stash-name
     *
     * @param stashName The name of the un-stash
     * @throws VcsException If an error occurs
     */
    public void unstash(String stashName) throws VcsException {
        gitWriteLock.lock();
        try {
            String result = execute(STASH_CMD, "apply", stashName);
            GitVcs.getInstance(project).showMessages(result);
        } finally {
            gitWriteLock.unlock();
        }
    }

    /**
     * Returns the current list of all stash names, null if none.
     *
     * @return stash list
     * @throws VcsException If an error occurs
     */
    public String[] stashList() throws VcsException {
        List<String> lines = new LinkedList<String>();

        StringTokenizer tok = new StringTokenizer(execute(STASH_CMD, new String[]{"list"}, true), "\n");
        while (tok.hasMoreTokens()) {
            lines.add(tok.nextToken());
        }

        if (lines.size() == 0) return null;
        return lines.toArray(new String[lines.size()]);
    }

    /**
     * Return true if the specified file is known to Git, otherwise false.
     *
     * @param file the file to check status of
     * @return true if Git owns the file, else false
     * @throws VcsException If an error occurs
     */
    public boolean status(VirtualFile file) throws VcsException {
        String path = getRelativeFilePath(file, GitUtil.getVcsRoot(project, file));
        String output = execute(STATUS_CMD, path);
        return !(output == null || output.length() == 0) && output.contains(path);
    }

    /**
     * Exec the git merge tool
     *
     * @param files The files to merge
     * @throws VcsException If an error occurs
     */
    public void mergetool(String[] files) throws VcsException {
        String result;
        if (files == null || files.length == 0)
            result = execute(MERGETOOL_CMD);
        else
            result = execute(MERGETOOL_CMD, (String[]) null, files);
        GitVcs.getInstance(project).showMessages(result);
    }

    /**
     * Use gitk to show revision graph for specified file.
     *
     * @param file The file to show
     * @throws VcsException if an error occurs
     */
    public void revisionGraph(VirtualFile file) throws VcsException {
        String wishcmd;
        String gitkcmd;
        File gitExec = new File(settings.GIT_EXECUTABLE);
        if (gitExec.exists()) {  // use absolute path if we can
            String sep = System.getProperty("file.separator", "\\");
            wishcmd = gitExec.getParent() + sep + "wish84";
            gitkcmd = gitExec.getParent() + sep + "gitk";
        } else {    // otherwise, assume user has $PATH setup
            wishcmd = "wish84";
            gitkcmd = "gitk";
        }

        String filename = getRelativeFilePath(file, vcsRoot);

        Process proc;
        try {
            File directory = VfsUtil.virtualToIoFile(vcsRoot);
            ProcessBuilder pb = new ProcessBuilder(wishcmd, gitkcmd, filename);
            // copy IDEA configured env into process exec env
            Map<String, String> pbenv = pb.environment();
            pbenv.putAll(EnvironmentUtil.getEnviromentProperties());

            pb.directory(directory);
            pb.redirectErrorStream(true);

            proc = pb.start();
            proc.waitFor();
        } catch (Exception e) {
            throw new VcsException(e);
        }

        if (proc.exitValue() != 0)
            throw new VcsException("Error executing gitk!");
    }

    /**
     * Builds the annotation for the specified file.
     *
     * @param filePath The path to the file.
     * @return The GitFileAnnotation.
     * @throws com.intellij.openapi.vcs.VcsException
     *          If it fails...
     */
    public GitFileAnnotation annotate(FilePath filePath) throws VcsException {
        String[] options = new String[]{"-l", "--"};
        String[] args = new String[]{getRelativeFilePath(filePath.getPath(), vcsRoot)};

        GitFileAnnotation annotation = new GitFileAnnotation(project);
        String cmdOutput = execute(ANNOTATE_CMD, options, args);
        if (cmdOutput == null || cmdOutput.length() == 0) return annotation;

        BufferedReader in = new BufferedReader(new StringReader(cmdOutput));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        String Line;
        int lineCount = 0;
        try {
            while ((Line = in.readLine()) != null) {
                String annValues[] = Line.split("\t", 4);
                if (annValues.length != 4) {
                    throw new VcsException("Framing error: unexpected number of values");
                }

                String revision = annValues[0];
                String user = annValues[1];
                String dateStr = annValues[2];
                String numberedLine = annValues[3];
                lineCount++;

                if (revision.length() != 40) {
                    throw new VcsException("Framing error: Illegal revision number: " + revision);
                }

                int idx = numberedLine.indexOf(')');
                if (!user.startsWith("(") || idx <= 0) {
                    continue;
                    //throw new VcsException("Framing error: unexpected format");
                }
                user = user.substring(1).trim(); // Ditch the (
                Long lineNumber = Long.valueOf(numberedLine.substring(0, idx));
                String lineContents = numberedLine.substring(idx + 1);

                Date date = dateFormat.parse(dateStr);
                annotation.appendLineInfo(date, new GitRevisionNumber(revision, date), user, lineContents, lineNumber);
            }

        } catch (IOException e) {
            throw new VcsException("Failed to load annotations", e);
        } catch (ParseException e) {
            throw new VcsException("Failed to load annotations", e);
        }
        return annotation;
    }

    /**
     * Builds collection of changed files for a given commit.
     *
     * @param commitId Long commit id.
     * @return Collection of changed files.
     * @throws VcsException if an error occurs
     */
    public Collection<Change> getChangesForCommit(String commitId) throws VcsException {
        final ArrayList<Change> result = new ArrayList<Change>();

        String[] options = new String[]{"-r", "--root", "--pretty=format:%P"}; // Show parent commit if it present
        String[] args = new String[]{commitId};

        String cmdOutput = execute(DIFF_TREE_CMD, options, args);
        final String[] changes = cmdOutput.split("\n");

        if (changes.length == 0) {
            return result;
        }

        GitRevisionNumber parentCommit = null;
        String parentCommitId = changes[0];
        // First line in the output should be id of parent commit. In case if this line is empty it means that commit is initial and has no any parent commit.

        // If so - then given commit could only add files, no change/move/delete allowed. Later we check that such commit has only ADDED file statuses.
        if (parentCommitId.length() > 0) {
            parentCommit = new GitRevisionNumber(parentCommitId);
        }

        for (int i = 1; i < changes.length; i++) {
            String gitChnage = changes[i];
            if (gitChnage.length() == 0)
                continue;

            // format for gitChange is following
            // :000000 100644 0000000000000000000000000000000000000000 984ca539b1c469fb2bbd6d6e26fe5fcd25ab76f1 A	src/git4idea/GitRefactoringListenerProvider.java
            final String[] tokens = gitChnage.split("[ \t]");
            assert tokens.length > 5;
            final String blogIdBefore = tokens[2];
            final String blobIdAfter = tokens[3];
            final GitVirtualFile.Status status = convertStatus(tokens[4].substring(0, 1));
            final String pathArg1 = vcsRoot.getPath() + "/" + tokens[5];
            final String pathArg2 = tokens.length > 6 ? (vcsRoot.getPath() + "/" + tokens[6]) : null;

            ContentRevision before = null;
            ContentRevision after = null;
            FileStatus fileStatus = null;

            switch (status) {
                case MODIFIED:
                    assert parentCommit != null;
                    GitVirtualFile gitFile = new GitVirtualFile(project, pathArg1);
                    before = new GitContentRevision(gitFile, parentCommit, project);
                    after = new GitContentRevision(gitFile, new GitRevisionNumber(commitId), project);
                    fileStatus = FileStatus.MODIFIED;
                    break;
                case COPY:
                case RENAME:
                    assert parentCommit != null;
                    before = new GitContentRevision(new GitVirtualFile(project, pathArg1), parentCommit, project);
                    after = new GitContentRevision(new GitVirtualFile(project, pathArg2), new GitRevisionNumber(commitId), project);
                    fileStatus = FileStatus.MODIFIED;
                    break;
                case ADDED:
                    after = new GitContentRevision(new GitVirtualFile(project, pathArg1), new GitRevisionNumber(commitId), project);
                    fileStatus = FileStatus.ADDED;
                    break;
                case DELETED:
                    assert parentCommit != null;
                    before = new GitContentRevision(new GitVirtualFile(project, pathArg1), parentCommit, project);
                    fileStatus = FileStatus.DELETED;
                    break;
            }

            result.add(new Change(before, after, fileStatus));
        }

        return result;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // Private worker & helper methods
    ////////////////////////////////////////////////////////////////////////////////////////////

    public String getRelativeFilePath(VirtualFile file, @NotNull final VirtualFile baseDir) {
        if (file == null) return null;
        return getRelativeFilePath(file.getPath(), baseDir);
    }

    public String getRelativeFilePath(String file, @NotNull final VirtualFile baseDir) {
        if (file == null) return null;
        String rfile = file.replace("\\", "/");
        final String basePath = baseDir.getPath();
        if (!rfile.startsWith(basePath)) return rfile;
        else if (rfile.equals(basePath)) return ".";
        return rfile.substring(baseDir.getPath().length() + 1);
    }

    private String execute(@NotNull String cmd, String arg) throws VcsException {
        return execute(cmd, null, arg);
    }

    private String execute(@NotNull String cmd, String oneOption, String[] args) throws VcsException {
        String[] options = new String[1];
        options[0] = oneOption;

        return execute(cmd, options, args);
    }

    private String execute(@NotNull String cmd, String option, String arg) throws VcsException {
        String[] options = null;
        if (option != null) {
            options = new String[1];
            options[0] = option;
        }
        String[] args = null;
        if (arg != null) {
            args = new String[1];
            args[0] = arg;
        }

        return execute(cmd, options, args);
    }

    private String execute(@NotNull String cmd, String[] options, String[] args) throws VcsException {
        List<String> cmdLine = new ArrayList<String>();
        if (options != null) {
            for (String opt : options) {
                if (opt != null)
                    cmdLine.add(opt);
            }
        }
        if (args != null) {
            for (String arg : args) {
                if (arg != null)
                    cmdLine.add(arg);
            }
        }
        return execute(cmd, cmdLine);
    }

    private String execute(@NotNull String cmd) throws VcsException {
        return execute(cmd, Collections.<String>emptyList());
    }

    private String execute(@NotNull String cmd, List<String> cmdArgs) throws VcsException {
        return execute(cmd, cmdArgs, false);
    }

    private String execute(@NotNull String cmd, boolean silent) throws VcsException {
        return execute(cmd, (List<String>) null, silent);
    }

    private String execute(@NotNull String cmd, String[] cmdArgs, boolean silent) throws VcsException {
        return execute(cmd, Arrays.asList(cmdArgs), silent);
    }

    private String execute(@NotNull String cmd, List<String> cmdArgs, boolean silent) throws VcsException {
        int bufsize = BUF_SIZE;
        List<String> cmdLine = new ArrayList<String>();
        cmdLine.add(settings.GIT_EXECUTABLE);
        cmdLine.add(cmd);
        if (cmdArgs != null) {

            for (String arg : cmdArgs) {
                if (arg != null)
                    cmdLine.add(arg);
            }
        }

        if (cmd.equals(SHOW_CMD) || cmd.equals(ANNOTATE_CMD)) {
            bufsize = BUF_SIZE * 8; // start with bigger buffer when getting contents of files
        }

        File directory = VfsUtil.virtualToIoFile(vcsRoot);

        String cmdStr = null;
        if (DEBUG) {
            cmdStr = StringUtil.join(cmdLine, " ");
            GitVcs.getInstance(project).showMessages("DEBUG: work-dir: [" + directory.getAbsolutePath() + "]" +
                    " exec: [" + cmdStr + "]");
        }

        if (!silent && !DEBUG) { // dont' print twice in DEBUG mode
            if (cmdStr == null)
                cmdStr = StringUtil.join(cmdLine, " ");
            GitVcs.getInstance(project).showMessages("git" + cmdStr.substring(settings.GIT_EXECUTABLE.length()));
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(cmdLine);
            // copy IDEA configured env into process exec env
            Map<String, String> pbenv = pb.environment();
            pbenv.putAll(EnvironmentUtil.getEnviromentProperties());
            pb.directory(directory);
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            // Get the output from the process.
            BufferedInputStream in = new BufferedInputStream(proc.getInputStream());

            byte[] workBuf = new byte[bufsize];
            byte[] retBuf = new byte[bufsize];
            int rlen = in.read(workBuf);   // length of current read
            int wpos = 0; // total count of all bytes read (also write position in retBuf)
            while (rlen != -1) {
                if ((wpos + rlen) > retBuf.length) {  // handle *big* output....
                    if ((retBuf.length * 2) >= MAX_BUF_ALLOWED)
                        throw new VcsException("Git command output limit exceeded, cannot process!");
                    byte[] newbuf = new byte[retBuf.length * 2];
                    System.arraycopy(retBuf, 0, newbuf, 0, wpos);
                    retBuf = newbuf;
                }
                System.arraycopy(workBuf, 0, retBuf, wpos, rlen);
                wpos += rlen;
                rlen = in.read(workBuf);
            }

            try {
                proc.waitFor();
            } catch (InterruptedException ie) {
                return EMPTY_STRING;
            }
            in.close();

            if (wpos == 0) return EMPTY_STRING;
            String output = new String(retBuf, 0, wpos);
            if (proc.exitValue() != 0)
                throw new VcsException(output);

            // empty repo with no commits yet...
            if (output != null && cmd.equals(DIFF_CMD) && output.contains("No HEAD commit to compare with"))
                return EMPTY_STRING;

            return output;
        }
        catch (IOException e) {
            throw new VcsException(e.getMessage());
        }
    }

    public InputStream execAsync() throws VcsException {
        if (cmd == null) throw new VcsException("No command specified!");

        List<String> cmdLine = new ArrayList<String>();
        cmdLine.add(settings.GIT_EXECUTABLE);
        cmdLine.add(cmd);
        if (opts != null && opts.length > 0)
            cmdLine.addAll(Arrays.asList(opts));

        if (args != null && args.length > 0)
            cmdLine.addAll(Arrays.asList(args));

        String cmdString = StringUtil.join(cmdLine, " ");
        GitVcs.getInstance(project).showMessages(cmdString);
        File directory = VfsUtil.virtualToIoFile(vcsRoot);

        try {
            ProcessBuilder pb = new ProcessBuilder(cmdLine);
            // copy IDEA configured env into process exec env
            Map<String, String> pbenv = pb.environment();
            pbenv.putAll(EnvironmentUtil.getEnviromentProperties());
            pb.directory(directory);
            pb.redirectErrorStream(true);

            proc = pb.start();
//            try {
//                proc.waitFor();
//            } catch (InterruptedException ie) {
//            }

            return proc.getInputStream();

        }
        catch (IOException e) {
            throw new VcsException(e.getMessage());
        }
    }

    public boolean isFinished() {
        if (proc == null) return false;
        try {
            proc.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            return false;
        }
    }

    public int exitCode() {
        return proc.exitValue();
    }

    /**
     * Returns the base path of the project.
     *
     * @return The base path of the project.
     */
    private String getBasePath() {
        return vcsRoot.getPath();
    }

    /**
     * Helper method to convert String status' from the git output to a GitFile status
     *
     * @param status The status from git as a String.
     * @return The git file status.
     * @throws com.intellij.openapi.vcs.VcsException
     *          something bad had happened
     */
    private GitVirtualFile.Status convertStatus(String status) throws VcsException {
        if (status.equals("M"))
            return GitVirtualFile.Status.MODIFIED;
        else if (status.equals("C"))
            return GitVirtualFile.Status.COPY;
        else if (status.equals("R"))
            return GitVirtualFile.Status.RENAME;
        else if (status.equals("A"))
            return GitVirtualFile.Status.ADDED;
        else if (status.equals("D"))
            return GitVirtualFile.Status.DELETED;
        else if (status.equals("U"))
            return GitVirtualFile.Status.UNMERGED;
        else if (status.equals("X"))
            return GitVirtualFile.Status.UNVERSIONED;
        else
            return GitVirtualFile.Status.UNMODIFIED;
    }
}