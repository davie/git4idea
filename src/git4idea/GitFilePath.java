package git4idea;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;

/**
 * Git FilePath implementation.
 */
public class GitFilePath implements FilePath {
    private GitVirtualFile vfile;

    public GitFilePath(@NotNull GitVirtualFile vfile) {
        this.vfile = vfile;

    }

    public VirtualFile getVirtualFile() {
        return vfile;
    }

    public VirtualFile getVirtualFileParent() {
        return vfile.getParent();
    }

    @NotNull
    public File getIOFile() {
        return new File(vfile.getPath());
    }

    public String getName() {
        return vfile.getName();
    }

    public String getPresentableUrl() {
        return vfile.getUrl();
    }

    @Nullable
    public Document getDocument() {
        return null;      // *sigh, hopefully we don't need these...
    }

    public Charset getCharset() {
        return Charset.defaultCharset();
    }

    public FileType getFileType() {
        return FileTypeManager.getInstance().getFileTypeByFile(vfile);
    }

    public void refresh() {
    }

    public String getPath() {
        return vfile.getPath();
    }

    public boolean isDirectory() {
        return vfile.getFile().isDirectory();
    }

    public boolean isUnder(FilePath parent, boolean strict) {
        String purl = parent.getPresentableUrl();
        String url = getPresentableUrl();
        return purl.startsWith(url);
    }

    @Nullable
    public FilePath getParentPath() {
        return new GitFilePath(new GitVirtualFile(vfile.getProject(), vfile.getFile().getParent()));
    }

    public boolean isNonLocal() {
        return false;
    }
}