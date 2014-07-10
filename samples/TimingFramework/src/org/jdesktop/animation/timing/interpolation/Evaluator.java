/*
 * Copyright (c) 2005-2006, Sun Microsystems, Inc
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following 
 *     disclaimer in the documentation and/or other materials provided 
 *     with the distribution.
 *   * Neither the name of the TimingFramework project nor the names of its
 *     contributors may be used to endorse or promote products derived 
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jdesktop.animation.timing.interpolation;

import java.awt.Color;
import java.awt.geom.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used by {@link KeyValues} to calculate intermediate values for specific types.
 * It provides built-in support for the following data types:
 * <ul>
 * <li>java.lang.Byte <li>java.lang.Short <li>java.lang.Integer <li>java.lang.Long <li>java.lang.Float
 * <li>java.lang.Double <li>java.awt.Color <li>java.awt.geom.Point2D <li>java.awt.geom.Line2D
 * <li>java.awt.geom.Dimension2D <li> java.awt.geom.Rectangle2D <li>java.awt.geom.RoundRectangle2D
 * <li>java.awt.geom.Ellipse2D <li>java.awt.geom.Arc2D <li>java.awt.geom.QuadCurve2D <li>java.awt.geom.CubicCurve2D
 * </ul>
 *
 * @author Chet
 */
@SuppressWarnings({"ClassReferencesSubclass", "OverlyCoupledClass"})
public abstract class Evaluator<T>
{
   /**
    * Map that holds all registered evaluators, including the pre-defined ones.
    */
   private static final Map<Class<?>, Evaluator<?>> impls = new HashMap<Class<?>, Evaluator<?>>() {{
      put(Byte.class, new EvaluatorByte());
      put(Short.class, new EvaluatorShort());
      put(Integer.class, new EvaluatorInteger());
      put(Long.class, new EvaluatorLong());
      put(Float.class, new EvaluatorFloat());
      put(Double.class, new EvaluatorDouble());
      put(Color.class, new EvaluatorColor());
      put(Point2D.class, new EvaluatorPoint2D());
      put(Line2D.class, new EvaluatorLine2D());
      put(Dimension2D.class, new EvaluatorDimension2D());
      put(Rectangle2D.class, new EvaluatorRectangle2D());
      put(RoundRectangle2D.class, new EvaluatorRoundRectangle2D());
      put(Ellipse2D.class, new EvaluatorEllipse2D());
      put(Arc2D.class, new EvaluatorArc2D());
      put(QuadCurve2D.class, new EvaluatorQuadCurve2D());
      put(CubicCurve2D.class, new EvaluatorCubicCurve2D());
   }};

   static <T> Evaluator<T> create(Class<?> type)
   {
      for (Class<?> registeredClass : impls.keySet()) {
         if (registeredClass.isAssignableFrom(type)) {
            //noinspection unchecked
            return (Evaluator<T>) impls.get(registeredClass);
         }
      }

      throw new IllegalArgumentException(
         "No Evaluator can be found for type " + type + "; consider using" +
         " different types for your values or supplying a custom Evaluator");
   }

   /**
    * Abstract method to evaluate between two boundary values.
    * Built-in implementations all use linear parametric evaluation:
    * <pre>
    *      v = v0 + (v1 - v0) * fraction
    * </pre>
    * Extenders of Evaluator will need to override this method and do something similar for their own types.
    * Note that this mechanism may be used to create non-linear interpolators for specific value types, although it may
    * be simpler to just use the linear/parametric interpolation technique here and perform non-linear interpolation
    * through custom Interpolators rather than perform custom calculations in this method; the point of this class
    * is to allow calculations with new/unknown types, not to provide another mechanism for non-linear interpolation.
    */
   public abstract T evaluate(T v0, T v1, float fraction);

   // Utility methods for use in subclasses ///////////////////////////////////////////////////////////////////////////

   protected final int interpolated(int v0, int v1, float fraction)
   {
      return v0 + (int) ((v1 - v0) * fraction + 0.5f);
   }

   protected final double interpolated(double v0, double v1, float fraction)
   {
      return v0 + (v1 - v0) * fraction;
   }
}

final class EvaluatorByte extends Evaluator<Byte>
{
   @Override
   public Byte evaluate(Byte v0, Byte v1, float fraction)
   {
      return (byte) (v0 + (byte) ((v1 - v0) * fraction));
   }
}

