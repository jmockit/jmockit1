/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.paths;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import javax.annotation.*;

public final class Path implements Serializable
{
   private static final long serialVersionUID = 8895491272907955543L;

   @Nonnull final List<Node> nodes = new ArrayList<Node>(4);
   @Nonnull private final AtomicInteger executionCount = new AtomicInteger();
   private final boolean shadowed;
   @Nullable private Path shadowPath;

   Path(@Nonnull Node.Entry entryNode)
   {
      shadowed = false;
      addNode(entryNode);
   }

   Path(@Nonnull Path sharedSubPath, boolean shadowed)
   {
      this.shadowed = shadowed;
      sharedSubPath.shadowPath = shadowed ? this : null;
      nodes.addAll(sharedSubPath.nodes);
   }

   void addNode(@Nonnull Node node) { nodes.add(node); }

   int countExecutionIfAllNodesWereReached(@Nonnull List<Node> nodesReached)
   {
      boolean allNodesReached = nodes.equals(nodesReached);

      if (allNodesReached) {
         return executionCount.getAndIncrement();
      }

      return -1;
   }

   public boolean isShadowed() { return shadowed; }
   @Nonnull public List<Node> getNodes() { return nodes; }

   public int getExecutionCount()
   {
      int count = executionCount.get();

      if (shadowPath != null) {
         count += shadowPath.executionCount.get();
      }

      return count;
   }

   void addCountFromPreviousTestRun(@Nonnull Path previousPath)
   {
      int currentExecutionCount = executionCount.get();
      int previousExecutionCount = previousPath.executionCount.get();
      executionCount.set(currentExecutionCount + previousExecutionCount);
   }
}
