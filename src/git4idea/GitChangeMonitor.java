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
import com.intellij.vcsUtil.VcsUtil;
import git4idea.commands.GitCommand;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

/**
 * Monitor changes in the Git repository
 */
public class GitChangeMonitor extends Thread implements ModuleRootListener {
    private boolean running = false;
    public static int DEF_INTERVAL_SECS = 30;
    private long interval = DEF_INTERVAL_SECS * 1000L;
    private static GitChangeMonitor monitor = null;
    private Map<VirtualFile, Set<GitVirtualFile>> changeMap;
    private GitVcsSettings settings;
    private Project project;
    private ChangeListManager changeListManager;


    public static synchronized GitChangeMonitor getInstance() {
        if (monitor == null) {
            monitor = new GitChangeMonitor(DEF_INTERVAL_SECS);
        }
        return monitor;
    }

    public static synchronized GitChangeMonitor getInstance(int secs) {
        if (monitor == null) {
            monitor = new GitChangeMonitor(secs);
        } else {
            monitor.setInterval(secs);
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
        running = true;
        setInterval(secs);
        changeMap = new ConcurrentHashMap<VirtualFile, Set<GitVirtualFile>>();

    }

    /**
     * Halt the change monitor
     */
    public void stopRunning() {
        running = false;
        changeMap.clear();
        ProjectRootManager.getInstance(project).removeModuleRootListener(this);
        interrupt();
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
        project = proj;
        ProjectRootManager pMgr = ProjectRootManager.getInstance(project);
        pMgr.removeModuleRootListener(this);
        for (VirtualFile root : pMgr.getContentRoots()) {
            addRoot(root);
        }
        pMgr.addModuleRootListener(this);
        changeListManager = ChangeListManager.getInstance(project);
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

    public synchronized void start() {
        if (project == null || settings == null)
            throw new IllegalStateException("Project & VCS settings not set!");
        super.start();
    }

    @SuppressWarnings({"EmptyCatchBlock"})
    public void run() {
        while (running) {
            check();
            try {
                sleep(interval);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Refresh the current set of changed file (ie: re-check immediately)
     */
    public void refresh() {
        interrupt();
    }

    /**
     * Add the specified content root to be monitored
     *
     * @param root The top level directory to monitor
     */
    public void addRoot(VirtualFile root) {
        if (changeMap.containsKey(root))
            return;
        Set<GitVirtualFile> files = new HashSet<GitVirtualFile>();
        changeMap.put(root, files);
    }

    /**
     * Delete the specified content root to be monitored
     *
     * @param root The top level directory to no longer monitor
     */
    public void deleteRoot(VirtualFile root) {
        changeMap.remove(root);
    }

    /**
     * Return the set of files that changed under the specified content root.
     *
     * @param root The content root to examine
     * @return The set of files that has changed
     */
    @Nullable
    public Set<GitVirtualFile> getChangedFiles(VirtualFile root) {
        return changeMap.get(root);
    }

    /**
     * Check to see what files have changed under the monitored content roots.
     */
    private void check() {
        Set<VirtualFile> roots = changeMap.keySet();
        for (VirtualFile root : roots) {
            GitCommand cmd = new GitCommand(project, settings, root);
            try {
                Set<GitVirtualFile> changedFiles = cmd.changedFiles();
                if (changedFiles == null || changedFiles.size() == 0) {
                    changeMap.remove(root);
                } else {
                    changeMap.put(root, changedFiles);
                    LocalChangeList uncommitted = changeListManager.findChangeList("All Uncommited Changes");
                    if(uncommitted == null) {
                        uncommitted = changeListManager.addChangeList("All Uncommited Changes","");
                    }

                    changeListManager.moveChangesTo(uncommitted, getChanges(changedFiles));
                    changeListManager.setDefaultChangeList(uncommitted);
                    changeListManager.scheduleUpdate();
                }
            } catch (VcsException e) {
                e.printStackTrace();
            }
        }
    }

    public void beforeRootsChange(ModuleRootEvent event) {     // unused
    }

    public void rootsChanged(ModuleRootEvent event) {
        changeMap.clear();
        for (VirtualFile root : ProjectRootManager.getInstance(project).getContentRoots()) {
            addRoot(root);
        }
        refresh();
    }

    private Change[] getChanges(Collection<GitVirtualFile> files) {
        Collection<Change> changes = new ArrayList<Change>();
        for (GitVirtualFile file : files) {
            FilePath path = VcsUtil.getFilePath(file.getPath());
            VirtualFile vfile = VcsUtil.getVirtualFile(file.getPath());
            ContentRevision beforeRev = null;
            if (file != null)
                beforeRev = new GitContentRevision(path, new GitRevisionNumber(GitRevisionNumber.TIP, new Date(file.getModificationStamp())), project);
            ContentRevision afterRev = CurrentContentRevision.create(path);

            switch (file.getStatus()) {
                case UNMERGED: {
                    changes.add(new Change(beforeRev, afterRev, FileStatus.MERGED_WITH_CONFLICTS));
                    break;
                }
                case ADDED: {
                    changes.add(new Change(null, afterRev, FileStatus.ADDED));
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

        return changes.toArray(new Change[ changes.size() ]);
    }
}