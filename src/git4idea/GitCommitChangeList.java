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
 * Author: Anatol Pomozov (Copyright 2008)
 */
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Date;

/**
 * Git change list
 */
public class GitCommitChangeList implements CommittedChangeList {
    private final String commiterName;
    private final Date commitDate;
    private final GitVcs vcs;
    private final Collection<Change> changes;
    private final String comment;

    public GitCommitChangeList(GitVcs vcs, GitFileRevision gitRevision, Collection<Change> changes) {
        this.vcs = vcs;
        this.commiterName = gitRevision.getAuthor();
        this.commitDate = gitRevision.getRevisionDate();
        this.changes = changes;
        this.comment = gitRevision.getCommitMessage();
    }

    public String getCommitterName() {
        return commiterName;
    }

    public Date getCommitDate() {
        return commitDate;
    }

    public long getNumber() {
        return 0;
    }

    public GitVcs getVcs() {
        return vcs;
    }

    public Collection<Change> getChanges() {
        return changes;
    }

    @NotNull
    public String getName() {
        return comment;
    }

    public String getComment() {
        return comment;
    }
}