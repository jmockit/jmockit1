/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.mockitousage.examples.use;

import org.junit.*;

import mockit.*;

import static java.util.Arrays.*;

// Total uses of JMockit API: 24
public final class ArticleManager_JMockit_Test
{
   @Injectable ArticleCalculator mockCalculator;
   @Injectable ArticleDatabase mockDatabase;
   @Tested ArticleManager articleManager;

   @Test
   public void managerCountsArticlesAndSavesThemInTheDatabase()
   {
      new Expectations() {{
         mockCalculator.countArticles("Guardian"); result = 12;
         mockCalculator.countArticlesInPolish(anyString); result = 5;
      }};

      articleManager.updateArticleCounters("Guardian");

      new Verifications() {{
         mockDatabase.updateNumberOfArticles("Guardian", 12);
         mockDatabase.updateNumberOfPolishArticles("Guardian", 5);
         mockDatabase.updateNumberOfEnglishArticles("Guardian", 7);
      }};
   }

   @Test
   public void managerCountsArticlesUsingCalculator()
   {
      articleManager.updateArticleCounters("Guardian");

      new Verifications() {{
         mockCalculator.countArticles("Guardian");
         mockCalculator.countArticlesInPolish("Guardian");
      }};
   }

   @Test
   public void managerSavesArticlesInTheDatabase()
   {
      articleManager.updateArticleCounters("Guardian");

      new Verifications() {{
         mockDatabase.updateNumberOfArticles("Guardian", 0);
         mockDatabase.updateNumberOfPolishArticles("Guardian", 0);
         mockDatabase.updateNumberOfEnglishArticles("Guardian", 0);
      }};
   }

   @Test
   public void managerUpdatesNumberOfRelatedArticles()
   {
      final Article articleOne = new Article();
      final Article articleTwo = new Article();
      final Article articleThree = new Article();

      new Expectations() {{
         mockCalculator.countNumberOfRelatedArticles(articleOne); result = 1;
         mockCalculator.countNumberOfRelatedArticles(articleTwo); result = 12;
         mockCalculator.countNumberOfRelatedArticles(articleThree); result = 0;
         mockDatabase.getArticlesFor("Guardian"); returns(articleOne, articleTwo, articleThree);
      }};

      articleManager.updateRelatedArticlesCounters("Guardian");

      new Verifications() {{
         mockDatabase.save(articleOne);
         mockDatabase.save(articleTwo);
         mockDatabase.save(articleThree);
      }};
   }

   @Test
   public void shouldPersistRecalculatedArticle()
   {
      final Article articleOne = new Article();
      final Article articleTwo = new Article();

      new Expectations() {{
         mockCalculator.countNumberOfRelatedArticles(articleOne); result = 1;
         mockCalculator.countNumberOfRelatedArticles(articleTwo); result = 12;
         mockDatabase.getArticlesFor("Guardian"); result = asList(articleOne, articleTwo);
      }};

      articleManager.updateRelatedArticlesCounters("Guardian");

      new VerificationsInOrder() {{
         mockCalculator.countNumberOfRelatedArticles(withInstanceOf(Article.class));
         mockDatabase.save((Article) withNotNull()); times = 2;
      }};
   }
}
