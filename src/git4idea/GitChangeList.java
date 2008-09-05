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

import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.Change;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

/**
 * Git change list
 */
public class GitChangeList extends LocalChangeList {
    private String name;
    private String comment;
    private Collection<Change> changes;
    private boolean isdefault = true;
    private boolean isreadonly = false;

    public GitChangeList(@NotNull String name, String comment, Collection<Change> changes) {
        super();
        setName(name);
        setComment(comment);
        this.changes = changes;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isDefault() {
        return isdefault;
    }

    public boolean isInUpdate() {
        return false;
    }

    public boolean isReadOnly() {
        return isreadonly;
    }

    public void setDefault(boolean isDefault) {
        isdefault = isDefault;
    }

    public void setReadOnly(boolean isReadOnly) {
        isreadonly = isReadOnly;
    }

    public Collection<Change> getChanges() {
        return changes;
    }

    public void setChanges(Collection<Change> newChanges) {
        if(!isreadonly)
            changes = newChanges;
    }

    public LocalChangeList clone() {
        return new GitChangeList(name, comment, changes);
    }
}