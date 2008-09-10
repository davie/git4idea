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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.*;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.io.IOException;
import java.io.File;

/**
 * Git virtual file adapter
 */
public class GitVirtualFileAdaptor extends VirtualFileAdapter implements LocalFileOperationsHandler {
    private Project project;
    private GitVcs vcs;
    private static final String ADD_TITLE = "Add file";
    private static final String ADD_MESSAGE = "Add file(s) to Git?\n{0}";
    private static final String DEL_TITLE = "Delete file";
    private static final String DEL_MESSAGE = "Delete file(s) from Git?\n{0}";
    // always keep git index in sync with file changes (do "git add" on every file save)
    private boolean doChangeSync = true;

    public GitVirtualFileAdaptor(@NotNull GitVcs vcs, @NotNull Project project) {
        this.vcs = vcs;
        this.project = project;
    }

    @Override
    public void beforeContentsChange(@NotNull VirtualFileEvent event) {
    }

    @Override
    //TODO: add user config option for "Always keep Git index in sync ("git add") with every file save?"
    public void contentsChanged(@NotNull VirtualFileEvent event) {  // keep Git repo in sync
        if (!doChangeSync || event.isFromRefresh())
            return;

        final VirtualFile file = event.getFile();
        if (!isFileProcessable(file))
            return;

        VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, file);
        GitCommand command = new GitCommand(project, vcs.getSettings(), vcsRoot);
        try {
            command.add(new VirtualFile[]{event.getFile()});
        }
        catch (VcsException e) {
            List<VcsException> es = new ArrayList<VcsException>();
            es.add(e);
            GitVcs.getInstance(project).showErrors(es, "Error syncing changes to Git index!");
        }
        statusChange(file);
        GitChangeMonitor.getInstance().refresh();
    }

    @Override
    public void fileCreated(@NotNull VirtualFileEvent event) {
        if (event.isFromRefresh())
            return;

        final VirtualFile file = event.getFile();
        if (!isFileProcessable(file))
            return;

        List<VirtualFile> files = new ArrayList<VirtualFile>();
        files.add(file);
        Collection<VirtualFile> filesToAdd = new ArrayList<VirtualFile>(1);
        VcsShowConfirmationOption option = vcs.getAddConfirmation();
        VcsShowConfirmationOption.Value userOpt = option.getValue();

        switch (userOpt) {
            case SHOW_CONFIRMATION:
                AbstractVcsHelper helper = AbstractVcsHelper.getInstance(project);
                filesToAdd = helper.selectFilesToProcess(files, ADD_TITLE, null, ADD_TITLE, ADD_MESSAGE, option);
                break;
            case DO_ACTION_SILENTLY:
                filesToAdd.add(file);
                break;
            case DO_NOTHING_SILENTLY:
                return;
        }

        VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, file);
        if (filesToAdd != null && filesToAdd.size() > 0 && vcsRoot != null) {
            GitCommand command = new GitCommand(project, vcs.getSettings(), vcsRoot);
            try {
                command.add(filesToAdd.toArray(new VirtualFile[filesToAdd.size()]));
            }
            catch (VcsException e) {
                List<VcsException> es = new ArrayList<VcsException>();
                es.add(e);
                GitVcs.getInstance(project).showErrors(es, "Error adding file");
            }
        }

        statusChange(file);
    }

    @Override
    public void fileCopied(@NotNull VirtualFileCopyEvent event) {
        fileCreated(event);
    }

    @Override
    public void beforeFileDeletion(@NotNull VirtualFileEvent event) {
        if (event.isFromRefresh())
            return;

        final VirtualFile file = event.getFile();
        if (!isFileProcessable(file))
            return;

        List<VirtualFile> files = new ArrayList<VirtualFile>();
        files.add(file);
        Collection<VirtualFile> filesToDelete = new ArrayList<VirtualFile>(1);
        VcsShowConfirmationOption option = vcs.getDeleteConfirmation();
        VcsShowConfirmationOption.Value userOpt = option.getValue();

        switch (userOpt) {
            case SHOW_CONFIRMATION:
                AbstractVcsHelper helper = AbstractVcsHelper.getInstance(project);
                filesToDelete = helper.selectFilesToProcess(files, DEL_TITLE, null, DEL_TITLE, DEL_MESSAGE, option);
                break;
            case DO_ACTION_SILENTLY:
                filesToDelete.add(file);
                break;
            case DO_NOTHING_SILENTLY:
                return;
        }

//        VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, file);
//        if (filesToDelete != null && filesToDelete.size() > 0 && vcsRoot != null) {
//            GitCommand command = new GitCommand(project, vcs.getSettings(), vcsRoot);
//            try {
//                command.delete(filesToDelete.toArray(new VirtualFile[filesToDelete.size()]));
//            }
//            catch (VcsException e) {
//                List<VcsException> es = new ArrayList<VcsException>();
//                es.add(e);
//                GitVcs.getInstance(project).showErrors(es, "Error deleting file");
//            }
//        }
    }

    @Override
    public void fileDeleted(@NotNull VirtualFileEvent event) {
        statusChange(event.getFile());
    }

    @Override
    public void beforeFileMovement(@NotNull VirtualFileMoveEvent event) {
        if (event.isFromRefresh())
            return;

        final VirtualFile file = event.getFile();
        if (!isFileProcessable(file))
            return;

        VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, file);
        GitCommand command = new GitCommand(project, vcs.getSettings(), vcsRoot);
        try {
            String oldPath = event.getOldParent().getPath();
            String newPath = event.getNewParent().getPath();
            String fileName = event.getFileName();
            VirtualFile ovf = new GitVirtualFile(project, oldPath + "/" + fileName, GitVirtualFile.Status.DELETED);
            VirtualFile nvf = new GitVirtualFile(project, newPath + "/" + fileName, GitVirtualFile.Status.ADDED);
            command.move(ovf, nvf);
        }
        catch (VcsException e) {
            List<VcsException> es = new ArrayList<VcsException>();
            es.add(e);
            GitVcs.getInstance(project).showErrors(es, "Error moving file");
        }

    }

    @Override
    public void fileMoved(@NotNull VirtualFileMoveEvent event) {
        statusChange(event.getOldParent());
        statusChange(event.getNewParent());
    }

    @Override
    public void propertyChanged(@NotNull VirtualFilePropertyEvent event) {  // do nothing
    }

    @Override
    public void beforePropertyChange(@NotNull VirtualFilePropertyEvent event) {
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LocalOperationsHandler
    public boolean delete(VirtualFile file) throws IOException {
        VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, file);
        GitCommand command = new GitCommand(project, vcs.getSettings(), vcsRoot);
        try {
            command.delete(new VirtualFile[]{file});
        }
        catch (VcsException e) {
            throw new IOException("Error deleting file!", e);
        }
        return true;
    }

    public boolean move(VirtualFile file, VirtualFile toDir) throws IOException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nullable
    public File copy(VirtualFile file, VirtualFile toDir, String copyName) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean rename(VirtualFile file, String newName) throws IOException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean createFile(VirtualFile dir, String name) throws IOException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean createDirectory(VirtualFile dir, String name) throws IOException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private methods

    /**
     * File is not processable if it is outside the vcs scope or it is in the
     * list of excluded project files.
     *
     * @param file The file to check.
     * @return Returns true of the file can be added.
     */
    private boolean isFileProcessable(VirtualFile file) {
        return VcsUtil.isFileForVcs(file, project, vcs) && !file.getName().contains(".git");
    }

    private void statusChange(@NotNull VirtualFile file) {
        if (file.isDirectory())
            VcsDirtyScopeManager.getInstance(project).dirDirtyRecursively(file);
        else
            VcsDirtyScopeManager.getInstance(project).fileDirty(file);
    }
}