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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.history.FileHistoryPanel;
import com.intellij.openapi.vcs.history.HistoryAsTreeProvider;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsHistorySession;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.util.ui.ColumnInfo;
import git4idea.actions.ShowAllSubmittedFilesAction;
import git4idea.commands.GitCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Git history provider implementation
 */
public class GitHistoryProvider implements VcsHistoryProvider {
    private final
    @NotNull
    Project project;
    private final
    @NotNull
    GitVcsSettings settings;

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
            return new AnAction[]{new ShowAllSubmittedFilesAction()};
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

    @SuppressWarnings({"unchecked"})
    @Override
    @Nullable
    public VcsHistorySession createSessionFor(final FilePath filePath) throws VcsException {
        final List<VcsFileRevision> revisions = new ArrayList<VcsFileRevision>(25);
        final VcsException[] exception = new VcsException[1];

        Runnable command = new Runnable() {
            public void run() {
                final ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
                progress.setIndeterminate(true);

                GitCommand gc = new GitCommand(project, settings, GitUtil.getVcsRoot(project, filePath));
                progress.setText2("Retrieving commit history for: " +
                        gc.getRelativeFilePath(filePath.getVirtualFile(),GitUtil.getVcsRoot(project, filePath)));
                try {
                    List<VcsFileRevision> revs = gc.log(filePath);
                    if (revs != null && revs.size() > 0)
                        revisions.addAll(revs);
                } catch (VcsException e) {
                    exception[0] = e;
                }
            }
        };

        if (ApplicationManager.getApplication().isDispatchThread()) {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(command, "Commit History", false, project);
        } else {
            command.run();
        }

        if (exception[0] != null)
            throw exception[0];

        return new VcsHistorySession(revisions) {
            @Nullable
            protected VcsRevisionNumber calcCurrentRevisionNumber() {
                return CurrentContentRevision.create(filePath).getRevisionNumber();
            }
        };
    }

    @Override
    @Nullable
    public HistoryAsTreeProvider getTreeHistoryProvider() {
        return new GitHistoryTreeProvider();
    }
}