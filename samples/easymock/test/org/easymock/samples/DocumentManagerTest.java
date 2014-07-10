/*
 * Copyright 2001-2009 OFFIS, Tammo Freese
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.easymock.samples;

import java.util.*;

import static org.easymock.EasyMock.*;
import org.easymock.*;

import static org.junit.Assert.*;
import org.junit.*;

public final class DocumentManagerTest
{
   private DocumentManager classUnderTest;
   private Collaborator mock;

   @Before
   public void setup()
   {
      mock = createMock(Collaborator.class);
      classUnderTest = new DocumentManager();
      classUnderTest.addListener(mock);
   }

   @Test
   public void removeNonExistingDocument()
   {
      // Expect no uses of the collaborator.
      replay(mock);

      assertTrue(classUnderTest.removeDocument("Does not exist"));
   }

   @Test
   public void addDocument()
   {
      mock.documentAdded("New Document");
      replay(mock);

      classUnderTest.addDocument("New Document", new byte[0]);
      verify(mock);
   }

   @Test
   public void addAndChangeDocument()
   {
      mock.documentAdded("Document");
      mock.documentChanged("Document"); expectLastCall().times(3);
      replay(mock);

      classUnderTest.addDocument("Document", new byte[0]);
      classUnderTest.addDocument("Document", new byte[0]);
      classUnderTest.addDocument("Document", new byte[0]);
      classUnderTest.addDocument("Document", new byte[0]);
      verify(mock);
   }

   @Test
   public void voteForRemoval()
   {
      // Expect document addition.
      mock.documentAdded("Document");
      // Expect to be asked to vote, and vote for it.
      expect(mock.voteForRemoval("Document")).andReturn(42);
      // Expect document removal.
      mock.documentRemoved("Document");
      replay(mock);

      classUnderTest.addDocument("Document", new byte[0]);
      assertTrue(classUnderTest.removeDocument("Document"));
      verify(mock);
   }

   @Test
   public void voteAgainstRemoval()
   {
      // Expect document addition.
      mock.documentAdded("Document");
      // Expect to be asked to vote, and vote against it.
      expect(mock.voteForRemoval("Document")).andReturn(-42);
      // Document removal is *not* expected.
      replay(mock);

      classUnderTest.addDocument("Document", new byte[0]);
      assertFalse(classUnderTest.removeDocument("Document"));
      verify(mock);
   }

   @Test
   public void voteForRemovals()
   {
      mock.documentAdded("Document 1");
      mock.documentAdded("Document 2");
      String[] documents = {"Document 1", "Document 2"};
      expect(mock.voteForRemovals(documents)).andReturn(42);
      mock.documentRemoved("Document 1");
      mock.documentRemoved("Document 2");
      replay(mock);

      classUnderTest.addDocument("Document 1", new byte[0]);
      classUnderTest.addDocument("Document 2", new byte[0]);
      assertTrue(classUnderTest.removeDocuments("Document 1", "Document 2"));
      verify(mock);
   }

   @Test
   public void voteAgainstRemovals()
   {
      mock.documentAdded("Document 1");
      mock.documentAdded("Document 2");
      expect(mock.voteForRemovals("Document 1", "Document 2")).andReturn(-42);
      replay(mock);

      classUnderTest.addDocument("Document 1", new byte[0]);
      classUnderTest.addDocument("Document 2", new byte[0]);
      assertFalse(classUnderTest.removeDocuments("Document 1", "Document 2"));
      verify(mock);
   }

   @Test
   public void answerVsDelegate()
   {
      @SuppressWarnings("unchecked")
      List<String> l = createMock(List.class);

      // andAnswer style
      expect(l.remove(10)).andAnswer(new IAnswer<String>() {
         @Override
         public String answer() throws Throwable
         {
            return getCurrentArguments()[0].toString();
         }
      });

      // andDelegateTo style
      expect(l.remove(10)).andDelegateTo(new ArrayList<String>() {
         @Override
         public String remove(int index) { return String.valueOf(index); }
      });

      replay(l);

      assertEquals("10", l.remove(10));
      assertEquals("10", l.remove(10));

      verify(l);
   }
}
