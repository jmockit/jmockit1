package org.jmock.samples.parentChild;

public class Child
{
   private final Parent parent;

   public Child(Parent parent)
   {
      this.parent = parent;
      parent.addChild(this);
   }

   public void reparent(Parent newParent)
   {
      parent.removeChild(this);
      newParent.addChild(this);
   }
}
