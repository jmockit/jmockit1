package org.jmock.samples.fruitPicker;

import org.jmock.*;
import org.jmock.lib.action.*;
import org.jmock.api.*;
import org.jmock.integration.junit4.*;
import static org.junit.Assert.*;
import org.junit.*;

import static java.util.Arrays.*;
import java.util.*;

/**
 * See the <a href="http://www.jmock.org/custom-actions.html">official jMock documentation page</a>
 * for an explanation of this test.
 */
public final class FruitPickerTest
{
   @Rule public final JUnitRuleMockery context = new JUnitRuleMockery();

   @Test
   @SuppressWarnings("unchecked")
   public void pickFruits()
   {
      final FruitTree mangoTree = context.mock(FruitTree.class);
      final Mango mango1 = new Mango();
      final Mango mango2 = new Mango();

      context.checking(new Expectations() {{
         oneOf(mangoTree).pickFruit(with(any(Collection.class)));
         will(new VoidAction() {
            @Override
            public Object invoke(Invocation invocation)
            {
               Collection<Fruit> fruits = (Collection<Fruit>) invocation.getParameter(0);
               fruits.add(mango1);
               fruits.add(mango2);
               return null;
            }
         });
      }});

      Collection<Fruit> fruits = new FruitPicker().pickFruits(asList(mangoTree));

      assertEquals(asList(mango1, mango2), fruits);
   }
}
