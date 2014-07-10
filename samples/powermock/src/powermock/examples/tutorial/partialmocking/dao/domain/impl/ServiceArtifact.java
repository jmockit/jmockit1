/*
 * Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package powermock.examples.tutorial.partialmocking.dao.domain.impl;

import powermock.examples.tutorial.partialmocking.dao.domain.Connection;
import powermock.examples.tutorial.partialmocking.domain.DataProducer;

public class ServiceArtifact
{
   private final int id;
   private final String name;
   private final DataProducer[] dataProducers;

   public ServiceArtifact(int id, String name, DataProducer... dataProducers)
   {
      this.id = id;
      this.name = name;
      this.dataProducers = dataProducers;
   }

   public DataProducer[] getDataProducers()
   {
      return dataProducers;
   }

   public int getId()
   {
      return id;
   }

   public String getName()
   {
      return name;
   }

   public Connection connectToService()
   {
      return new ConnectionImpl();
   }
}
