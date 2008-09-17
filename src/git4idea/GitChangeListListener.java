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
 * Author: Mark Scott
 */
import com.intellij.openapi.vcs.changes.ChangeListListener;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.project.Project;

import java.util.Collection;

/**
 * Git change list listener
 *
 * NOTE: probably unneeded for git...
 */
public class GitChangeListListener implements ChangeListListener {

    public GitChangeListListener(Project project) {
        //ChangeListManager.getInstance(project).addChangeListListener(this);
    }
    
    public void changeListAdded(ChangeList list) {
    }

    public void changeListRemoved(ChangeList list) {
    }

    public void changeListChanged(ChangeList list) {
    }

    public void changeListRenamed(ChangeList list, String oldName) {
    }

    public void changeListCommentChanged(ChangeList list, String oldComment) {
    }

    public void changesMoved(Collection<Change> changes, ChangeList fromList, ChangeList toList) {
    }

    public void defaultListChanged(ChangeList newDefaultList) {
    }

    public void unchangedFileStatusChanged() {
    }

    public void changeListUpdateDone() {
    }
}