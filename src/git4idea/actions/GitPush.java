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
 * Copyright 2007 Decentrix Inc
 * Copyright 2007 Aspiro AS
 * Copyright 2008 MQSoftware
 * Authors: gevession, Erlend Simonsen & Mark Scott
 *
 * This code was originally derived from the MKS & Mercurial IDEA VCS plugins
 */
import git4idea.GitVcs;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Git "push" action
 */
public class GitPush extends BasicAction {
    @Override
    protected void perform(@NotNull Project project, GitVcs vcs, @NotNull List<VcsException> exceptions,
                           @NotNull VirtualFile[] affectedFiles) throws VcsException {
        saveAll();
        
        final VirtualFile[] roots = ProjectLevelVcsManager.getInstance(project).getRootsUnderVcs(vcs);
        for (VirtualFile root : roots) {
            GitCommand command = new GitCommand(project, vcs.getSettings(), root);
            command.push();

            GitCommandRunnable cmdr = new GitCommandRunnable(project, vcs.getSettings(), root);
            cmdr.setCommand(GitCommand.PUSH_CMD);
            cmdr.setArgs(new String[] { "--mirror" });

            ProgressManager manager = ProgressManager.getInstance();
            //TODO: make this async so the git command output can be seen in the version control window as it happens...
            manager.runProcessWithProgressSynchronously(cmdr, "Pushing all commited changes to remote repos", false, project);

            VcsException ex = cmdr.getException();
            if(ex != null)  {
                Messages.showErrorDialog(project, ex.getMessage(), "Error occurred during 'git push'");
            }
        }
    }

    @Override
    @NotNull
    protected String getActionName(@NotNull AbstractVcs abstractvcs) {
        return "Push";
    }

    @Override
    protected boolean isEnabled(@NotNull Project project, @NotNull GitVcs vcs, @NotNull VirtualFile... vFiles) {
        return true;
    }
}