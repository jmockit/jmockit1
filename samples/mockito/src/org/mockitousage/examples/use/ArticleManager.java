/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockitousage.examples.use;

import java.util.List;

public class ArticleManager
{
   private final ArticleCalculator calculator;
   private final ArticleDatabase database;

   public ArticleManager(ArticleCalculator calculator, ArticleDatabase database)
   {
      this.calculator = calculator;
      this.database = database;
      // Comparison note: with JMockit, the following way of obtaining the dependency works just as
      // well, eliminating the need for the "database" constructor parameter:
      // this.database = new ArticleDatabase();
   }

   public void updateArticleCounters(String newspaper)
   {
      int articles = calculator.countArticles(newspaper);
      int polishArticles = calculator.countArticlesInPolish(newspaper);

      database.updateNumberOfArticles(newspaper, articles);
      database.updateNumberOfPolishArticles(newspaper, polishArticles);
      database.updateNumberOfEnglishArticles(newspaper, articles - polishArticles);
   }

   public void updateRelatedArticlesCounters(String newspaper)
   {
      List<Article> articles = database.getArticlesFor(newspaper);

      for (Article article : articles) {
         int numberOfRelatedArticles = calculator.countNumberOfRelatedArticles(article);
         article.setNumberOfRelatedArticles(numberOfRelatedArticles);
         database.save(article);
      }
   }
}
