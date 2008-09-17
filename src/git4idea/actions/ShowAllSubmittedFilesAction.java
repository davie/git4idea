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
 * Author: Anatol Pomozov (Copyright 2008)
 */

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.vcsUtil.VcsRunnable;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.GitBundle;
import git4idea.GitCommitChangeList;
import git4idea.GitFileRevision;
import git4idea.GitRevisionNumber;
import git4idea.GitVcs;
import git4idea.commands.GitCommand;

import java.util.Collection;

/**
 * IDEA action that shows all files changed in the given revision.
 */
public class ShowAllSubmittedFilesAction extends AnAction {
    public ShowAllSubmittedFilesAction() {
        super(GitBundle.message("action.text.show.all.submitted"), null, IconLoader.getIcon("allRevisions.png"));
    }

    public void update(AnActionEvent e) {
        super.update(e);
        final Project project = e.getData(DataKeys.PROJECT);
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        e.getPresentation().setEnabled(e.getData(VcsDataKeys.VCS_FILE_REVISION) != null);
    }

    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getData(DataKeys.PROJECT);
        if (project == null) return;
        final VcsFileRevision revision = e.getData(VcsDataKeys.VCS_FILE_REVISION);
        if (revision != null) {
            final GitFileRevision gitRevision = ((GitFileRevision) revision);
            showSubmittedFiles(project, gitRevision);
        }
    }

    public static void showSubmittedFiles(final Project project, final GitFileRevision gitRevision) {

        final Ref<GitCommitChangeList> cl = new Ref<GitCommitChangeList>();

        final GitRevisionNumber revisionNumber = (GitRevisionNumber) gitRevision.getRevisionNumber();
        String title = getTitle(revisionNumber.getShortRev());
        final GitFileRevision gfr = gitRevision;
        final Project proj = project;

        try {

            final boolean result = VcsUtil.runVcsProcessWithProgress(new VcsRunnable() {
                public void run() throws VcsException {
                    GitCommand command = new GitCommand(proj, GitVcs.getInstance(project).getSettings(),
                            VcsUtil.getVcsRootFor(project, gfr.getFilePath()));
                    Collection<Change> changes = command.getChangesForCommit(revisionNumber.getRev());

                    GitVcs vcs = GitVcs.getInstance(proj);
                    cl.set(new GitCommitChangeList(vcs, gitRevision, changes));
                }
            }, GitBundle.message("show.all.files.from.change.list.searching.for.changed.files.progress.title"), true, project);
            if (result) {
                AbstractVcsHelper.getInstance(project).showChangesBrowser(cl.get(), title);
            }
        }
        catch (VcsException ex) {
            Messages.showErrorDialog(GitBundle.message("message.text.cannot.show.commit", ex.getLocalizedMessage()), title);
        }
    }

    private static String getTitle(final String commitId) {
        return GitBundle.message("dialog.title.show.all.revisions.in.changelist", commitId);
    }
}
