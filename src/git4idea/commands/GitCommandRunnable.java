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
 * Copyright 2008 MQSoftware
 * Author: Mark Scott
 *
 * This code was originally derived from the MKS & Mercurial IDEA VCS plugins
 */
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.EnvironmentUtil;

import java.io.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.nio.charset.Charset;

import org.jetbrains.annotations.NotNull;
import git4idea.GitVcsSettings;
import git4idea.GitVcs;

/**
 * Run a Git command as a Runnable
 */
@SuppressWarnings({"JavaDoc"})
public class GitCommandRunnable implements Runnable {
    private static final int BUF_SIZE = 4096;
    private String cmd = null;
    private Project project = null;
    private GitVcsSettings settings = null;
    private String[] opts = null;
    private String[] args = null;
    private VirtualFile vcsRoot = null;
    private VcsException vcsEx = null;
    private boolean keepit = false;
    private boolean silent = false;
    private ByteArrayOutputStream baos = null;

    public GitCommandRunnable(@NotNull final Project project, @NotNull GitVcsSettings settings, @NotNull VirtualFile vcsRoot) {
        this.project = project;
        this.settings = settings;
        this.vcsRoot = vcsRoot;
    }

    public GitCommandRunnable(@NotNull final Project project, @NotNull GitVcsSettings settings, @NotNull VirtualFile vcsRoot,
                              String cmd, String[] opts, String[] args) {
        this.project = project;
        this.settings = settings;
        this.vcsRoot = vcsRoot;
        this.cmd = cmd;
        this.opts = opts;
        this.args = args;
    }

    @SuppressWarnings({"EmptyCatchBlock"})
    @Override
    public void run() {
        if (cmd == null) throw new IllegalStateException("No command set!");
        vcsEx = null;
        GitVcs vcs = GitVcs.getInstance(project);

        List<String> cmdLine = new ArrayList<String>();
        cmdLine.add(settings.GIT_EXECUTABLE);
        cmdLine.add(cmd);
        if (opts != null && opts.length > 0)
            cmdLine.addAll(Arrays.asList(opts));
        if (args != null && args.length > 0)
            cmdLine.addAll(Arrays.asList(args));

        ProgressManager manager = ProgressManager.getInstance();
        ProgressIndicator indicator = manager.getProgressIndicator();
        indicator.setText("Git " + cmd + "...");
        indicator.setIndeterminate(true);

        String cmdStr = StringUtil.join(cmdLine, " ");
        vcs.showMessages("git" +  cmdStr.substring(settings.GIT_EXECUTABLE.length()) );

        ProcessBuilder pb = new ProcessBuilder(cmdLine);
        // copy IDEA configured env into process exec env
        Map<String, String> pbenv = pb.environment();
        pbenv.putAll(EnvironmentUtil.getEnviromentProperties());

        File directory = VfsUtil.virtualToIoFile(vcsRoot);
        pb.directory(directory);
        pb.redirectErrorStream(true);

        Process proc;
        BufferedInputStream in = null;
        int exitValue = -1;

        byte[] buf = new byte[BUF_SIZE];
        if (keepit)
            baos = new ByteArrayOutputStream(buf.length);

        try {
            proc = pb.start();
            Thread.sleep(250);
            in = new BufferedInputStream(proc.getInputStream());

            int l;
            while ((l = in.read(buf)) != -1) {
                if (keepit)
                    baos.write(buf, 0, l);
                if (!silent)
                    vcs.showMessages(new String(buf, 0, l, Charset.defaultCharset()));
            }
            exitValue = proc.waitFor();
        } catch (InterruptedException ie) {
        } catch (Exception e) {
            vcsEx = new VcsException(e);
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
            }
        }

        if (exitValue != 0 || vcsEx != null) {
            String msg;
            if (vcsEx != null)
                msg = vcsEx.getMessage();
            else {
                msg = new String(buf);
                int nullIdx = msg.indexOf(0);
                if (nullIdx > 5)
                    msg = msg.substring(0, nullIdx); // strip out nulls
                vcsEx = new VcsException(msg);
            }
        }
    }

    /**
     * Returns the exception thrown by the command runnable
     *
     * @return The exception, else null if the command was sucessful
     */
    public VcsException getException() {
        return vcsEx;
    }

    /**
     * Set the runnable's Git command.
     */
    public void setCommand(String cmd) {
        this.cmd = cmd;
    }

    /**
     * Set the runnable's Git command options.
     */
    public void setOptions(String[] opts) {
        this.opts = opts;
    }

    /**
     * Set the runnable's Git command arguments.
     */
    public void setArgs(String[] args) {
        this.args = args;
    }

    /**
     * Set to true if a copy of the git command output should be saved. Use getOutput()
     * later to retrieve it. (Default is false)
     */
    public void saveOutput(boolean keepit) {
        this.keepit = keepit;
    }

    /**
     * Set to true if git command output is to NOT be sent the version control console. (Default is false)
     */
    public void setSilent(boolean isSilent) {
        silent = isSilent;
    }

    /**
     * Retrieve the output (error & stdout are mingled) from the git command. This is only useful after the command has finished running...
     */
    public String getOutput() {
        if (!keepit)
            return null;
        try {
            return baos.toString(Charset.defaultCharset().name());
        } catch (UnsupportedEncodingException e) { // should never happen...
            return null;
        }
    }
}