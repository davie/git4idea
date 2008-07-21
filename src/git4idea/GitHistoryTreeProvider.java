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
 *
 * This code was originally derived from the MKS IDEA VCS plugin
 */
import com.intellij.openapi.vcs.history.HistoryAsTreeProvider;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.util.TreeItem;

import java.util.*;

public class GitHistoryTreeProvider implements HistoryAsTreeProvider {

        public GitHistoryTreeProvider() {
        }

        /**
         * It seems we should only return roots
         *
         * @param allRevisions all the revisions to be viewed as a tree (will become node/treeitems)
         * @return a list of the roots, usually the only root (aka revision 1.1)
         */
        @SuppressWarnings({"EmptyCatchBlock"})
        public List<TreeItem<VcsFileRevision>> createTreeOn(List<VcsFileRevision> allRevisions) {
                Map<VcsRevisionNumber, TreeItem<VcsFileRevision>> treeItemMap =
                                new HashMap<VcsRevisionNumber, TreeItem<VcsFileRevision>>();
                Map<VcsRevisionNumber, VcsFileRevision> revisionsByRevNumber =
                                new HashMap<VcsRevisionNumber, VcsFileRevision>();
                for (VcsFileRevision revision : allRevisions) {
                        revisionsByRevNumber.put(revision.getRevisionNumber(), revision);
                }
                // first order the revisions, so we can simply process the list and be sure
                // parent revisions have been processed first
                List<VcsRevisionNumber> orderedRevisions = new ArrayList<VcsRevisionNumber>(revisionsByRevNumber.keySet());
                List<TreeItem<VcsFileRevision>> result = new ArrayList<TreeItem<VcsFileRevision>>(orderedRevisions.size());
                Collections.sort(orderedRevisions);

                for (VcsRevisionNumber revisionNumber : orderedRevisions) {
                        VcsFileRevision revision = revisionsByRevNumber.get(revisionNumber);
                        TreeItem<VcsFileRevision> treeItem = new TreeItem<VcsFileRevision>(revision);
                        treeItemMap.put(revisionNumber, treeItem);
                        result.add(treeItem);
                        // now look for parents and set parent/child relationships
                        String parentRevString = ((GitRevisionNumber) revision.getRevisionNumber()).getParentRevisionStr();

                        try {
                                if (parentRevString != null && !"".equals(parentRevString)) {
                                        final TreeItem<VcsFileRevision> parentItem;
                                        parentItem = treeItemMap.get(GitRevisionNumber.createRevision(parentRevString));
                                        if (parentItem == null) {
                                        } else {
//                                      parentItem.addChild(treeItem);
                                                // we want to keep newer revisions on top, thus reverse the order
                                                parentItem.addChild(treeItem);
//                                              treeItem.addChild(parentItem);
                                                // remove children so they don't appear multiple times
                                                if (result.contains(treeItem)) {
                                                        result.remove(treeItem);
                                                }
                                        }
                                }
                        } catch (Exception e) {
                        }
                }
                return result;
        }
}