package git4idea.actions;
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

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.progress.ProgressManager;
import git4idea.GitVcs;
import git4idea.GitBranch;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandRunnable;

import java.util.List;

/**
 * Git "fetch" action
 */
public class GitFetch extends BasicAction {
    @Override
    protected void perform(@NotNull Project project, GitVcs vcs, @NotNull List<VcsException> exceptions,
                           @NotNull VirtualFile[] affectedFiles) throws VcsException {
        saveAll();

        final VirtualFile[] roots = ProjectLevelVcsManager.getInstance(project).getRootsUnderVcs(vcs);
        for (VirtualFile root : roots) {
            GitCommand command = new GitCommand(project, vcs.getSettings(), root);

            String initialValue = null;
            List<GitBranch> rbranches = command.branchList(true);
            if (rbranches != null && rbranches.size() > 0) {
                initialValue = command.remoteRepoURL(rbranches.get(0));
            }
            String repoURL = Messages.showInputDialog(project,
                    "Enter remote repository URL to fetch (empty for default):",
                    "Fetch URL", Messages.getQuestionIcon(), initialValue, null);

            GitCommandRunnable cmdr = new GitCommandRunnable(project, vcs.getSettings(), root);
            cmdr.setCommand(GitCommand.FETCH_CMD);
            cmdr.setArgs(new String[]{repoURL});

            ProgressManager manager = ProgressManager.getInstance();
            manager.runProcessWithProgressSynchronously(cmdr, "Fetching from " + repoURL, false, project);

            VcsException ex = cmdr.getException();
            if (ex != null) {
                Messages.showErrorDialog(project, ex.getMessage(), "Error occurred during 'git fetch'");
                return;
            }

            cmdr.setArgs(new String[] { "--tags", repoURL });
            manager.runProcessWithProgressSynchronously(cmdr, "Updating tags from " + repoURL, false, project);
            ex = cmdr.getException();
            if(ex != null)  {
                Messages.showErrorDialog(project, ex.getMessage(), "Error occurred during 'git fetch --tags'");
                return;
            }
        }
    }

    @Override
    @NotNull
    protected String getActionName(@NotNull AbstractVcs abstractvcs) {
        return "Fetch";
    }

    @Override
    protected boolean isEnabled(@NotNull Project project, @NotNull GitVcs vcs, @NotNull VirtualFile... vFiles) {
        return true;
    }
}