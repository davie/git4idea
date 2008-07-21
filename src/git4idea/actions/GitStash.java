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
 * Authors: Mark Scott
 */

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.progress.ProgressManager;
import git4idea.GitVcs;
import git4idea.GitUtil;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandRunnable;

import java.util.List;
import java.util.Map;

/**
 * Git stash action
 */
public class GitStash extends BasicAction {

    protected void perform(@NotNull Project project, GitVcs vcs, @NotNull List<VcsException> exceptions, @NotNull VirtualFile[] affectedFiles) throws VcsException {
        saveAll();

        if (!ProjectLevelVcsManager.getInstance(project).checkAllFilesAreUnder(GitVcs.getInstance(project), affectedFiles))
            return;

        final Map<VirtualFile, List<VirtualFile>> roots = GitUtil.sortFilesByVcsRoot(project, affectedFiles);

        String stashName = Messages.showInputDialog(project,
                "Enter new stash name/description: ",
                "Stash", Messages.getQuestionIcon(), "", null);

        if (stashName == null || stashName.length() == 0) return;

        for (VirtualFile root : roots.keySet()) {
            GitCommandRunnable cmdr = new GitCommandRunnable(project, vcs.getSettings(), root);
            cmdr.setCommand(GitCommand.STASH_CMD);
            cmdr.setArgs(new String[]{ "save", stashName} );

            ProgressManager manager = ProgressManager.getInstance();
            //TODO: make this async so the git command output can be seen in the version control window as it happens...
            manager.runProcessWithProgressSynchronously(cmdr, "Stashing changes... ", false, project);

            VcsException ex = cmdr.getException();
            if (ex != null) {
                Messages.showErrorDialog(project, ex.getMessage(), "Error occurred during 'git stash'");
                break;
            }
        }
    }

    @NotNull
    protected String getActionName(@NotNull AbstractVcs abstractvcs) {
        return "Stash";
    }

    protected boolean isEnabled(@NotNull Project project, @NotNull GitVcs mksvcs, @NotNull VirtualFile... vFiles) {
        return true;
    }
}