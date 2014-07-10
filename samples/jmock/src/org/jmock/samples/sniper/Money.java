package org.jmock.samples.sniper;

import java.math.*;

public final class Money implements Comparable<Money>
{
   private final BigDecimal amount;

   public Money(BigDecimal amount)
   {
      this.amount = amount;
   }

   public Money(int amount)
   {
      this(new BigDecimal(amount));
   }

   public Money add(Money other)
   {
      return new Money(amount.add(other.amount));
   }

   public Money subtract(Money other)
   {
      return new Money(amount.subtract(other.amount));
   }

   @Override
   public String toString()
   {
      return "£" + amount;
   }

   @Override
   public boolean equals(Object o)
   {
      return
         this == o ||
         !(o == null || getClass() != o.getClass()) && amount.compareTo(((Money) o).amount) == 0;
   }

   @Override
   public int hashCode()
   {
      return amount.hashCode();
   }

   public int compareTo(Money other)
   {
      return amount.compareTo(other.amount);
   }

   public static Money min(Money m1, Money m2)
   {
      return new Money(m1.amount.min(m2.amount));
   }
}
