/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jmock.samples.fruitPicker;

import java.util.*;

import org.junit.*;

import mockit.*;

import static java.util.Arrays.*;
import static org.junit.Assert.*;

public final class FruitPicker_JMockit_Test
{
   @Test
   public void pickFruits(@Mocked final FruitTree mangoTree)
   {
      final Mango mango1 = new Mango();
      final Mango mango2 = new Mango();

      new Expectations() {{
         mangoTree.pickFruit((Collection<Fruit>) any);
         result = new Delegate() {
            void pickFruit(Collection<Fruit> fruits)
            {
               fruits.add(mango1);
               fruits.add(mango2);
            }
         };
      }};

      Collection<Fruit> fruits = new FruitPicker().pickFruits(asList(mangoTree));

      assertEquals(asList(mango1, mango2), fruits);
   }
}
