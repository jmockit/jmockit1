/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package orderMngr.domain.order;

public final class InvalidOrderItem extends Exception
{
   public InvalidOrderItem(String message)
   {
      super(message);
   }
}
