/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.paths;

import java.io.*;
import java.util.*;
import javax.annotation.*;

import mockit.coverage.paths.Node.*;

public final class MethodCoverageData implements Serializable
{
   private static final long serialVersionUID = -5073393714435522417L;

   @Nonnull private List<Node> nodes;
   @Nonnegative private int firstLine;
   @Nonnegative private int lastLine;

   // Helper fields used during node building and path execution:
   @Nonnull private final transient ThreadLocal<List<Node>> nodesReached;
   @Nonnull private final transient ThreadLocal<Integer> previousNodeIndex;

   @Nonnull public List<Path> paths;
   @Nonnull private List<Path> nonShadowedPaths;

   public MethodCoverageData() {
      nodes = Collections.emptyList();
      paths = Collections.emptyList();
      nonShadowedPaths = Collections.emptyList();
      nodesReached = new ThreadLocal<>();
      previousNodeIndex = new ThreadLocal<>();
      clearNodes();
   }

   public void buildPaths(@Nonnegative int lastExecutableLine, @Nonnull NodeBuilder nodeBuilder) {
      firstLine = nodeBuilder.firstLine;
      lastLine = lastExecutableLine;

      nodes = nodeBuilder.nodes;
      paths = PathBuilder.buildPaths(nodes);
      buildListOfNonShadowedPaths();
   }

   private void buildListOfNonShadowedPaths() {
      nonShadowedPaths = new ArrayList<>(paths.size());

      for (Path path : paths) {
         if (!path.isShadowed()) {
            nonShadowedPaths.add(path);
         }
      }
   }

   @Nonnegative public int getFirstLineInBody() { return firstLine; }
   @Nonnegative public int getLastLineInBody() { return lastLine; }

   int markNodeAsReached(@Nonnegative int nodeIndex) {
      if (nodeIndex == 0) {
         clearNodes();
      }

      if (nodeIndex < nodes.size()) {
         Node node = nodes.get(nodeIndex);
         List<Node> currentNodesReached = nodesReached.get();

         if (!node.wasReached() && (nodeIndex == 0 || nodeIndex > previousNodeIndex.get())) {
            node.setReached(Boolean.TRUE);
            currentNodesReached.add(node);
            previousNodeIndex.set(nodeIndex);
         }

         if (node instanceof Exit) {
            Exit exitNode = (Exit) node;

            for (Path path : exitNode.paths) {
               int previousExecutionCount = path.countExecutionIfAllNodesWereReached(currentNodesReached);

               if (previousExecutionCount >= 0) {
                  return previousExecutionCount;
               }
            }
         }
      }

      return -1;
   }

   private void clearNodes() {
      for (Node node : nodes) {
         node.setReached(null);
      }

      nodesReached.set(new ArrayList<Node>());
      previousNodeIndex.set(0);
   }

   @Nonnull public List<Path> getPaths() { return nonShadowedPaths; }

   @Nonnegative
   public int getExecutionCount() {
      int totalCount = 0;

      for (Path path : nonShadowedPaths) {
         totalCount += path.getExecutionCount();
      }

      return totalCount;
   }

   @Nonnegative
   public int getTotalPaths() { return nonShadowedPaths.size(); }

   @Nonnegative
   public int getCoveredPaths() {
      int coveredCount = 0;

      for (Path path : nonShadowedPaths) {
         if (path.getExecutionCount() > 0) {
            coveredCount++;
         }
      }

      return coveredCount;
   }

   void addCountsFromPreviousTestRun(@Nonnull MethodCoverageData previousData) {
      for (int i = 0; i < paths.size(); i++) {
         Path path = paths.get(i);
         Path previousPath = previousData.paths.get(i);
         path.addCountFromPreviousTestRun(previousPath);
      }
   }
}
