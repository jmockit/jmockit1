package mockit;

import groovy.lang.*;
import mockit.internal.expectations.transformation.*;
import org.codehaus.groovy.runtime.*;

public abstract class GroovyExpectations extends Expectations implements GroovyObject {
   private transient MetaClass metaClass = InvokerHelper.getMetaClass(getClass());

   protected GroovyExpectations() {
   }

   protected GroovyExpectations(Object... classesOrObjectsToBePartiallyMocked) {
      super(classesOrObjectsToBePartiallyMocked);
   }

   protected GroovyExpectations(Integer numberOfIterations, Object... classesOrObjectsToBePartiallyMocked) {
      super(numberOfIterations, classesOrObjectsToBePartiallyMocked);
   }

   static void handleGetProperty(String name) {
      GroovyInvocations.handleGetProperty(name);
   }

   static void handleSetProperty(String name, Object value) {
      GroovyInvocations.handleSetProperty(name, value);

      if ("result".equals(name)) {
         ActiveInvocations.addResult(value);
      }
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
