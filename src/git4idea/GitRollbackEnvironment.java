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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedList;

import org.jetbrains.annotations.NotNull;
import git4idea.commands.GitCommand;

/**
 * Git rollback/revert environment
 */
public class GitRollbackEnvironment implements RollbackEnvironment {
    private final Project project;

    public GitRollbackEnvironment(@NotNull Project project) {
        this.project = project;
    }

    @Override
    @NotNull
    public String getRollbackOperationName() {
        return "Revert";
    }

    @Override
    public List<VcsException> rollbackModifiedWithoutCheckout(@NotNull List<VirtualFile> files) {
       List<VcsException> exceptions = new LinkedList<VcsException>();
        final Map<VirtualFile, List<VirtualFile>> roots = GitUtil.sortFilesByVcsRoot(project, files);
        for (VirtualFile root : roots.keySet()) {
            GitCommand command = new GitCommand(project, GitVcsSettings.getInstance(project), root);
            List<VirtualFile> rfiles = roots.get(root);
            try {
                command.revert(rfiles.toArray(new VirtualFile[rfiles.size()]));
            } catch (VcsException e) {
                exceptions.add(e);
            }
        }

        VcsDirtyScopeManager mgr = VcsDirtyScopeManager.getInstance(project);
        for (VirtualFile file : files) {
            mgr.fileDirty(file);
            file.refresh(true, true);
        }

        GitChangeMonitor.getInstance().refresh();
        return exceptions;
    }

    @Override
    public List<VcsException> rollbackMissingFileDeletion(@NotNull List<FilePath> files) {
        return null;
    }

    @Override
    public void rollbackIfUnchanged(@NotNull VirtualFile file) {
    }

    @Override
    public List<VcsException> rollbackChanges(@NotNull List<Change> changes) {
        List<VirtualFile> affectedFiles = new ArrayList<VirtualFile>(changes.size());
        for(Change change: changes) {
            ContentRevision rev = change.getAfterRevision() != null ? change.getAfterRevision(): change.getBeforeRevision();
            if(rev == null) continue;
            FilePath fp = rev.getFile();
            GitVirtualFile file = new GitVirtualFile(project, fp.getPath());
            affectedFiles.add(file);
        }

        return rollbackModifiedWithoutCheckout(affectedFiles);
    }
}