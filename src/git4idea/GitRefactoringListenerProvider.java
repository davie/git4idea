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
 * Copyright 2008 MQSoftware
 * Authors: Mark Scott
 */
import com.intellij.refactoring.listeners.RefactoringElementListenerProvider;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Refactoring listener provider for Git.
 */
public class GitRefactoringListenerProvider implements RefactoringElementListenerProvider {
    GitRefactoringListenerProvider() {
    }

    /**
     * Should return a listener for particular element. Invoked in read action.
     */
    public RefactoringElementListener getListener(PsiElement element) {
        if (!(element instanceof PsiFile)) return null;
        PsiFile pFile = (PsiFile)element;
        VirtualFile vFile = pFile.getVirtualFile();
        return new GitRenameListener(vFile.getPath(), element);
    }
}