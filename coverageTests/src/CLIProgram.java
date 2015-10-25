/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */

public final class CLIProgram
{
   private CLIProgram() {}

   public static void main(String[] args) throws Exception
   {
      System.out.println("Command Line Interface program");

      do {
         System.out.println("Press Enter");
      }
      while (System.in.read() != '\n');
   }
}
