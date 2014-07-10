/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.interpolation;

import java.awt.*;
import java.awt.geom.*;

import org.junit.*;

import static org.junit.Assert.*;

public final class EvaluatorTest
{
   @Test
   public void evaluateByte()
   {
      assertEvaluation(Byte.class, (byte) 0, (byte) 5, (byte) 2);
   }

   private <T> void assertEvaluation(Class<T> type, T v0, T v1, Object result)
   {
      Evaluator<T> evaluator = Evaluator.create(type);
      T evaluatedValue = evaluator.evaluate(v0, v1, 0.5f);
      assertEquals(result, evaluatedValue);
   }

   @Test
   public void evaluateShort()
   {
      assertEvaluation(Short.class, (short) 0, (short) 5, (short) 2);
   }

   @Test
   public void evaluateInteger()
   {
      assertEvaluation(Integer.class, 0, 5, 2);
   }

   @Test
   public void evaluateLong()
   {
      assertEvaluation(Long.class, 0L, 5L, 2L);
   }

   @Test
   public void evaluateFloat()
   {
      assertEvaluation(Float.class, 0.0f, 5.0f, 2.5f);
   }

   @Test
   public void evaluateDouble()
   {
      assertEvaluation(Double.class, 0.0, 5.0, 2.5);
   }

   @Test
   public void evaluateColor()
   {
      Evaluator<Color> evaluator = Evaluator.create(Color.class);
      Color evaluatedValue = evaluator.evaluate(Color.red, Color.green, 0.5f);
      assertEquals(new Color(128, 128, 0), evaluatedValue);
   }

   @Test
   public void evaluatePoint2D()
   {
      Evaluator<Point2D> evaluator = Evaluator.create(Point2D.class);
      Point2D evaluatedValue = evaluator.evaluate(new Point2D.Float(1, 3), new Point2D.Float(4.5f, 8), 0.5f);
      assertEquals(new Point2D.Float(2.75f, 5.5f), evaluatedValue);
   }

   @Test
   public void evaluateLine2D()
   {
      Evaluator<Line2D> evaluator = Evaluator.create(Line2D.class);
      Line2D evaluatedValue =
         evaluator.evaluate(new Line2D.Float(0, 0, 2, 2), new Line2D.Float(1, 1, 4, 5), 0.5f);
      assertEquals(new Point2D.Float(0.5f, 0.5f), evaluatedValue.getP1());
      assertEquals(new Point2D.Float(3, 3.5f), evaluatedValue.getP2());
   }

   @Test
   public void evaluateDimension2D()
   {
      Evaluator<Dimension2D> evaluator = Evaluator.create(Dimension2D.class);
      Dimension2D evaluatedValue =
         evaluator.evaluate(new Dimension(1, 3), new Dimension(5, 8), 0.5f);
      assertEquals(new Dimension(3, 6), evaluatedValue);
   }

   @Test
   public void evaluateRectangle2D()
   {
      Evaluator<Rectangle2D> evaluator = Evaluator.create(Rectangle2D.class);
      Rectangle2D evaluatedValue =
         evaluator.evaluate(
            new Rectangle2D.Float(0, 0, 2, 2), new Rectangle2D.Float(1, 1, 4, 5), 0.5f);
      assertEquals(new Rectangle2D.Float(0.5f, 0.5f, 3, 3.5f), evaluatedValue);
   }

   @Test
   public void evaluateRoundRectangle2D()
   {
      Evaluator<RoundRectangle2D> evaluator = Evaluator.create(RoundRectangle2D.class);
      RoundRectangle2D evaluatedValue =
         evaluator.evaluate(
            new RoundRectangle2D.Float(0, 0, 2, 2, 0.5f, 0.4f),
            new RoundRectangle2D.Float(1, 1, 4, 5, 1, 1), 0.5f);
      assertEquals(new RoundRectangle2D.Float(0.5f, 0.5f, 3, 3.5f, 0.75f, 0.7f), evaluatedValue);
   }

   @Test
   public void evaluateEllipse2D()
   {
      Evaluator<Ellipse2D> evaluator = Evaluator.create(Ellipse2D.class);
      Ellipse2D evaluatedValue =
         evaluator.evaluate(
            new Ellipse2D.Float(0, 0, 2, 2), new Ellipse2D.Float(1, 1, 4, 5), 0.5f);
      assertEquals(new Ellipse2D.Float(0.5f, 0.5f, 3, 3.5f), evaluatedValue);
   }

   @Test
   public void evaluateArc2D()
   {
      Evaluator<Arc2D> evaluator = Evaluator.create(Arc2D.class);
      Arc2D evaluatedValue =
         evaluator.evaluate(
            new Arc2D.Float(0, 0, 6, 8, 90, 180, Arc2D.OPEN),
            new Arc2D.Float(10, 10, 4, 5, 45, 270, Arc2D.OPEN), 0.5f);
      assertEquals(5, evaluatedValue.getX(), 0);
      assertEquals(5, evaluatedValue.getY(), 0);
      assertEquals(10, evaluatedValue.getMaxX(), 0);
      assertEquals(11.5f, evaluatedValue.getMaxY(), 0);
      assertEquals(67.5, evaluatedValue.getAngleStart(), 0.0);
      assertEquals(225, evaluatedValue.getAngleExtent(), 0.0);
   }

   @Test
   public void evaluateQuadCurve2D()
   {
      Evaluator<QuadCurve2D> evaluator = Evaluator.create(QuadCurve2D.class);
      QuadCurve2D evaluatedValue =
         evaluator.evaluate(
            new QuadCurve2D.Float(0, 0, 6, 8, 2, 3),
            new QuadCurve2D.Float(10, 10, 4, 5, 15, 27), 0.5f);
      assertEquals(5, evaluatedValue.getX1(), 0);
      assertEquals(5, evaluatedValue.getY1(), 0);
      assertEquals(8.5, evaluatedValue.getX2(), 0);
      assertEquals(15, evaluatedValue.getY2(), 0);
      assertEquals(5, evaluatedValue.getCtrlX(), 0.0);
      assertEquals(6.5, evaluatedValue.getCtrlY(), 0.0);
   }

   @Test
   public void evaluateCubicCurve2D()
   {
      Evaluator<CubicCurve2D> evaluator = Evaluator.create(CubicCurve2D.class);
      CubicCurve2D evaluatedValue =
         evaluator.evaluate(
            new CubicCurve2D.Float(0, 0, 2, 3, 6, 8, 2.5f, 3.8f),
            new CubicCurve2D.Float(10, 10, 13, 6, 4, 5, 15, 27), 0.5f);
      assertEquals(5, evaluatedValue.getX1(), 0);
      assertEquals(5, evaluatedValue.getY1(), 0);
      assertEquals(8.75, evaluatedValue.getX2(), 0);
      assertEquals(15.39, evaluatedValue.getY2(), 0.01);
      assertEquals(7.5, evaluatedValue.getCtrlX1(), 0.0);
      assertEquals(4.5, evaluatedValue.getCtrlY1(), 0.0);
      assertEquals(5, evaluatedValue.getCtrlX2(), 0.0);
      assertEquals(6.5, evaluatedValue.getCtrlY2(), 0.0);
   }

   @Test(expected = IllegalArgumentException.class)
   public void evaluateUnknownType()
   {
      Evaluator.create(Path2D.class);
   }
}
