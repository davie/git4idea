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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;

import java.io.File;

/**
 * Git VCS settings
 */
@State(
        name = "Git.Settings",
        storages = {
        @Storage(
                id = "ws",
                file = "$WORKSPACE_FILE$"
        )}
)
public class GitVcsSettings implements PersistentStateComponent<GitVcsSettings> {
    public static final String DEFAULT_CYGWIN_GIT_EXEC = "C:\\cygwin\\bin\\git.exe";
    public static final String DEFAULT_MSYS_GIT_EXEC = "C:\\Program Files\\Git\\bin\\git.exe";
    public static final String DEFAULT_MAC_GIT_EXEC = "/usr/local/bin/git";
    public static final String DEFAULT_UNIX_GIT_EXEC = "/usr/bin/git";
    public static final String DEFAULT_GIT_EXEC = "git";
    public String GIT_EXECUTABLE = defaultGit();

    @Override
    public GitVcsSettings getState() {
        return this;
    }

    @Override
    public void loadState(GitVcsSettings gitVcsSettings) {
        XmlSerializerUtil.copyBean(gitVcsSettings, this);
    }

    public static GitVcsSettings getInstance(Project project) {
        return ServiceManager.getService(project, GitVcsSettings.class);
    }

    private String defaultGit() {
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            File exe = new File(DEFAULT_CYGWIN_GIT_EXEC);   // Look for Cygwin Git first
            if (exe.exists()) return exe.getAbsolutePath();
            exe = new File(DEFAULT_MSYS_GIT_EXEC);          // Look for Msys Git second
            if (exe.exists()) return exe.getAbsolutePath();
        } else if (os.startsWith("Mac")) {
            File exe = new File(DEFAULT_MAC_GIT_EXEC);
            if (exe.exists()) return exe.getAbsolutePath();
        } else {
            File exe = new File(DEFAULT_UNIX_GIT_EXEC);
            if (exe.exists()) return exe.getAbsolutePath();
        }
        return DEFAULT_GIT_EXEC;     // otherwise, hope it's in $PATH
    }
}