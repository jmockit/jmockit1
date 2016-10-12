package mockit;

import java.util.*;
import static java.util.Arrays.*;

import groovy.lang.*;
import mockit.internal.expectations.transformation.*;
import mockit.internal.state.*;
import org.codehaus.groovy.runtime.*;

abstract class GroovyInvocations extends Invocations implements GroovyObject {
   private static List<String> handledPropertyGetters = asList("any", "anyString", "anyInt", "anyBoolean", "anyLong", "anyDouble", "anyFloat", "anyChar", "anyShort", "anyByte");
   private static List<String> handledPropertySetters = asList("times", "minTimes", "maxTimes");
   private transient MetaClass metaClass = InvokerHelper.getMetaClass(getClass());

   static void handleGetProperty(String name) {
      if (handledPropertyGetters.contains(name)) {
         InvokerHelper.invokeStaticNoArgumentsMethod(ActiveInvocations.class, name);
      }
   }

   static void handleSetProperty(String name, Object value) {
      if (handledPropertySetters.contains(name)) {
         InvokerHelper.invokeStaticMethod(ActiveInvocations.class, name, value);
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
