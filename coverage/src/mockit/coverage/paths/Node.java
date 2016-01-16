/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.paths;

import java.util.*;
import java.io.*;
import javax.annotation.*;

@SuppressWarnings("ClassReferencesSubclass")
public class Node implements Serializable
{
   private static final long serialVersionUID = 7521062699264845946L;

   @Nonnull private final transient ThreadLocal<Boolean> reached = new ThreadLocal<Boolean>();
   public final int line;
   protected int segment;

   private Node(int line) { this.line = line; }

   void setSegmentAccordingToPrecedingNode(@Nonnull Node precedingNode)
   {
      int currentSegment = precedingNode.segment;
      segment = precedingNode instanceof Fork ? currentSegment + 1 : currentSegment;
   }

   public final int getSegment() { return segment; }

   final void setReached(@Nullable Boolean reached) { this.reached.set(reached); }
   final boolean wasReached() { return reached.get() != null; }

   @Override
   public final String toString() { return getClass().getSimpleName() + ':' + line + '-' + segment; }

   static final class Entry extends Node
   {
      private static final long serialVersionUID = -3065417917872259568L;
      @Nullable Fork nextNode;

      Entry(int entryLine) { super(entryLine); }
   }

   interface ConditionalSuccessor extends Serializable
   {
      void addToPath(@Nonnull Path path);
   }

   interface GotoSuccessor extends Serializable
   {
      void setNextNodeAfterGoto(@Nonnull Join newJoin);
   }

   static final class Exit extends Node implements ConditionalSuccessor
   {
      private static final long serialVersionUID = -4801498566218642509L;
      @Nonnull final List<Path> paths = new ArrayList<Path>(4);

      Exit(int exitLine) { super(exitLine); }

      @Override
      public void addToPath(@Nonnull Path path)
      {
         path.addNode(this);
         paths.add(path);
      }
   }

   static final class BasicBlock extends Node implements ConditionalSuccessor, GotoSuccessor
   {
      private static final long serialVersionUID = 2637678937923952603L;
      @Nullable ConditionalSuccessor nextConsecutiveNode;
      @Nullable private Join nextNodeAfterGoto;

      BasicBlock(int startingLine) { super(startingLine); }

      @Override @SuppressWarnings("NullableProblems")
      public void setNextNodeAfterGoto(@Nonnull Join newJoin) { nextNodeAfterGoto = newJoin; }

      @Override
      public void addToPath(@Nonnull Path path)
      {
         path.addNode(this);

         if (nextNodeAfterGoto != null) {
            assert nextConsecutiveNode == null;
            nextNodeAfterGoto.addToPath(path);
         }
         else if (nextConsecutiveNode != null) {
            nextConsecutiveNode.addToPath(path);
         }
      }
   }

   public abstract static class Fork extends Node implements ConditionalSuccessor
   {
      private static final long serialVersionUID = -4995089238476806249L;

      Fork(int line) { super(line); }

      abstract void addNextNode(@Nonnull Join nextNode);

      static void createAlternatePath(@Nonnull Path parentPath, @Nonnull Join targetJoin)
      {
         Path alternatePath = new Path(parentPath, targetJoin.fromTrivialFork);
         targetJoin.addToPath(alternatePath);
      }
   }

   static final class SimpleFork extends Fork
   {
      private static final long serialVersionUID = -521666665272332763L;
      @Nullable ConditionalSuccessor nextConsecutiveNode;
      @Nullable private Join nextNodeAfterJump;

      SimpleFork(int line) { super(line); }

      @Override
      void addNextNode(@Nonnull Join nextNode) { nextNodeAfterJump = nextNode; }

      @Override
      public void addToPath(@Nonnull Path path)
      {
         path.addNode(this);

         if (nextNodeAfterJump != null) {
            createAlternatePath(path, nextNodeAfterJump);
         }

         if (nextConsecutiveNode != null) {
            nextConsecutiveNode.addToPath(path);
         }
      }
   }

   static final class MultiFork extends Fork
   {
      private static final long serialVersionUID = 1220318686622690670L;
      @Nonnull private final List<Join> caseNodes = new ArrayList<Join>();

      MultiFork(int line) { super(line); }

      @Override
      void addNextNode(@Nonnull Join nextNode) { caseNodes.add(nextNode); }

      @Override
      public void addToPath(@Nonnull Path path)
      {
         path.addNode(this);

         for (Join caseJoin : caseNodes) {
            createAlternatePath(path, caseJoin);
         }
      }
   }

   static final class Join extends Node implements ConditionalSuccessor, GotoSuccessor
   {
      private static final long serialVersionUID = -1983522899831071765L;
      @Nullable ConditionalSuccessor nextNode;
      transient boolean fromTrivialFork;

      Join(int joiningLine) { super(joiningLine); }

      @Override
      public void setNextNodeAfterGoto(@Nonnull Join newJoin) { nextNode = newJoin; }

      @Override
      public void addToPath(@Nonnull Path path)
      {
         path.addNode(this);

         // TODO: can be null when there is a try..finally block; see WhileStatements#whileTrueContainingTryFinally
         if (nextNode != null) {
            nextNode.addToPath(path);
         }
      }

      @Override
      void setSegmentAccordingToPrecedingNode(@Nonnull Node precedingNode)
      {
         segment = precedingNode.segment + 1;
      }
   }

   static final class Goto extends Node implements ConditionalSuccessor, GotoSuccessor
   {
      private static final long serialVersionUID = -4715451134432419220L;
      private Join nextNodeAfterGoto;

      Goto(int line) { super(line); }

      @Override
      public void setNextNodeAfterGoto(@Nonnull Join newJoin) { nextNodeAfterGoto = newJoin; }

      @Override
      public void addToPath(@Nonnull Path path)
      {
         path.addNode(this);
         nextNodeAfterGoto.addToPath(path);
      }
   }
}
