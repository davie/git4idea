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

import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * Git revision number
 */
public class GitRevisionNumber implements VcsRevisionNumber {
    public static final String TIP = "HEAD";
    private String revisionStr;
    Date timestamp;

    public GitRevisionNumber() {
        revisionStr = TIP;
        timestamp = new Date();
    }

    public GitRevisionNumber(@NotNull String version) {
        this.revisionStr = version;
        this.timestamp = new Date();
    }

    public GitRevisionNumber(@NotNull String version, @NotNull Date timeStamp) {
        this.timestamp = timeStamp;
        this.revisionStr = version;
    }

    @Override
    public String asString() {
        return revisionStr;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getRev() {
        return revisionStr;
    }

    public String getShortRev() {
        if(revisionStr == null || revisionStr.length() == 0)
            return "";
        if(revisionStr.length() == 40)
            return revisionStr.substring(0, 8);
        if(revisionStr.length() > 40)  // revision string encoded with date too
            return revisionStr.substring(revisionStr.indexOf("[") + 1, 8);
        return revisionStr;
    } 

    @Override
    public int compareTo(VcsRevisionNumber crev) {
        if (this == crev) return 0;

        if (crev instanceof GitRevisionNumber) {
            GitRevisionNumber crevg = (GitRevisionNumber) crev;
            if ((revisionStr != null) && (crevg.revisionStr != null) && revisionStr.equals(crevg.revisionStr))
                return timestamp.compareTo(crevg.timestamp);

            if((crevg.revisionStr.indexOf("[") > 0) && (timestamp != null && crevg.timestamp != null))
               return timestamp.compareTo(crevg.timestamp);

            // check for parent revs
            String crevName = null;
            String revName = null;
            int crevNum = -1;
            int revNum = -1;

            if (crevg.revisionStr.contains("~")) {
                int tildeIdx = crevg.revisionStr.indexOf('~');
                crevName = crevg.revisionStr.substring(0, tildeIdx);
                crevNum = Integer.parseInt(crevg.revisionStr.substring(tildeIdx));
            }

            if (revisionStr.contains("~")) {
                int tildeIdx = revisionStr.indexOf('~');
                revName = revisionStr.substring(0, tildeIdx);
                revNum = Integer.parseInt(revisionStr.substring(tildeIdx));
            }

            if( crevName == null && revName == null) {
                return timestamp.compareTo(crevg.timestamp);
            } else if( crevName == null && revName != null) {
                return 1;  // I am an ancestor of the compared revision
            } else if( crevName != null && revName == null) {
                return -1; // the compared revision is my ancestor
            } else if( crevName != null && revName != null) {
                return revNum - crevNum;  // higher relative rev numbers are older ancestors
            }
        }

        return -1;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if ((obj == null) || (obj.getClass() != this.getClass()))
            return false;

        GitRevisionNumber test = (GitRevisionNumber) obj;
        if (revisionStr != null && test.revisionStr != null)
            return revisionStr.equals(test.revisionStr);

        return (timestamp == test.timestamp);
    }

    public int hashCode() {
        if (revisionStr != null)
            return revisionStr.hashCode();
        if(timestamp != null)
            return timestamp.hashCode();

        return 1;
    }

    public String getParentRevisionStr() {
        String rev = revisionStr;
        int bracketIdx = rev.indexOf("[");
        if(bracketIdx > 0) {
            rev = revisionStr.substring(bracketIdx + 1, revisionStr.indexOf("]"));
        }

        int tildeIdx = rev.indexOf("~");
        if (tildeIdx > 0) {
            int n = Integer.parseInt(rev.substring(tildeIdx)) + 1;
            return rev.substring(0, tildeIdx) + "~" + n;
        }
        return rev + "~1";
    }

    public static GitRevisionNumber createRevision(String rev) {
        return new GitRevisionNumber(rev);
    }
}