/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockitousage.examples.use;

import java.util.List;

// Comparison note: with Mockito, classes like this cannot be final.
public /*final*/ class ArticleDatabase
{
   // Comparison note: with Mockito, methods like this cannot be final.
   public /*final*/ void updateNumberOfArticles(String newspaper, int articles)
   {
   }

   public void updateNumberOfPolishArticles(String newspaper, int polishArticles)
   {
   }

   public void updateNumberOfEnglishArticles(String newspaper, int i)
   {
   }

   public List<Article> getArticlesFor(String string)
   {
      return null;
   }

   // Comparison note: with Mockito, methods like this cannot be static.
   public /*static*/ void save(Article article)
   {
   }
}
