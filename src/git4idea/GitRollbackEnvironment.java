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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Git rollback/revert environment
 */
public class GitRollbackEnvironment implements RollbackEnvironment {
    //TODO: implement this whole class!!
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
        Messages.showInfoMessage(project, "rollbackModifiedWithoutCheckout", "Revert");
        return null;
    }

    @Override
    public List<VcsException> rollbackMissingFileDeletion(@NotNull List<FilePath> files) {
        Messages.showInfoMessage(project, "rollbackMissingFileDeletion", "Revert");
        return null;
    }

    @Override
    public void rollbackIfUnchanged(@NotNull VirtualFile file) {
        Messages.showInfoMessage(project, "rollbackIfUnchanged", "Revert");
    }

    @Override
    public List<VcsException> rollbackChanges(@NotNull List<Change> changes) {
        Messages.showInfoMessage(project, "rollbackChanges", "Rollback");
        return null;
    }
}