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
 * Authors: mike.aizatsky,gevession, Erlend Simonsen & Mark Scott
 *
 * This code was originally derived from the MKS & Mercurial IDEA VCS plugins
 */
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Git revision graph editor
 */
public class GitRevisionGraphEditorProvider implements FileEditorProvider {
    //TODO: implement this whole class!!  Do we really need this????
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file instanceof GitVirtualFile;
    }

    @NotNull
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        throw new UnsupportedOperationException("Method createEditor not implemented in " + getClass());
    }

    public void disposeEditor(@NotNull FileEditor editor) {
        throw new UnsupportedOperationException("Method disposeEditor not implemented in " + getClass());
    }

    @NotNull
    public FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile file) {
        throw new UnsupportedOperationException("Method readState not implemented in " + getClass());
    }

    public void writeState(@NotNull FileEditorState state, @NotNull Project project, @NotNull Element targetElement) {
        throw new UnsupportedOperationException("Method writeState not implemented in " + getClass());
    }

    @NotNull
    @NonNls
    public String getEditorTypeId() {
        return "GitRevisionGraph";
    }

    @NotNull
    public FileEditorPolicy getPolicy() {
        throw new UnsupportedOperationException("Method getPolicy not implemented in " + getClass());
    }
}