final class EvaluatorShort extends Evaluator<Short>
{
   @Override
   public Short evaluate(Short v0, Short v1, float fraction)
   {
      return (short) (v0 + (short) ((v1 - v0) * fraction));
   }
}

final class EvaluatorInteger extends Evaluator<Integer>
{
   @Override
   public Integer evaluate(Integer v0, Integer v1, float fraction)
   {
      return v0 + (int) ((v1 - v0) * fraction);
   }
}

final class EvaluatorLong extends Evaluator<Long>
{
   @Override
   public Long evaluate(Long v0, Long v1, float fraction)
   {
      return v0 + (long) ((v1 - v0) * fraction);
   }
}

final class EvaluatorFloat extends Evaluator<Float>
{
   @Override
   public Float evaluate(Float v0, Float v1, float fraction)
   {
      return v0 + (v1 - v0) * fraction;
   }
}

final class EvaluatorDouble extends Evaluator<Double>
{
   @Override
   public Double evaluate(Double v0, Double v1, float fraction)
   {
      return v0 + (v1 - v0) * fraction;
   }
}

final class EvaluatorColor extends Evaluator<Color>
{
   @Override
   public Color evaluate(Color v0, Color v1, float fraction)
   {
      int red = interpolated(v0.getRed(), v1.getRed(), fraction);
      int green = interpolated(v0.getGreen(), v1.getGreen(), fraction);
      int blue = interpolated(v0.getBlue(), v1.getBlue(), fraction);
      int alpha = interpolated(v0.getAlpha(), v1.getAlpha(), fraction);
      Color value = new Color(red, green, blue, alpha);
      return value;
   }
}

abstract class EvaluatorWithCachedValue<T extends Cloneable> extends Evaluator<T>
{
   T value;
}

final class EvaluatorPoint2D extends EvaluatorWithCachedValue<Point2D>
{
   @Override
   public Point2D evaluate(Point2D v0, Point2D v1, float fraction)
   {
      if (value == null) {
         value = (Point2D) v0.clone();
      }

      double x = interpolated(v0.getX(), v1.getX(), fraction);
      double y = interpolated(v0.getY(), v1.getY(), fraction);
      value.setLocation(x, y);
      return value;
   }
}

final class EvaluatorLine2D extends EvaluatorWithCachedValue<Line2D>
{
   @Override
   public Line2D evaluate(Line2D v0, Line2D v1, float fraction)
   {
      if (value == null) {
         value = (Line2D) v0.clone();
      }

      double x1 = interpolated(v0.getX1(), v1.getX1(), fraction);
      double y1 = interpolated(v0.getY1(), v1.getY1(), fraction);
      double x2 = interpolated(v0.getX2(), v1.getX2(), fraction);
      double y2 = interpolated(v0.getY2(), v1.getY2(), fraction);
      value.setLine(x1, y1, x2, y2);
      return value;
   }
}

final class EvaluatorDimension2D extends EvaluatorWithCachedValue<Dimension2D>
{
   @Override
   public Dimension2D evaluate(Dimension2D v0, Dimension2D v1, float fraction)
   {
      if (value == null) {
         value = (Dimension2D) v0.clone();
      }

      double w = interpolated(v0.getWidth(), v1.getWidth(), fraction);
      double h = interpolated(v0.getHeight(), v1.getHeight(), fraction);
      value.setSize(w, h);
      return value;
   }
}

abstract class EvaluatorForRectangularShape<T extends RectangularShape> extends EvaluatorWithCachedValue<T>
{
   T newValue(T givenValue)
   {
      if (value == null) {
         //noinspection unchecked
         value = (T) givenValue.clone();
      }

      return value;
   }

   final double interpolatedX(T v0, T v1, float f) { return interpolated(v0.getX(), v1.getX(), f); }
   final double interpolatedY(T v0, T v1, float f) { return interpolated(v0.getY(), v1.getY(), f); }
   final double interpolatedWidth(T v0, T v1, float f) { return interpolated(v0.getWidth(), v1.getWidth(), f); }
   final double interpolatedHeight(T v0, T v1, float f) { return interpolated(v0.getHeight(), v1.getHeight(), f); }
}

