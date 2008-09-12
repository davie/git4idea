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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.application.RuntimeInterruptedException;
import com.intellij.vcsUtil.VcsUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.ProjectTopics;
import git4idea.commands.GitCommand;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jetbrains.annotations.Nullable;

/**
 * Monitor changes in the Git repository
 */
public class GitChangeMonitor extends Thread implements ModuleRootListener {
    private boolean running = false;
    public static int DEF_INTERVAL_SECS = 10;
    private long interval = DEF_INTERVAL_SECS * 1000L;
    private static GitChangeMonitor monitor = null;
    private Map<VirtualFile, Set<VirtualFile>> changeMap;
    private GitVcsSettings settings;
    private Project project;
    private ChangeListManager changeListManager;
    private MessageBusConnection msgBus;
    private static final Lock threadLock = new ReentrantLock();


    public static GitChangeMonitor getInstance() {
        threadLock.lock();
        try {
            if (monitor == null) {
                monitor = new GitChangeMonitor(DEF_INTERVAL_SECS);
            }
        } finally {
            threadLock.unlock();
        }
        return monitor;
    }

    public static GitChangeMonitor getInstance(int secs) {
        threadLock.lock();
        try {
            if (monitor == null) {
                monitor = new GitChangeMonitor(secs);
            } else {
                monitor.setInterval(secs);
            }
        } finally {
            threadLock.unlock();
        }
        return monitor;
    }

    /**
     * Create a Git change monitor thread, which checks for changes at the specified interval in seconds.
     *
     * @param secs How often to check for changed files
     */
    private GitChangeMonitor(int secs) {
        super("GitChangeMonitor");
        setDaemon(true);
        setInterval(secs);
        changeMap = new ConcurrentHashMap<VirtualFile, Set<VirtualFile>>();
    }

    /**
     * Halt the change monitor
     */
    public void stopRunning() {
        threadLock.lock();
        try {
            running = false;
            if (msgBus != null) {
                msgBus.disconnect();
                msgBus = null;
            }
            changeMap.clear();
            interrupt();
        } finally {
            threadLock.unlock();
        }
    }

    /**
     * Set the specified check interval for monitoring
     *
     * @param secs The number of seconds to use for a check-interval
     */
    public void setInterval(int secs) {
        if (secs < 1)
            secs = DEF_INTERVAL_SECS;
        interval = secs * 1000L;
    }

    /**
     * Set the project for this change monitor
     *
     * @param proj The project to asssociate with
     */
    public void setProject(Project proj) {
        threadLock.lock();
        try {
            project = proj;
            if (msgBus != null)
                msgBus.disconnect();
            changeMap.clear();
            for (VirtualFile root : ProjectRootManager.getInstance(project).getContentRoots())
                addRoot(root);
            msgBus = project.getMessageBus().connect();
            msgBus.subscribe(ProjectTopics.PROJECT_ROOTS, this);
            changeListManager = ChangeListManager.getInstance(project);
        } finally {
            threadLock.unlock();
        }
    }

    /**
     * Set the Git VCS settings for this change monitor
     *
     * @param gsettings The settings to use
     */
    public void setGitVcsSettings(GitVcsSettings gsettings) {
        settings = gsettings;
        setInterval(settings.GIT_INTERVAL);
    }

    public void start() {
        threadLock.lock();
        try {
            if (project == null || settings == null)
                throw new IllegalStateException("Project & VCS settings not set!");
            if (!running) {
                running = true;
                this.setName("GitChangeMonitor");
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
     * Refresh the current set of changed file (ie: re-check immediately)
     */
    public void refresh() {
          check();
    }

    /**
     * Add the specified content root to be monitored
     *
     * @param root The top level directory to monitor
     */
    public void addRoot(VirtualFile root) {
        if (changeMap.containsKey(root))
            return;
        Set<VirtualFile> files = new HashSet<VirtualFile>();
        changeMap.put(root, files);
    }

    /**
     * Return the set of files that changed under the specified content root.
     *
     * @param root The content root to examine
     * @return The set of files that has changed
     */
    @Nullable
    public Set<VirtualFile> getChangedFiles(VirtualFile root) {
        return changeMap.get(root);
    }

    /**
     * Check to see what files have changed under the monitored content roots.
     */
    private void check() {
        Set<VirtualFile> roots = changeMap.keySet();

        if(!threadLock.tryLock()) return;   // don't check if one is already in progress...
        try {
            for (VirtualFile root : roots) {
                GitCommand cmd = new GitCommand(project, settings, root);
                try {
                    Set<VirtualFile> changedFiles = cmd.changedFiles();
                    if (changedFiles == null || changedFiles.size() == 0) {
                        changeMap.put(root, new HashSet<VirtualFile>());
                    } else {
                        changeMap.put(root, changedFiles);
                        Change[] allGitChanges = getChanges(changedFiles);
                        for (Change gitChange : allGitChanges) { // go through every change Git finds & see if in
                            // a changelist already, if not put in default changelist
                            boolean matched = false;
                            for (LocalChangeList list : changeListManager.getChangeLists()) {
                                for (Change change : list.getChanges()) {
                                    if (gitChange.equals(change)) {
                                        if (gitChange.getType() == change.getType()) {
                                            matched = true;    // don't match if status/type is different
                                        }
                                        break;
                                    }
                                }
                                if (matched) break;
                            }
                            if (!matched) {
                                LocalChangeList gitList = changeListManager.getDefaultChangeList();
                                if (gitList.isReadOnly())
                                    gitList.setReadOnly(false);
                                changeListManager.moveChangesTo(gitList, new Change[]{gitChange});
                            }
                        }
                    }
                } catch (VcsException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            threadLock.unlock();
        }
        
      changeListManager.scheduleUpdate(true);
    }

    public void beforeRootsChange(ModuleRootEvent event) {     // unused
    }

    public void rootsChanged(ModuleRootEvent event) {
        threadLock.lock();
        try {
            changeMap.clear();
            for (VirtualFile root : ProjectRootManager.getInstance(project).getContentRoots())
                addRoot(root);
        } finally {
            threadLock.unlock();
        }
        refresh();
    }

    private Change[] getChanges(Collection<VirtualFile> files) {
        Collection<Change> changes = new ArrayList<Change>();
        for (VirtualFile file : files) {
            if (file == null) continue;
            GitVirtualFile gvFile = (GitVirtualFile) file;
            ContentRevision beforeRev = new GitContentRevision(gvFile, new GitRevisionNumber(GitRevisionNumber.TIP, new Date(file.getModificationStamp())), project);
            ContentRevision afterRev = beforeRev;

            switch (gvFile.getStatus()) {
                case UNMERGED: {
                    changes.add(new Change(beforeRev, afterRev, FileStatus.MERGED_WITH_CONFLICTS));
                    break;
                }
                case ADDED: {
                    changes.add(new Change(afterRev, afterRev, FileStatus.ADDED));
                    break;
                }
                case DELETED: {
                    changes.add(new Change(beforeRev, afterRev, FileStatus.DELETED));
                    break;
                }
                case COPY:
                case RENAME:
                case MODIFIED: {
                    changes.add(new Change(beforeRev, afterRev, FileStatus.MODIFIED));
                    break;
                }
                case UNMODIFIED:
                case UNVERSIONED:
                default:
            }
        }

        return changes.toArray(new Change[changes.size()]);
    }
}