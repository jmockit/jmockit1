package org.jmock.samples.fruitPicker;

import java.util.*;

public final class FruitPicker
{
   public Collection<Fruit> pickFruits(Collection<FruitTree> fruitTrees)
   {
      Collection<Fruit> fruits = new ArrayList<Fruit>();

      for (FruitTree fruitTree : fruitTrees) {
         fruitTree.pickFruit(fruits);
      }

      return fruits;
   }
}
