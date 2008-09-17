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

import com.intellij.openapi.vcs.vfs.VcsFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

/**
 * Git VCS filesystem implementation
 */
public class GitFileSystem extends VcsFileSystem {
    private static GitFileSystem gitfs = new GitFileSystem();
    static final String PROTOCOL = "git";
    static final String PREFIX_REV_GRAPH = "revg";

    // per project, store a cache map of path->vfile mappings
    private Map<Project, Map<String, VirtualFile>> projects = new HashMap<Project, Map<String, VirtualFile>>();

    public static GitFileSystem getInstance() {
        return gitfs;
    }

    public VirtualFile findFileByPath(@NotNull Project project, @NotNull final String path) {
        Map<String, VirtualFile> cache = projects.get(project);
        if (cache == null) {
            cache = new HashMap<String, VirtualFile>();
            projects.put(project, cache);
        }

        if (cache.containsKey(path)) return cache.get(path);

        final GitVirtualFile file = new GitVirtualFile(project, path);
        cache.put(path, file);
        return file;
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    @NotNull
    public String getComponentName() {
        return "GitFileSystem";
    }

    @Override
    public void refresh(boolean asynchronous) {
    }

    @Override
    public VirtualFile refreshAndFindFileByPath(String path) {
        for (Map<String, VirtualFile> cache : projects.values()) {
            if (cache.containsKey(path)) return cache.get(path);
        }
        return null;
    }

    @Override
    public void fireContentsChanged(Object requestor, VirtualFile file, long oldModificationStamp) {
        super.fireContentsChanged(requestor, file, oldModificationStamp);
    }

    @Override
    protected void fireBeforeFileDeletion(Object requestor, VirtualFile file) {
        super.fireBeforeFileDeletion(requestor, file);
    }

    @Override
    protected void fireFileDeleted(Object requestor, VirtualFile file, String fileName, VirtualFile parent) {
        super.fireFileDeleted(requestor, file, fileName, parent);
    }

    @Override
    public void initComponent() {
        super.initComponent();
    }

    @Override
    public void disposeComponent() {
        super.disposeComponent();
    }

    @Override
    protected void fireBeforeContentsChange(Object requestor, VirtualFile file) {
        super.fireBeforeContentsChange(requestor, file);
    }

    @Override
    public void deleteFile(Object requestor, VirtualFile file) throws IOException {
        // Git delete happens in GitVirtualFileAdapter.beforeFileDeletion()
    }

    @Override
    public void moveFile(Object requestor, VirtualFile vFile, VirtualFile newParent) throws IOException {
        // Git file is moved in GitVirtualFileAdapter.beforeFileMovement()
    }

    @Override
    public VirtualFile copyFile(Object requestor, VirtualFile vFile, VirtualFile newParent, final String copyName) throws IOException {
        throw new RuntimeException(COULD_NOT_IMPLEMENT_MESSAGE);
    }

    @Override
    public void renameFile(Object requestor, VirtualFile file, String newName) throws IOException {
       // Git file is renamed in GitVirtualFileAdapter.rename()
    }

    @Override
    public VirtualFile createChildFile(Object requestor, VirtualFile vDir, String fileName) throws IOException {
        throw new RuntimeException(COULD_NOT_IMPLEMENT_MESSAGE);
    }

    @Override
    public VirtualFile createChildDirectory(Object requestor, VirtualFile vDir, String dirName) throws IOException {
        throw new RuntimeException(COULD_NOT_IMPLEMENT_MESSAGE);
    }
}