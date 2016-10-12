package mockit;

import groovy.lang.*;
import org.codehaus.groovy.runtime.*;

public abstract class GroovyFullVerificationsInOrder extends FullVerificationsInOrder implements GroovyObject {
   private transient MetaClass metaClass = InvokerHelper.getMetaClass(getClass());

   protected GroovyFullVerificationsInOrder() {
   }

   protected GroovyFullVerificationsInOrder(int numberOfIterations) {
      super(numberOfIterations);
   }

   protected GroovyFullVerificationsInOrder(Object... mockedTypesAndInstancesToVerify) {
      super(mockedTypesAndInstancesToVerify);
   }

   protected GroovyFullVerificationsInOrder(Integer numberOfIterations, Object... mockedTypesAndInstancesToVerify) {
      super(numberOfIterations, mockedTypesAndInstancesToVerify);
   }

   static void handleGetProperty(String name) {
      GroovyVerifications.handleGetProperty(name);
   }

   static void handleSetProperty(String name, Object value) {
      GroovyVerifications.handleSetProperty(name, value);
   }

   @Override
   public Object getProperty(String name) {
      handleGetProperty(name);
      return getMetaClass().getProperty(this, name);
   }

   @Override
   public void setProperty(String name, Object value) {
      handleSetProperty(name, value);
   }

   @Override
   public Object invokeMethod(String name, Object args) {
      return getMetaClass().invokeMethod(this, name, args);
   }

   @Override
   public MetaClass getMetaClass() {
      if (metaClass == null) {
         metaClass = InvokerHelper.getMetaClass(getClass());
      }

      return metaClass;
   }

   @Override
   public void setMetaClass(MetaClass metaClass) {
      this.metaClass = metaClass;
   }
}
