/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.paths;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.jetbrains.annotations.*;

public final class Path implements Serializable
{
   private static final long serialVersionUID = 8895491272907955543L;

   @NotNull final List<Node> nodes = new ArrayList<Node>(4);
   @NotNull private final AtomicInteger executionCount = new AtomicInteger();
   private final boolean shadowed;
   @Nullable private Path shadowPath;

   Path(@NotNull Node.Entry entryNode)
   {
      shadowed = false;
      addNode(entryNode);
   }

   Path(@NotNull Path sharedSubPath, boolean shadowed)
   {
      this.shadowed = shadowed;
      sharedSubPath.shadowPath = shadowed ? this : null;
      nodes.addAll(sharedSubPath.nodes);
   }

   void addNode(@NotNull Node node) { nodes.add(node); }

   int countExecutionIfAllNodesWereReached(@NotNull List<Node> nodesReached)
   {
      boolean allNodesReached = nodes.equals(nodesReached);

      if (allNodesReached) {
         return executionCount.getAndIncrement();
      }

      return -1;
   }

   public boolean isShadowed() { return shadowed; }
   @NotNull public List<Node> getNodes() { return nodes; }

   public int getExecutionCount()
   {
      int count = executionCount.get();

      if (shadowPath != null) {
         count += shadowPath.executionCount.get();
      }

      return count;
   }

   void addCountFromPreviousTestRun(@NotNull Path previousPath)
   {
      int currentExecutionCount = executionCount.get();
      int previousExecutionCount = previousPath.executionCount.get();
      executionCount.set(currentExecutionCount + previousExecutionCount);
   }

   void reset()
   {
      executionCount.set(0);
   }
}
