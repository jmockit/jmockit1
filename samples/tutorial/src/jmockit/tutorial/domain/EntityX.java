/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package jmockit.tutorial.domain;

import java.math.*;
import javax.persistence.*;

@Entity
public class EntityX
{
   private String someProperty;
   private String customerEmail;
   private BigDecimal total;

   public EntityX() {}

   public EntityX(int type, String code, String customerEmail)
   {
      this.customerEmail = customerEmail;
      someProperty = "abc";
   }

   public String getSomeProperty() { return someProperty; }
   private void setSomeProperty(String someProperty) { this.someProperty = someProperty; }

   public String getCustomerEmail() { return customerEmail; }
   public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

   public BigDecimal getTotal() { return total; }
   public void setTotal(BigDecimal total) { this.total = total; }
}
