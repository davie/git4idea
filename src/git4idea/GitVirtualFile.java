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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.log4j.lf5.util.StreamUtils;

import java.io.*;

/**
 * Git implementation of VCS virtual file
 */
public class GitVirtualFile extends VirtualFile {
    private final Project project;
    private final String path;
    private final String URL;
    private File file;
    private Status status;
    private String fileSep = System.getProperty("file.separator", "/");


    public GitVirtualFile(@NotNull Project project, @NotNull String path, @NotNull Status status) {
        this(project, path);
        this.status = status;
    }

    public GitVirtualFile(@NotNull Project project, @NotNull String path) {
        this.project = project;
        String p = path.replace("/", fileSep);
        this.file = new File(p);
        this.path = this.file.getAbsolutePath().replace("\\", "/");
        this.URL = "file://" + this.path;
    }

    @NotNull
    public Status getStatus() {
        return status;
    }

    public void setStatus(@NotNull Status status) {
        this.status = status;
    }

    @Override
    @NotNull
    public String getName() {
        return file.getName();
    }

    @Override
    @NotNull
    public VirtualFileSystem getFileSystem() {
        return GitFileSystem.getInstance();
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    @NotNull
    public String getUrl() {
        return URL;
    }

    @Override
    public boolean isWritable() {
        return file.canWrite();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean isValid() {
        return file.exists();
    }

    @Override
    @Nullable
    public VirtualFile getParent() {
        String parent = file.getParent();
        if(parent == null) return null;
        return new GitVirtualFile(project, parent);
    }

    @Override
    @Nullable
    public VirtualFile[] getChildren() {
        if (file.isDirectory()) {
            String[] list = file.list();
            if (list == null || list.length == 0)
                return null;
            VirtualFile[] files = new VirtualFile[list.length];
            for (int i = 0; i < list.length; i++) {
                files[i] = new GitVirtualFile(project, list[i]);
            }
            return files;
        } else {
            return null;
        }
    }

    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        return new FileOutputStream(path);
    }

    @Override
    public byte[] contentsToByteArray() throws IOException {
        return StreamUtils.getBytes(getInputStream());
    }

    @Override
    public long getTimeStamp() {
        return file.lastModified();
    }

    @Override
    public long getModificationStamp() {
        return file.lastModified();
    }

    public File getFile() {
        return file;
    }

    @Override
    public long getLength() {
        return file.length();
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) {
        file = new File(getPath());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(path);
    }

    @Override
    public boolean isInLocalFileSystem() {
        return file.exists();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof VirtualFile))
            return false;

        VirtualFile that = (VirtualFile) obj;
        return this == that || getUrl().equals(that.getUrl());
    }

    public int hashCode() {
        if(path == null) return -1;
        return getUrl().hashCode();
    }

    public String toString() {
        return getUrl();
    }

    Project getProject() {
        return project;
    }

    public enum Status {
        MODIFIED,
        COPY,
        RENAME,
        ADDED,
        DELETED,
        UNMERGED,
        UNMODIFIED,
        UNVERSIONED
    }
}