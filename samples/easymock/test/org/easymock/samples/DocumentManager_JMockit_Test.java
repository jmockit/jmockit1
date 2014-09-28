/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.easymock.samples;

import java.util.*;

import static org.junit.Assert.*;
import org.junit.*;

import mockit.*;

public final class DocumentManager_JMockit_Test
{
   DocumentManager classUnderTest;
   @Mocked Collaborator mock; // A mock field which will be automatically set.

   @Before
   public void setup()
   {
      classUnderTest = new DocumentManager();
      classUnderTest.addListener(mock);
   }

   @Test
   public void removeNonExistingDocument()
   {
      assertTrue(classUnderTest.removeDocument("Does not exist"));

      // Verify there were no uses of the collaborator.
      new FullVerifications() {};
   }

   @Test
   public void addDocument()
   {
      classUnderTest.addDocument("New Document", new byte[0]);

      new Verifications() {{ mock.documentAdded("New Document"); }};
   }

   @Test
   public void addAndChangeDocument()
   {
      classUnderTest.addDocument("Document", new byte[0]);
      classUnderTest.addDocument("Document", new byte[0]);
      classUnderTest.addDocument("Document", new byte[0]);
      classUnderTest.addDocument("Document", new byte[0]);

      new Verifications() {{
         mock.documentAdded("Document");
         mock.documentChanged("Document"); times = 3;
      }};
   }

   @Test
   public void voteForRemoval()
   {
      new Expectations() {{
         // Expect to be asked to vote, and vote for it.
         mock.voteForRemoval("Document"); result = 42; times = 1;
      }};

      classUnderTest.addDocument("Document", new byte[0]);
      assertTrue(classUnderTest.removeDocument("Document"));

      new Verifications() {{
         // Verify document addition.
         mock.documentAdded("Document");
         // Verify document removal.
         mock.documentRemoved("Document");
      }};
   }

   @Test
   public void voteAgainstRemoval()
   {
      new Expectations() {{
         // Expect to be asked to vote, and vote against it.
         mock.voteForRemoval("Document"); result = -42; times = 1;
      }};

      classUnderTest.addDocument("Document", new byte[0]);
      assertFalse(classUnderTest.removeDocument("Document"));

      new Verifications() {{
         // Verify document addition.
         mock.documentAdded("Document");
         // Document removal is *not* expected.
      }};
   }

   @Test
   public void voteForRemovals()
   {
      new Expectations() {{
         mock.voteForRemovals("Document 1", "Document 2"); result = 42; times = 1;
      }};

      classUnderTest.addDocument("Document 1", new byte[0]);
      classUnderTest.addDocument("Document 2", new byte[0]);
      assertTrue(classUnderTest.removeDocuments("Document 1", "Document 2"));

      new Verifications() {{
         mock.documentAdded("Document 1");
         mock.documentAdded("Document 2");
         mock.documentRemoved("Document 1");
         mock.documentRemoved("Document 2");
      }};
   }

   @Test
   public void voteAgainstRemovals()
   {
      new Expectations() {{
         mock.voteForRemovals("Document 1", "Document 2"); result = -42; times = 1;
      }};

      classUnderTest.addDocument("Document 1", new byte[0]);
      classUnderTest.addDocument("Document 2", new byte[0]);
      assertFalse(classUnderTest.removeDocuments("Document 1", "Document 2"));

      new Verifications() {{
         mock.documentAdded("Document 1");
         mock.documentAdded("Document 2");
      }};
   }

   @Test
   public void delegateMethodWhichProducesResultBasedOnCustomLogic(@Mocked final List<String> l)
   {
      new Expectations() {{
         l.remove(10);
         result = new Delegate() {
            @SuppressWarnings("unused")
            String remove(int index) { return String.valueOf(index); }
         };
      }};

      assertEquals("10", l.remove(10));
   }
}
