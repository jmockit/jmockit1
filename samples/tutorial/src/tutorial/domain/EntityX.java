/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package tutorial.domain;

import java.math.*;
import javax.persistence.*;

import static javax.persistence.GenerationType.*;

@Entity
public class EntityX
{
   @Id @GeneratedValue private int id;
   @Column(length = 20, nullable = false) private String someProperty;
   @Column(length = 100) private String customerEmail;
   @Column(precision = 15, scale = 2) private BigDecimal total;

   public EntityX() {}

   public EntityX(int type, String code, String customerEmail)
   {
      this.customerEmail = customerEmail;
      someProperty = "abc";
   }

   public int getId() { return id; }

   public String getSomeProperty() { return someProperty; }
   private void setSomeProperty(String someProperty) { this.someProperty = someProperty; }

   public String getCustomerEmail() { return customerEmail; }
   public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

   public BigDecimal getTotal() { return total; }
   public void setTotal(BigDecimal total) { this.total = total; }
}
