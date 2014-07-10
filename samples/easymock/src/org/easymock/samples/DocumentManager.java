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

public final class DocumentManager
{
   private final Set<Collaborator> listeners = new HashSet<Collaborator>();
   private final Map<String, byte[]> documents = new HashMap<String, byte[]>();

   public void addListener(Collaborator listener)
   {
      listeners.add(listener);
   }

   public void addDocument(String title, byte[] document)
   {
      boolean documentChange = documents.containsKey(title);
      documents.put(title, document);

      if (documentChange) {
         notifyListenersDocumentChanged(title);
      }
      else {
         notifyListenersDocumentAdded(title);
      }
   }

   private void notifyListenersDocumentChanged(String title)
   {
      for (Collaborator listener : listeners) {
         listener.documentChanged(title);
      }
   }

   private void notifyListenersDocumentAdded(String title)
   {
      for (Collaborator listener : listeners) {
         listener.documentAdded(title);
      }
   }

   public boolean removeDocument(String title)
   {
      if (!documents.containsKey(title)) {
         return true;
      }

      if (!listenersAllowRemoval(title)) {
         return false;
      }

      documents.remove(title);
      notifyListenersDocumentRemoved(title);

      return true;
   }

   private boolean listenersAllowRemoval(String title)
   {
      int votes = 0;

      for (Collaborator listener : listeners) {
         votes += listener.voteForRemoval(title);
      }

      return votes > 0;
   }

   private void notifyListenersDocumentRemoved(String title)
   {
      for (Collaborator listener : listeners) {
         listener.documentRemoved(title);
      }
   }

   public boolean removeDocuments(String... titles)
   {
      if (!listenersAllowRemovals(titles)) {
         return false;
      }

      for (String title : titles) {
         documents.remove(title);
         notifyListenersDocumentRemoved(title);
      }

      return true;
   }

   private boolean listenersAllowRemovals(String... titles)
   {
      int votes = 0;

      for (Collaborator listener : listeners) {
         votes += listener.voteForRemovals(titles);
      }

      return votes > 0;
   }
}
