/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.parsing;

import javax.annotation.*;

import mockit.coverage.reporting.parsing.LineElement.*;

/**
 * Parses a source line into one or more consecutive segments, identifying which ones contain Java code and which ones
 * contain only comments.
 * Block comments initiated in a previous line are kept track of until the end of the block is reached.
 */
public final class LineParser
{
   private static final String SEPARATORS = ".,;()";

   private int lineNum;
   @Nonnull private String line;
   @Nullable private LineElement initialElement;
   private boolean inComments;

   // Helper fields:
   @Nullable private LineElement currentElement;
   private int lineLength;
   private int startPos;
   private boolean inCodeElement;
   private int pos;
   private int currChar;

   public int getNumber() { return lineNum; }

   public boolean isInComments() { return inComments; }

   public boolean isBlankLine()
   {
      int n = line.length();

      for (int i = 0; i < n; i++) {
         char c = line.charAt(i);

         if (!Character.isWhitespace(c)) {
            return false;
         }
      }

      return true;
   }

   @Nonnull public LineElement getInitialElement() { assert initialElement != null; return initialElement; }

   boolean parse(@Nonnull String lineToParse)
   {
      lineNum++;
      initialElement = null;
      currentElement = null;
      line = lineToParse;
      lineLength = lineToParse.length();
      startPos = inComments ? 0 : -1;
      inCodeElement = false;

      for (pos = 0; pos < lineLength; pos++) {
         currChar = lineToParse.codePointAt(pos);

         if (parseComment()) {
            break;
         }

         parseSeparatorsAndCode();
      }

      if (startPos >= 0) {
         addFinalElement();
      }
      else if (initialElement == null) {
         initialElement = new LineElement(ElementType.SEPARATOR, "");
         return false;
      }

      return !inComments && !isBlankLine();
   }

   private void parseSeparatorsAndCode()
   {
      boolean separator = isSeparator();

      if (separator && !inCodeElement) {
         startNewElementIfNotYetStarted();
      }
      else if (!separator && !inCodeElement) {
         if (startPos >= 0) {
            addElement();
         }

         inCodeElement = true;
         startPos = pos;
      }
      else if (separator) {
         addElement();
         inCodeElement = false;
         startPos = pos;
      }
   }

   private boolean isSeparator()
   {
      return Character.isWhitespace(currChar) || SEPARATORS.indexOf(currChar) >= 0;
   }

   private void startNewElementIfNotYetStarted()
   {
      if (startPos < 0) {
         startPos = pos;
      }
   }

   private boolean parseComment()
   {
      if (inComments && parseUntilEndOfLineOrEndOfComment()) {
         return true;
      }

      while (currChar == '/' && pos < lineLength - 1) {
         int c2 = line.codePointAt(pos + 1);

         if (c2 == '/') {
            endCodeElementIfPending();
            startNewElementIfNotYetStarted();
            inComments = true;
            addFinalElement();
            inComments = false;
            startPos = -1;
            return true;
         }
         else if (c2 == '*') {
            endCodeElementIfPending();
            startNewElementIfNotYetStarted();
            inComments = true;
            pos += 2;

            if (parseUntilEndOfLineOrEndOfComment()) {
               return true;
            }
         }
         else {
            break;
         }
      }

      return false;
   }

   private void endCodeElementIfPending()
   {
      if (inCodeElement) {
         addElement();
         startPos = pos;
         inCodeElement = false;
      }
   }

   private boolean parseUntilEndOfLineOrEndOfComment()
   {
      while (pos < lineLength) {
         currChar = line.codePointAt(pos);

         if (currChar == '*' && pos < lineLength - 1 && line.codePointAt(pos + 1) == '/') {
            pos += 2;
            addElement();
            startPos = -1;
            inComments = false;
            break;
         }

         pos++;
      }

      if (pos < lineLength) {
         currChar = line.codePointAt(pos);
         return false;
      }
      else {
         return true;
      }
   }

   private void addFinalElement()
   {
      String text = line.substring(startPos);
      addElement(text);
   }

   private void addElement()
   {
      String text = pos > 0 ? line.substring(startPos, pos) : line.substring(startPos);
      addElement(text);
   }

   private void addElement(@Nonnull String text)
   {
      ElementType type;

      if (inComments) {
         type = ElementType.COMMENT;
      }
      else if (inCodeElement) {
         type = ElementType.CODE;
      }
      else {
         type = ElementType.SEPARATOR;
      }

      LineElement newElement = new LineElement(type, text);

      if (initialElement == null) {
         initialElement = newElement;
      }
      else {
         assert currentElement != null;
         currentElement.setNext(newElement);
      }

      currentElement = newElement;
   }
}
