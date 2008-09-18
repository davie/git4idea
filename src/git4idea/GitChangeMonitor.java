package git4idea;
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
 * Authors: Mark Scott
 */

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.RuntimeInterruptedException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.commands.GitCommand;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Monitor filesystem changes in the Git repository
 */
public class GitChangeMonitor extends Thread {
    private boolean running = false;
    public static int DEF_INTERVAL_SECS = 30;
    private long interval = DEF_INTERVAL_SECS * 1000L;
    private static GitChangeMonitor monitor = null;
    private GitVcsSettings settings;
    private Project project;
    ChangelistBuilder builder;
    private static final Lock threadLock = new ReentrantLock();


    public static GitChangeMonitor getInstance() {
        threadLock.lock();
        try {
            if (monitor == null) {
                monitor = new GitChangeMonitor();
            }
        } finally {
            threadLock.unlock();
        }
        return monitor;
    }

    /**
     * Create a Git change monitor thread.
     */
    private GitChangeMonitor() {
        super("GitChangeMonitor");
        setDaemon(true);
    }

    /**
     * Set the change monitor's change list builder.
     *
     * @param builder The builder to use
     */
    public void setBuilder(ChangelistBuilder builder) {
        if (builder != null)
            this.builder = builder;
    }

    /**
     * Halt the change monitor
     */
    public void stopRunning() {
        running = false;
        interrupt();
    }

    /**
     * Set the project for this change monitor
     *
     * @param proj The project to asssociate with
     */
    public void setProject(Project proj) {
        project = proj;
    }

    /**
     * Set the Git VCS settings for this change monitor
     *
     * @param gsettings The settings to use
     */
    public void setGitVcsSettings(GitVcsSettings gsettings) {
        settings = gsettings;
    }

    public void start() {
        threadLock.lock();
        try {
            if (project == null || settings == null)
                throw new IllegalStateException("Project & VCS settings not set!");
            if (!running) {
                running = true;
                super.start();
            }
        } finally {
            threadLock.unlock();
        }
    }

    @SuppressWarnings({"EmptyCatchBlock"})
    public void run() {
        while (running) {
            try {
                check();
                sleep(interval);
            } catch (InterruptedException e) {
            } catch (RuntimeInterruptedException e) {
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check to see what files have changed under the monitored content roots.
     */
    private void check() {
        threadLock.lock();
        try {
            final VirtualFile[][] roots = new VirtualFile[1][];
            ApplicationManager.getApplication().runReadAction(
                        new Runnable() {
                            public void run() {
                                    roots[0] = ProjectRootManager.getInstance(project).getContentRoots();
                            }
                        });

            for (VirtualFile root : roots[0]) {
                if (root == null) continue;
                if (builder == null) return;
                final GitCommand cmd = new GitCommand(project, settings, root);
                try {
                final Set<GitVirtualFile> uncached = cmd.gitUnCachedFiles();
                final Set<GitVirtualFile> others = cmd.gitOtherFiles();
                ApplicationManager.getApplication().invokeLater(
                        new Runnable() {
                            public void run() {
                                    processChanges(uncached);
                                    processChanges(others);
                                    ChangeListManager.getInstance(project).scheduleUpdate(true);
                            }
                        });
                } catch(VcsException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            threadLock.unlock();
        }
    }

    private void processChanges(Collection<GitVirtualFile> files) {
        if (files == null || files.size() == 0 || builder == null) return;
        for (GitVirtualFile file : files) {
            if (file == null) continue;
            ContentRevision beforeRev = new GitContentRevision(file, new GitRevisionNumber(GitRevisionNumber.TIP, new Date(file.getModificationStamp())), project);
            ContentRevision afterRev = CurrentContentRevision.create(VcsUtil.getFilePath(file.getPath()));

            switch (file.getStatus()) {
                case UNMERGED: {
                    builder.processChange(new Change(beforeRev, afterRev, FileStatus.MERGED_WITH_CONFLICTS));
                    break;
                }
                case ADDED: {
                    builder.processChange(new Change(null, afterRev, FileStatus.ADDED));
                    break;
                }
                case DELETED: {
                    builder.processChange(new Change(beforeRev, null, FileStatus.DELETED));
                    break;
                }
                case COPY:
                case RENAME:
                case MODIFIED: {
                    builder.processChange(new Change(beforeRev, afterRev, FileStatus.MODIFIED));
                    break;
                }
                case UNMODIFIED:
                    break;
                case UNVERSIONED:
                    builder.processUnversionedFile(file);
                    break;
                default:
                    builder.processChange(new Change(null, afterRev, FileStatus.UNKNOWN));
            }
        }
    }
}