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
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;

import java.util.Set;
import java.util.Collection;
import java.util.Date;

import org.jetbrains.annotations.NotNull;

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
        for (VirtualFile root : roots) {
            Set<VirtualFile> cfiles = GitChangeMonitor.getInstance().getChangedFiles(root);
            if(cfiles == null || cfiles.size() == 0) return;
            for (VirtualFile file : cfiles)
                getChange(builder, file);
        }
    }

    @Override
    public boolean isModifiedDocumentTrackingRequired() {
        return false;
    }

    private void getChange(ChangelistBuilder builder, VirtualFile file) {
        if(builder == null || file == null) return;
        GitVirtualFile gvFile = (GitVirtualFile) file;
        VirtualFile vfile = VcsUtil.getVirtualFile(file.getPath());
        ContentRevision beforeRev = new GitContentRevision(gvFile, new GitRevisionNumber(
                GitRevisionNumber.TIP, new Date(gvFile.getModificationStamp())), project);
        ContentRevision afterRev = new GitContentRevision(gvFile, new GitRevisionNumber(
                GitRevisionNumber.TIP, new Date(gvFile.getModificationStamp())), project);

        switch (gvFile.getStatus()) {
            case UNMERGED: {
                builder.processChange(
                        new Change(beforeRev, afterRev, FileStatus.MERGED_WITH_CONFLICTS));
                break;
            }
            case ADDED: {
                builder.processChange(
                        new Change(null, afterRev, FileStatus.ADDED));
                break;
            }
            case DELETED: {
                builder.processChange(
                        new Change(beforeRev, null, FileStatus.DELETED));
                break;
            }
            case COPY:
            case RENAME:
            case MODIFIED: {
                builder.processChange(
                        new Change(beforeRev, afterRev, FileStatus.MODIFIED));
                break;
            }
            case UNMODIFIED: {
                break;
            }
            case UNVERSIONED: {
                builder.processUnversionedFile(vfile);
                break;
            }
            default: {
                builder.processChange(
                        new Change(null, afterRev, FileStatus.UNKNOWN));
            }
        }
    }
}