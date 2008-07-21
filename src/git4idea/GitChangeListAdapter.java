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

import com.intellij.openapi.vcs.changes.ChangeListAdapter;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.Change;

import java.util.Collection;

/**
 * Git change list adapter
 */
public class GitChangeListAdapter extends ChangeListAdapter {
    private GitVcs vcs = null;

    public GitChangeListAdapter(GitVcs vcs) {
        this.vcs = vcs;
    }

    public void changeListAdded(ChangeList list) {
    }

    public void changeListRemoved(ChangeList list) {
    }

    public void changeListChanged(ChangeList list) {
    }

    public void changeListRenamed(ChangeList list, String oldName) {
    }

    public void changeListCommentChanged(final ChangeList list, final String oldComment) {
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
