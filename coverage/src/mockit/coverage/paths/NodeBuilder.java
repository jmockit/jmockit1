/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.paths;

import java.util.*;
import javax.annotation.*;

import mockit.coverage.paths.Node.*;
import mockit.external.asm.*;

public final class NodeBuilder
{
   public int firstLine;
   @Nonnull final List<Node> nodes = new ArrayList<Node>();

   @Nullable private Entry entryNode;
   @Nullable private SimpleFork currentSimpleFork;
   @Nullable private BasicBlock currentBasicBlock;
   @Nullable private Join currentJoin;
   @Nonnull private final Map<Label, List<Fork>> jumpTargetToForks = new LinkedHashMap<Label, List<Fork>>();
   @Nonnull private final Map<Label, List<GotoSuccessor>> gotoTargetToSuccessors =
      new LinkedHashMap<Label, List<GotoSuccessor>>();

   private int potentiallyTrivialJump;

   public void handleEntry(int line)
   {
      firstLine = line;
      entryNode = new Entry(line);
      addNewNode(entryNode);
   }

   private int addNewNode(@Nonnull Node newNode)
   {
      int newNodeIndex = nodes.size();

      if (newNodeIndex == 0 && !(newNode instanceof Entry)) {
         return -1;
      }

      nodes.add(newNode);

      if (newNodeIndex > 0) {
         Node precedingNode = nodes.get(newNodeIndex - 1);

         if (precedingNode.line == newNode.line) {
            newNode.setSegmentAccordingToPrecedingNode(precedingNode);
         }
      }

      return newNodeIndex;
   }

   public boolean hasNodes() { return !nodes.isEmpty(); }

   public int handleRegularInstruction(int line, int opcode)
   {
      if (currentSimpleFork == null && currentJoin == null) {
         potentiallyTrivialJump = 0;
         return -1;
      }

      assert currentBasicBlock == null;

      BasicBlock newNode = new BasicBlock(line);
      connectNodes(newNode, opcode);

      return addNewNode(newNode);
   }

   public int handleJump(@Nonnull Label targetBlock, int line, boolean conditional)
   {
      if (conditional) {
         SimpleFork newFork = new SimpleFork(line);
         assert currentSimpleFork == null;
         connectNodes(targetBlock, newFork);
         currentSimpleFork = newFork;
         potentiallyTrivialJump = 1;
         return addNewNode(newFork);
      }
      else if (currentBasicBlock == null && currentJoin == null) {
         Goto newGoto = new Goto(line);
         connectNodes(newGoto);
         setUpMappingFromGotoTargetToCurrentGotoSuccessor(targetBlock, newGoto);
         return addNewNode(newGoto);
      }
      else {
         setUpMappingFromGotoTargetToCurrentGotoSuccessor(targetBlock, null);
         return -1;
      }
   }

   public int handleJumpTarget(@Nonnull Label basicBlock, int line)
   {
      // Ignore for visitLabel calls preceding visitLineNumber:
      if (isNewLineTarget(basicBlock)) {
         return -1;
      }

      Join newNode = new Join(line);
      connectNodes(basicBlock, newNode);

      return addNewNode(newNode);
   }

   private boolean isNewLineTarget(@Nonnull Label basicBlock)
   {
      return !jumpTargetToForks.containsKey(basicBlock) && !gotoTargetToSuccessors.containsKey(basicBlock);
   }

   private void connectNodes(@Nonnull BasicBlock newBasicBlock, int opcode)
   {
      if (currentSimpleFork != null) {
         currentSimpleFork.nextConsecutiveNode = newBasicBlock;
         currentSimpleFork = null;

         if (potentiallyTrivialJump == 1) {
            potentiallyTrivialJump = opcode == Opcodes.ICONST_1 ? 2 : 0;
         }
      }
      else {
         assert currentJoin != null;

         if (potentiallyTrivialJump == 3) {
            if (opcode == Opcodes.ICONST_0) {
               currentJoin.fromTrivialFork = true;
            }

            potentiallyTrivialJump = 0;
         }

         currentJoin.nextNode = newBasicBlock;
         currentJoin = null;
      }

      currentBasicBlock = newBasicBlock;
   }

