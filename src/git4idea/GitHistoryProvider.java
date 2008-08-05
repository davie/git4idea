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
import git4idea.commands.GitCommand;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.history.*;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Git history provider implementation
 */
public class GitHistoryProvider implements VcsHistoryProvider {
    private final Project project;
    private final GitVcsSettings settings;

    public GitHistoryProvider(@NotNull Project project, @NotNull GitVcsSettings settings) {
        this.project = project;
        this.settings = settings;
    }

    @Override
    public ColumnInfo[] getRevisionColumns() {
        return new ColumnInfo[0];
    }

    @Override
    public AnAction[] getAdditionalActions(FileHistoryPanel panel) {
        return new AnAction[0];  
    }

    @Override
    public boolean isDateOmittable() {
        return false;
    }

    @Override
    @Nullable
    public String getHelpId() {
        return null;
    }

    @Override
    @Nullable
    public VcsHistorySession createSessionFor(FilePath filePath) throws VcsException {
        GitCommand command = new GitCommand(project, settings, GitUtil.getVcsRoot(project, filePath));
        List<VcsFileRevision> revisions = command.log(filePath);
        final FilePath path = filePath;

        return new VcsHistorySession(revisions) {
            @Nullable
            protected VcsRevisionNumber calcCurrentRevisionNumber() {
                return CurrentContentRevision.create(path).getRevisionNumber();
            }
        };
    }

    @Override
    @Nullable
    public HistoryAsTreeProvider getTreeHistoryProvider() {
        return new GitHistoryTreeProvider();
    }
}