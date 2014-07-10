/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.paths;

import java.util.*;

import org.jetbrains.annotations.*;

import mockit.coverage.paths.Node.*;
import mockit.external.asm4.*;

public final class NodeBuilder
{
   public int firstLine;
   @NotNull final List<Node> nodes = new ArrayList<Node>();

   @Nullable private Node.Entry entryNode;
   @Nullable private Node.SimpleFork currentSimpleFork;
   @Nullable private Node.BasicBlock currentBasicBlock;
   @Nullable private Node.Join currentJoin;
   @NotNull private final Map<Label, List<Node.Fork>> jumpTargetToForks = new LinkedHashMap<Label, List<Node.Fork>>();
   @NotNull private final Map<Label, List<Node.GotoSuccessor>> gotoTargetToSuccessors =
      new LinkedHashMap<Label, List<Node.GotoSuccessor>>();

   private int potentiallyTrivialJump;

   public void handleEntry(int line)
   {
      firstLine = line;
      entryNode = new Node.Entry(line);
      addNewNode(entryNode);
   }

   private int addNewNode(@NotNull Node newNode)
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

      Node.BasicBlock newNode = new Node.BasicBlock(line);
      connectNodes(newNode, opcode);

      return addNewNode(newNode);
   }

   public int handleJump(@NotNull Label targetBlock, int line, boolean conditional)
   {
      if (conditional) {
         Node.SimpleFork newFork = new Node.SimpleFork(line);
         assert currentSimpleFork == null;
         connectNodes(targetBlock, newFork);
         currentSimpleFork = newFork;
         potentiallyTrivialJump = 1;
         return addNewNode(newFork);
      }
      else if (currentBasicBlock == null && currentJoin == null) {
         Node.Goto newGoto = new Node.Goto(line);
         connectNodes(newGoto);
         setUpMappingFromGotoTargetToCurrentGotoSuccessor(targetBlock, newGoto);
         return addNewNode(newGoto);
      }
      else {
         setUpMappingFromGotoTargetToCurrentGotoSuccessor(targetBlock, null);
         return -1;
      }
   }

   public int handleJumpTarget(@NotNull Label basicBlock, int line)
   {
      // Ignore for visitLabel calls preceding visitLineNumber:
      if (isNewLineTarget(basicBlock)) {
         return -1;
      }

      Node.Join newNode = new Node.Join(line);
      connectNodes(basicBlock, newNode);

      return addNewNode(newNode);
   }

   private boolean isNewLineTarget(@NotNull Label basicBlock)
   {
      return !jumpTargetToForks.containsKey(basicBlock) && !gotoTargetToSuccessors.containsKey(basicBlock);
   }

   private void connectNodes(@NotNull Node.BasicBlock newBasicBlock, int opcode)
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

   private void connectNodes(@NotNull Label targetBlock, @NotNull Node.Fork newFork)
   {
      assert entryNode != null;

      if (entryNode.nextNode == null) {
         entryNode.nextNode = newFork;
      }

      setUpMappingFromConditionalTargetToFork(targetBlock, newFork);
      connectNodes(newFork);
   }

   private void setUpMappingFromConditionalTargetToFork(@NotNull Label targetBlock, @NotNull Node.Fork newFork)
   {
      List<Node.Fork> forksWithSameTarget = jumpTargetToForks.get(targetBlock);

      if (forksWithSameTarget == null) {
         forksWithSameTarget = new LinkedList<Node.Fork>();
         jumpTargetToForks.put(targetBlock, forksWithSameTarget);
      }

      forksWithSameTarget.add(newFork);
   }

   private void setUpMappingFromGotoTargetToCurrentGotoSuccessor(
      @NotNull Label targetBlock, @Nullable Node.Goto gotoNode)
   {
      List<Node.GotoSuccessor> successors = gotoTargetToSuccessors.get(targetBlock);

      if (successors == null) {
         successors = new LinkedList<Node.GotoSuccessor>();
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

   private void connectNodes(@NotNull Label basicBlock, @NotNull Node.Join newJoin)
   {
      connectNodes(newJoin);
      connectSourceForksToTargetedJoin(basicBlock, newJoin);
      connectGotoSuccessorsToNewJoin(basicBlock, newJoin);
      currentJoin = newJoin;
   }

   public int handleExit(int exitLine)
   {
      Node.Exit newNode = new Node.Exit(exitLine);
      connectNodes(newNode);

      return addNewNode(newNode);
   }

   private void connectNodes(@NotNull Node.ConditionalSuccessor newNode)
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

   private void connectSourceForksToTargetedJoin(@NotNull Label targetBlock, @NotNull Node.Join newJoin)
   {
      List<Node.Fork> forks = jumpTargetToForks.get(targetBlock);

      if (forks != null) {
         for (Node.Fork fork : forks) {
            fork.addNextNode(newJoin);
         }

         jumpTargetToForks.remove(targetBlock);
      }
   }

   private void connectGotoSuccessorsToNewJoin(@NotNull Label targetBlock, @NotNull Node.Join newJoin)
   {
      List<Node.GotoSuccessor> successors = gotoTargetToSuccessors.get(targetBlock);

      if (successors != null) {
         for (Node.GotoSuccessor successorToGoto : successors) {
            successorToGoto.setNextNodeAfterGoto(newJoin);
         }

         gotoTargetToSuccessors.remove(targetBlock);
      }
   }

   public int handleForwardJumpsToNewTargets(@NotNull Label defaultBlock, @NotNull Label[] caseBlocks, int line)
   {
      Node.Fork newJoin = new Node.MultiFork(line);

      for (Label targetBlock : caseBlocks) {
         if (targetBlock != defaultBlock) {
            connectNodes(targetBlock, newJoin);
         }
      }

      connectNodes(defaultBlock, newJoin);

      return addNewNode(newJoin);
   }
}
