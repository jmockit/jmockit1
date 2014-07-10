package org.mockitousage.examples;

public class Item
{
   private long id;
   private String name;

   public Item(long id, String name)
   {
      this.id = id;
      this.name = name;
   }

   public long getId()
   {
      return id;
   }

   public void setId(long id)
   {
      this.id = id;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null || !(obj instanceof Item)) {
         return false;
      }

      return getId() == ((Item) obj).getId();
   }

   @Override
   public String toString()
   {
      return getName();
   }
}