final class EvaluatorRectangle2D extends EvaluatorForRectangularShape<Rectangle2D>
{
   @Override
   public Rectangle2D evaluate(Rectangle2D v0, Rectangle2D v1, float fraction)
   {
      double x = interpolatedX(v0, v1, fraction);
      double y = interpolatedY(v0, v1, fraction);
      double w = interpolatedWidth(v0, v1, fraction);
      double h = interpolatedHeight(v0, v1, fraction);
      newValue(v0).setRect(x, y, w, h);
      return value;
   }
}

final class EvaluatorRoundRectangle2D extends EvaluatorForRectangularShape<RoundRectangle2D>
{
   @Override
   public RoundRectangle2D evaluate(RoundRectangle2D v0, RoundRectangle2D v1, float fraction)
   {
      double x = interpolatedX(v0, v1, fraction);
      double y = interpolatedY(v0, v1, fraction);
      double w = interpolatedWidth(v0, v1, fraction);
      double h = interpolatedHeight(v0, v1, fraction);
      double arcw = interpolated(v0.getArcWidth(), v1.getArcWidth(), fraction);
      double arch = interpolated(v0.getArcHeight(), v1.getArcHeight(), fraction);
      newValue(v0).setRoundRect(x, y, w, h, arcw, arch);
      return value;
   }
}

final class EvaluatorEllipse2D extends EvaluatorForRectangularShape<Ellipse2D>
{
   @Override
   public Ellipse2D evaluate(Ellipse2D v0, Ellipse2D v1, float fraction)
   {
      double x = interpolatedX(v0, v1, fraction);
      double y = interpolatedY(v0, v1, fraction);
      double w = interpolatedWidth(v0, v1, fraction);
      double h = interpolatedHeight(v0, v1, fraction);
      newValue(v0).setFrame(x, y, w, h);
      return value;
   }
}

final class EvaluatorArc2D extends EvaluatorForRectangularShape<Arc2D>
{
   @Override
   public Arc2D evaluate(Arc2D v0, Arc2D v1, float fraction)
   {
      double x = interpolatedX(v0, v1, fraction);
      double y = interpolatedY(v0, v1, fraction);
      double w = interpolatedWidth(v0, v1, fraction);
      double h = interpolatedHeight(v0, v1, fraction);
      double start = interpolated(v0.getAngleStart(), v1.getAngleStart(), fraction);
      double extent = interpolated(v0.getAngleExtent(), v1.getAngleExtent(), fraction);
      newValue(v0).setArc(x, y, w, h, start, extent, v0.getArcType());
      return value;
   }
}

final class EvaluatorQuadCurve2D extends EvaluatorWithCachedValue<QuadCurve2D>
{
   @Override
   public QuadCurve2D evaluate(QuadCurve2D v0, QuadCurve2D v1, float fraction)
   {
      double x1 = interpolated(v0.getX1(), v1.getX1(), fraction);
      double y1 = interpolated(v0.getY1(), v1.getY1(), fraction);
      double x2 = interpolated(v0.getX2(), v1.getX2(), fraction);
      double y2 = interpolated(v0.getY2(), v1.getY2(), fraction);
      double ctrlx = interpolated(v0.getCtrlX(), v1.getCtrlX(), fraction);
      double ctrly = interpolated(v0.getCtrlY(), v1.getCtrlY(), fraction);

      if (value == null) {
         value = (QuadCurve2D) v0.clone();
      }

      value.setCurve(x1, y1, ctrlx, ctrly, x2, y2);
      return value;
   }
}

final class EvaluatorCubicCurve2D extends EvaluatorWithCachedValue<CubicCurve2D>
{
   @Override
   public CubicCurve2D evaluate(CubicCurve2D v0, CubicCurve2D v1, float fraction)
   {
      double x1 = interpolated(v0.getX1(), v1.getX1(), fraction);
      double y1 = interpolated(v0.getY1(), v1.getY1(), fraction);
      double x2 = interpolated(v0.getX2(), v1.getX2(), fraction);
      double y2 = interpolated(v0.getY2(), v1.getY2(), fraction);
      double ctrlx1 = interpolated(v0.getCtrlX1(), v1.getCtrlX1(), fraction);
      double ctrly1 = interpolated(v0.getCtrlY1(), v1.getCtrlY1(), fraction);
      double ctrlx2 = interpolated(v0.getCtrlX2(), v1.getCtrlX2(), fraction);
      double ctrly2 = interpolated(v0.getCtrlY2(), v1.getCtrlY2(), fraction);

      if (value == null) {
         value = (CubicCurve2D) v0.clone();
      }

      value.setCurve(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
      return value;
   }
}
