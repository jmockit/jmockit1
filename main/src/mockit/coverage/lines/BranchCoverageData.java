/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.lines;

import java.io.*;
import javax.annotation.*;

import mockit.external.asm.*;

/**
 * Coverage data gathered for a branch inside a line of source code.
 */
public final class BranchCoverageData extends LineSegmentData
{
   private static final long serialVersionUID = 1003335601845442606L;
   static final BranchCoverageData INVALID = new BranchCoverageData(new Label());

   @Nonnull private transient Label label;

   BranchCoverageData(@Nonnull Label label) { this.label = label; }

   int getLine() { return label.info == null ? label.line : (Integer) label.info; }

   private void readObject(@Nonnull ObjectInputStream in) throws IOException, ClassNotFoundException
   {
      label = new Label();
      label.line = in.readInt();
      in.defaultReadObject();
   }

   private void writeObject(@Nonnull ObjectOutputStream out) throws IOException
   {
      int line = getLine();
      out.writeInt(line);
      out.defaultWriteObject();
   }
}
