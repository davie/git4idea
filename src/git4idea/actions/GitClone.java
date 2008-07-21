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
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.progress.ProgressManager;
import git4idea.GitUtil;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandRunnable;
import git4idea.GitVcs;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Use Git to clone a repository
 */
public class GitClone extends BasicAction {
    @Override
    public void perform(@NotNull Project project, GitVcs vcs, @NotNull List<VcsException> exceptions,
                        @NotNull VirtualFile[] affectedFiles) throws VcsException {
        saveAll();

        // TODO: implement remote repository login/password - setup remote repos in Git config,
        // TODO: then just reference repo name here
        final String src_repo = Messages.showInputDialog(project, "Specify source repository URL", "clone",
                Messages.getQuestionIcon());
        if (src_repo == null)
            return;

        FileChooserDescriptor fcd = new FileChooserDescriptor(false, true, false, false, false, false);
        fcd.setShowFileSystemRoots(true);
        fcd.setTitle("Destination Directory");
        fcd.setDescription("Select destination directory for clone.");
        fcd.setHideIgnored(false);
        VirtualFile[] files = FileChooser.chooseFiles(project, fcd, null);
        if (files.length != 1 || files[0] == null) {
            return;
        }

        final Map<VirtualFile, List<VirtualFile>> roots = GitUtil.sortFilesByVcsRoot(project, affectedFiles);
        for (VirtualFile root : roots.keySet()) {
            GitCommandRunnable cmdr = new GitCommandRunnable(project, vcs.getSettings(), root);
            cmdr.setCommand(GitCommand.CLONE_CMD);
            cmdr.setArgs(new String[]{src_repo, files[0].getPath()});

            ProgressManager manager = ProgressManager.getInstance();
            //TODO: make this async so the git command output can be seen in the version control window as it happens...
            manager.runProcessWithProgressSynchronously(cmdr, "Cloning source repo " + src_repo, false, project);

            VcsException ex = cmdr.getException();
            if (ex != null) {
                Messages.showErrorDialog(project, ex.getMessage(), "Error occurred during 'git clone'");
                break;
            }
        }

        VcsDirtyScopeManager mgr = VcsDirtyScopeManager.getInstance(project);
        for (VirtualFile file : affectedFiles) {
            mgr.fileDirty(file);
            file.refresh(true, true);
        }
    }

    @Override
    @NotNull
    protected String getActionName(@NotNull AbstractVcs abstractvcs) {
        return "Clone";
    }

    @Override
    protected boolean isEnabled(@NotNull Project project, @NotNull GitVcs mksvcs, @NotNull VirtualFile... vFiles) {
        return true;
    }
}