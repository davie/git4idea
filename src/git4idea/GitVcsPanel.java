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
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.jetbrains.annotations.NotNull;

/**
 * Git VCS configuration panel
 */
public class GitVcsPanel {
    private JButton testButton;
    private JComponent panel;
    private TextFieldWithBrowseButton gitField;
    private JSpinner intervalSpinner;
    private Project project;

    public GitVcsPanel(@NotNull Project project) {
        this.project = project;
        testButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        testConnection();
                    }
                });

        gitField.addBrowseFolderListener(
                "Git Configuration",
                "Select path to git executable",
                project,
                new FileChooserDescriptor(true, false, false, false, false, false));
    }

    private void testConnection() {
        final GitVcsSettings settings = new GitVcsSettings();
        settings.GIT_EXECUTABLE = gitField.getText();
        final VirtualFile baseDir = project.getBaseDir();
        assert baseDir != null;
        final GitCommand command = new GitCommand(project, settings, baseDir);
        final String s;

        try {
            s = command.version();
        }
        catch (VcsException e) {
            Messages.showErrorDialog(project, e.getMessage(), "Error Running git");
            return;
        }
        Messages.showInfoMessage(project, s, "Git Executed Successfully");
    }

    public JComponent getPanel() {
        return panel;
    }

    public void load(@NotNull GitVcsSettings settings) {
        gitField.setText(settings.GIT_EXECUTABLE);
        intervalSpinner.setValue(settings.GIT_INTERVAL);
    }

    public boolean isModified(@NotNull GitVcsSettings settings) {
        return !settings.GIT_EXECUTABLE.equals(gitField.getText()) ||
                !settings.GIT_INTERVAL.equals(intervalSpinner.getValue());
    }

    public void save(@NotNull GitVcsSettings settings) {
        settings.GIT_EXECUTABLE = gitField.getText();
        Integer val = (Integer) intervalSpinner.getValue();
        if (val < 10)
            settings.GIT_INTERVAL = 10;
        else
            settings.GIT_INTERVAL = val;

       GitChangeMonitor.getInstance().setGitVcsSettings(settings);
    }
}