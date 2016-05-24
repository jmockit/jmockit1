/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.parsing;

import java.util.*;
import java.util.regex.*;
import javax.annotation.*;
import static java.util.Arrays.*;

public final class LineElement implements Iterable<LineElement>
{
   private static final List<String> CONDITIONAL_OPERATORS = asList("||", "&&", ":", "else", "?");
   private static final Pattern OPEN_TAG = Pattern.compile("<");
   private static final NoSuchElementException LAST_ELEMENT_REACHED = new NoSuchElementException();

   enum ElementType { CODE, COMMENT, SEPARATOR }

   enum ConditionalStatement
   {
      IF("if"), FOR("for"), WHILE("while");

      @Nonnull private final String keyword;
      ConditionalStatement(@Nonnull String keyword) { this.keyword = keyword; }

      @Nullable
      static ConditionalStatement find(@Nonnull String keyword)
      {
         for (ConditionalStatement statement : values()) {
            if (statement.keyword.equals(keyword)) {
               return statement;
            }
         }

         return null;
      }
   }

   @Nonnull private final ElementType type;
   @Nonnull private final String text;
   @Nullable private String openingTag;
   @Nullable private String closingTag;
   @Nullable private LineElement next;

   @Nullable private ConditionalStatement conditionalStatement;
   private int parenthesesBalance;

   LineElement(@Nonnull ElementType type, @Nonnull String text)
   {
      this.type = type;
      this.text = OPEN_TAG.matcher(text).replaceAll("&lt;");
   }

   public boolean isCode() { return type == ElementType.CODE; }
   public boolean isComment() { return type == ElementType.COMMENT; }

   public boolean isKeyword(@Nonnull String keyword)
   {
      return isCode() && text.equals(keyword);
   }

   public boolean isDotSeparator()
   {
      return type == ElementType.SEPARATOR && text.charAt(0) == '.';
   }

   @Nonnull public String getText() { return text; }

   @Nullable public LineElement getNext() { return next; }
   void setNext(@Nullable LineElement next) { this.next = next; }

   @Nullable
   public LineElement getNextCodeElement()
   {
      if (next != null) {
         for (LineElement element : next) {
            if (element.isCode()) {
               return element;
            }
         }
      }

      return null;
   }

   public void wrapText(@Nonnull String desiredOpeningTag, @Nonnull String desiredClosingTag)
   {
      openingTag = desiredOpeningTag;
      closingTag = desiredClosingTag;
   }

   @Nullable
   public LineElement appendUntilNextCodeElement(@Nonnull StringBuilder line)
   {
      LineElement element = this;

      while (!element.isCode()) {
         element.appendText(line);
         element = element.next;

         if (element == null) {
            break;
         }

         copyConditionalTrackingState(element);
      }

      return element;
   }

   private void copyConditionalTrackingState(@Nonnull LineElement destination)
   {
      destination.conditionalStatement = conditionalStatement;
      destination.parenthesesBalance = parenthesesBalance;
   }

   private void appendText(@Nonnull StringBuilder line)
   {
      if (openingTag == null) {
         line.append(text);
      }
      else {
         line.append(openingTag).append(text).append(closingTag);
      }
   }

   @Nullable
   public LineElement findNextBranchingPoint()
   {
      if (conditionalStatement == null) {
         conditionalStatement = ConditionalStatement.find(text);
      }

      if (isBranchingElement()) {
         if (next != null) {
            copyConditionalTrackingState(next);
         }

         return this;
      }

      if (conditionalStatement != null) {
         int balance = getParenthesisBalance();
         parenthesesBalance += balance;

         if (balance != 0 && parenthesesBalance == 0) {
            return next;
         }
      }

      if (next == null) {
         return null;
      }

      copyConditionalTrackingState(next);

      //noinspection TailRecursion
      return next.findNextBranchingPoint();
   }

   public boolean isBranchingElement()
   {
      if (conditionalStatement == ConditionalStatement.FOR) {
         int p = text.indexOf(':');
         if (p < 0) p = text.indexOf(';');
         return p >= 0 && text.trim().length() == 1;
      }

      return CONDITIONAL_OPERATORS.contains(text);
   }

   private int getParenthesisBalance()
   {
      int balance = 0;
      int p = text.indexOf('(');

      while (p >= 0) {
         balance++;
         p = text.indexOf('(', p + 1);
      }

      int q = text.indexOf(')');

      while (q >= 0) {
         balance--;
         q = text.indexOf(')', q + 1);
      }

      return balance;
   }

   @Nullable
   public LineElement findWord(@Nonnull String word)
   {
      for (LineElement element : this) {
         if (element.isCode() && word.equals(element.text)) {
            return element;
         }
      }

      return null;
   }

   public int getBraceBalanceUntilEndOfLine()
   {
      int balance = 0;

      for (LineElement element : this) {
         balance += element.getBraceBalance();
      }

      return balance;
   }

   private int getBraceBalance()
   {
      if (isCode() && text.length() == 1) {
         char c = text.charAt(0);

         if (c == '{') {
            return 1;
         }
         else if (c == '}') {
            return -1;
         }
      }

      return 0;
   }

   public void appendAllBefore(@Nonnull StringBuilder line, @Nullable LineElement elementToStopBefore)
   {
      LineElement elementToPrint = this;

      do {
         elementToPrint.appendText(line);
         elementToPrint = elementToPrint.next;
      }
      while (elementToPrint != null && elementToPrint != elementToStopBefore);
   }

   @Nonnull @Override
   public Iterator<LineElement> iterator()
   {
      return new Iterator<LineElement>() {
         @Nullable private LineElement current = LineElement.this;

         @Override public boolean hasNext() { return current != null; }

         @Nonnull @Override
         public LineElement next()
         {
            if (current == null) {
               throw LAST_ELEMENT_REACHED;
            }

            LineElement nextElement = current;
            current = current.next;

            return nextElement;
         }

         @Override public void remove() {}
      };
   }

   @Override
   public String toString()
   {
      StringBuilder line = new StringBuilder(200);

      for (LineElement element : this) {
         element.appendText(line);
      }

      return line.toString();
   }
}
