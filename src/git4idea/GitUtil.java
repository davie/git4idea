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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Git utility/helper methods
 */
public class GitUtil {

    @NotNull
    public static VirtualFile getVcsRoot(@NotNull final Project project, @NotNull final FilePath filePath) {
        VirtualFile vfile = VcsUtil.getVcsRootFor(project, filePath);
        if (vfile == null)
            vfile = GitFileSystem.getInstance().findFileByPath(project,filePath.getPath());

        return vfile;
    }

    @NotNull
    public static VirtualFile getVcsRoot(@NotNull final Project project, final VirtualFile virtualFile) {
        VirtualFile vfile = VcsUtil.getVcsRootFor(project, virtualFile);
        if (vfile == null)
            vfile = GitFileSystem.getInstance().findFileByPath(project, virtualFile.getPath());
        return vfile;
    }

    @NotNull
    public static Map<VirtualFile, List<VirtualFile>> sortFilesByVcsRoot(
            @NotNull Project project,
            @NotNull List<VirtualFile> virtualFiles) {
        Map<VirtualFile, List<VirtualFile>> result = new HashMap<VirtualFile, List<VirtualFile>>();

        for (VirtualFile file : virtualFiles) {
            final VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, file);
            assert vcsRoot != null;

            List<VirtualFile> files = result.get(vcsRoot);
            if (files == null) {
                files = new ArrayList<VirtualFile>();
                result.put(vcsRoot, files);
            }
            files.add(file);
        }

        return result;
    }

    @NotNull
    public static Map<VirtualFile, List<VirtualFile>> sortFilesByVcsRoot(Project project, VirtualFile[] affectedFiles) {
        return sortFilesByVcsRoot(project, Arrays.asList(affectedFiles));
    }
}