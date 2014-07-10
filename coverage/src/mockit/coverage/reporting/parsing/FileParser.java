/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.parsing;

import java.util.*;

import org.jetbrains.annotations.*;

public final class FileParser
{
   private static final class PendingClass
   {
      @NotNull final String className;
      int braceBalance;
      PendingClass(@NotNull String className) { this.className = className; }
   }

   @NotNull public final LineParser lineParser = new LineParser();
   @NotNull public final List<PendingClass> currentClasses = new ArrayList<PendingClass>(2);

   @Nullable private PendingClass currentClass;
   private boolean openingBraceForClassFound;
   private int currentBraceBalance;

   public boolean parseCurrentLine(@NotNull String line)
   {
      if (!lineParser.parse(line)) {
         return false;
      }

      LineElement firstElement = lineParser.getInitialElement();
      LineElement classDeclaration = findClassNameInNewClassDeclaration();

      if (classDeclaration != null) {
         firstElement = classDeclaration;
         registerStartOfClassDeclaration(classDeclaration);
      }

      if (currentClass != null) {
         detectPotentialEndOfClassDeclaration(firstElement);
      }

      return true;
   }

   @Nullable private LineElement findClassNameInNewClassDeclaration()
   {
      LineElement previous = null;

      for (LineElement element : lineParser.getInitialElement()) {
         if (element.isKeyword("class") && (previous == null || !previous.isDotSeparator())) {
            return element.getNextCodeElement();
         }

         previous = element;
      }

      return null;
   }

   private void registerStartOfClassDeclaration(@NotNull LineElement elementWithClassName)
   {
      String className = elementWithClassName.getText();

      if (currentClass != null) {
         currentClass.braceBalance = currentBraceBalance;
      }

      currentClass = new PendingClass(className);
      currentClasses.add(currentClass);
      currentBraceBalance = 0;
   }

   private void detectPotentialEndOfClassDeclaration(@NotNull LineElement firstElement)
   {
      // TODO: how to deal with classes defined entirely in one line?
      currentBraceBalance += firstElement.getBraceBalanceUntilEndOfLine();

      if (!openingBraceForClassFound && currentBraceBalance > 0) {
         openingBraceForClassFound = true;
      }
      else if (openingBraceForClassFound && currentBraceBalance == 0) {
         restorePreviousPendingClassIfAny();
      }
   }

   private void restorePreviousPendingClassIfAny()
   {
      currentClasses.remove(currentClass);

      if (currentClasses.isEmpty()) {
         currentClass = null;
      }
      else {
         currentClass = currentClasses.get(currentClasses.size() - 1);
         currentBraceBalance = currentClass.braceBalance;
      }
   }

   @Nullable public String getCurrentlyPendingClass()
   {
      return currentClass == null ? null : currentClass.className;
   }
}
