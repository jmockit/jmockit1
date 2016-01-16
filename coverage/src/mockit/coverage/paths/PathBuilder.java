/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.paths;

import java.util.*;
import javax.annotation.*;

import mockit.coverage.paths.Node.*;

final class PathBuilder
{
   private PathBuilder() {}

   @Nonnull
   static List<Path> buildPaths(@Nonnull List<Node> nodes)
   {
      if (nodes.size() == 1) {
         return Collections.emptyList();
      }

      Entry entryNode = (Entry) nodes.get(0);
      Path path = new Path(entryNode);

      ConditionalSuccessor nextNode = entryNode.nextNode;

      if (nextNode == null) {
         nextNode = (ConditionalSuccessor) nodes.get(1);
      }

      nextNode.addToPath(path);

      return getAllPathsFromExitNodes(nodes);
   }

   @Nonnull
   private static List<Path> getAllPathsFromExitNodes(@Nonnull List<Node> nodes)
   {
      List<Path> paths = new ArrayList<Path>();

      for (Node node : nodes) {
         if (node instanceof Exit) {
            paths.addAll(((Exit) node).paths);
         }
      }

      return paths;
   }
}
