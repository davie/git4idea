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
 * Copyright 2007 Decentrix Inc
 * Copyright 2007 Aspiro AS
 * Copyright 2008 MQSoftware
 * Authors: gevession, Erlend Simonsen & Mark Scott
 *
 * This code was originally derived from the MKS & Mercurial IDEA VCS plugins
 */

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.VcsDirtyScope;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.commands.GitCommand;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Git repository change provide
 */
public class GitChangeProvider implements ChangeProvider {
    private Project project;
    private GitVcsSettings settings;

    public GitChangeProvider(@NotNull Project project, @NotNull GitVcsSettings settings) {
        this.project = project;
        this.settings = settings;
    }

    @Override
    public void getChanges(VcsDirtyScope dirtyScope, ChangelistBuilder builder, ProgressIndicator progress) throws VcsException {
        Collection<VirtualFile> roots = dirtyScope.getAffectedContentRoots();
        final ChangeListManager clmgr = ChangeListManager.getInstance(project);

        for (VirtualFile root : roots) {
            GitCommand command = new GitCommand(project, settings, root);
            Set<GitVirtualFile> files = command.changedFiles();
            for (GitVirtualFile file : files) {
                Change c = getChange(file);
                if(c != null)
                    builder.processChange(c);
            }
        }
        clmgr.scheduleUpdate(true);
    }

    @Override
    public boolean isModifiedDocumentTrackingRequired() {
        return false;
    }

    private Change getChange(VirtualFile file) {
        if ( file == null) return null;
        GitVirtualFile gvFile = (GitVirtualFile) file;
        ContentRevision beforeRev = new GitContentRevision(gvFile, new GitRevisionNumber(
                GitRevisionNumber.TIP, new Date(gvFile.getModificationStamp())), project);
        ContentRevision afterRev = CurrentContentRevision.create(VcsUtil.getFilePath(file.getPath()));

        Change c = null;
        switch (gvFile.getStatus()) {
            case UNMERGED: {
                c = new Change(beforeRev, afterRev, FileStatus.MERGED_WITH_CONFLICTS);
                break;
            }
            case ADDED: {
                c = new Change(null, afterRev, FileStatus.ADDED);
                break;
            }
            case DELETED: {
                c = new Change(beforeRev, null, FileStatus.DELETED);
                break;
            }
            case COPY:
            case RENAME:
            case MODIFIED: {
                c = new Change(beforeRev, afterRev, FileStatus.MODIFIED);
                break;
            }
            case UNMODIFIED: {
                break;
            }
            default: {
                c = new Change(null, afterRev, FileStatus.UNKNOWN);
            }
        }
        return c;
    }
}