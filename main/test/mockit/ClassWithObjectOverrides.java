/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

public final class ClassWithObjectOverrides implements Cloneable
{
   private final StringBuilder text;

   public ClassWithObjectOverrides(String text) { this.text = new StringBuilder(text); }

   @Override
   public boolean equals(Object o)
   {
      return o instanceof ClassWithObjectOverrides && text.equals(((ClassWithObjectOverrides) o).text);
   }

   @Override
   public int hashCode() { return text.hashCode(); }

   @Override
   public String toString() { return text.toString(); }

   @SuppressWarnings("FinalizeDeclaration")
   @Override
   protected void finalize() throws Throwable
   {
      super.finalize();
      text.setLength(0);
   }

   @Override
   public ClassWithObjectOverrides clone()
   {
      ClassWithObjectOverrides theClone = null;
      try { theClone = (ClassWithObjectOverrides) super.clone(); } catch (CloneNotSupportedException ignore) {}
      return theClone;
   }

   int getIntValue() { return -1; }
   void doSomething() { throw new RuntimeException(); }
   int doSomething(Object arg) { return arg.hashCode(); }
}