   private void connectNodes(@Nonnull Label targetBlock, @Nonnull Fork newFork)
   {
      assert entryNode != null;

      if (entryNode.nextNode == null) {
         entryNode.nextNode = newFork;
      }

      setUpMappingFromConditionalTargetToFork(targetBlock, newFork);
      connectNodes(newFork);
   }

   private void setUpMappingFromConditionalTargetToFork(@Nonnull Label targetBlock, @Nonnull Fork newFork)
   {
      List<Fork> forksWithSameTarget = jumpTargetToForks.get(targetBlock);

      if (forksWithSameTarget == null) {
         forksWithSameTarget = new LinkedList<Fork>();
         jumpTargetToForks.put(targetBlock, forksWithSameTarget);
      }

      forksWithSameTarget.add(newFork);
   }

   private void setUpMappingFromGotoTargetToCurrentGotoSuccessor(@Nonnull Label targetBlock, @Nullable Goto gotoNode)
   {
      List<GotoSuccessor> successors = gotoTargetToSuccessors.get(targetBlock);

      if (successors == null) {
         successors = new LinkedList<GotoSuccessor>();
         gotoTargetToSuccessors.put(targetBlock, successors);
      }

      // TODO: they both can be non-null here; what to do?
      if (currentBasicBlock != null) {
         assert currentJoin == null : "Ambiguous situation for " + targetBlock;
         successors.add(currentBasicBlock);
         currentBasicBlock = null;

         if (potentiallyTrivialJump == 2) {
            potentiallyTrivialJump = 3;
         }
      }
      else if (currentJoin != null) {
         successors.add(currentJoin);
         currentJoin = null;
      }
      else {
         successors.add(gotoNode);
      }
   }

   private void connectNodes(@Nonnull Label basicBlock, @Nonnull Join newJoin)
   {
      connectNodes(newJoin);
      connectSourceForksToTargetedJoin(basicBlock, newJoin);
      connectGotoSuccessorsToNewJoin(basicBlock, newJoin);
      currentJoin = newJoin;
   }

   public int handleExit(int exitLine)
   {
      Exit newNode = new Exit(exitLine);
      connectNodes(newNode);

      return addNewNode(newNode);
   }

   private void connectNodes(@Nonnull ConditionalSuccessor newNode)
   {
      if (currentSimpleFork != null) {
         currentSimpleFork.nextConsecutiveNode = newNode;
         currentSimpleFork = null;
         assert currentJoin == null;
         assert currentBasicBlock == null;
      }

      if (currentJoin != null) {
         currentJoin.nextNode = newNode;
         currentJoin = null;
         assert currentBasicBlock == null;
      }

      if (currentBasicBlock != null) {
         currentBasicBlock.nextConsecutiveNode = newNode;
         currentBasicBlock = null;
      }
   }

   private void connectSourceForksToTargetedJoin(@Nonnull Label targetBlock, @Nonnull Join newJoin)
   {
      List<Fork> forks = jumpTargetToForks.get(targetBlock);

      if (forks != null) {
         for (Fork fork : forks) {
            fork.addNextNode(newJoin);
         }

         jumpTargetToForks.remove(targetBlock);
      }
   }

   private void connectGotoSuccessorsToNewJoin(@Nonnull Label targetBlock, @Nonnull Join newJoin)
   {
      List<GotoSuccessor> successors = gotoTargetToSuccessors.get(targetBlock);

      if (successors != null) {
         for (GotoSuccessor successorToGoto : successors) {
            successorToGoto.setNextNodeAfterGoto(newJoin);
         }

         gotoTargetToSuccessors.remove(targetBlock);
      }
   }

   public int handleForwardJumpsToNewTargets(@Nonnull Label defaultBlock, @Nonnull Label[] caseBlocks, int line)
   {
      Fork newJoin = new MultiFork(line);

      for (Label targetBlock : caseBlocks) {
         if (targetBlock != defaultBlock) {
            connectNodes(targetBlock, newJoin);
         }
      }

      connectNodes(defaultBlock, newJoin);

      return addNewNode(newJoin);
   }
}
