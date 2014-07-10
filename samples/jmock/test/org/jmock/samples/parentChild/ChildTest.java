package org.jmock.samples.parentChild;

import org.jmock.*;
import org.jmock.auto.*;
import org.jmock.integration.junit4.*;
import org.junit.*;

/**
 * See the <a href="http://www.jmock.org/override.html">official jMock documentation page</a> for an
 * explanation of this test.
 */
public final class ChildTest
{
   @Rule public final JUnitRuleMockery mockery = new JUnitRuleMockery();
   States test = mockery.states("test");

   @Mock Parent parent;

   Child child;

   @Before
   public void createChildOfParent()
   {
      mockery.checking(new Expectations() {{
         ignoring(parent).addChild(with(aNonNull(Child.class))); when(test.isNot("fully-set-up"));
      }});

      // Creating the child adds it to the parent.
      child = new Child(parent);

      test.become("fully-set-up");
   }

   @Test
   public void removesItselfFromOldParentWhenAssignedNewParent()
   {
      final Parent newParent = mockery.mock(Parent.class, "newParent");

      mockery.checking(new Expectations() {{
         oneOf(parent).removeChild(child);
         oneOf(newParent).addChild(child);
      }});

      child.reparent(newParent);
   }
}
