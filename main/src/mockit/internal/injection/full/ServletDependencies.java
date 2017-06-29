/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection.full;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.annotation.*;
import javax.servlet.*;
import javax.servlet.descriptor.*;
import javax.servlet.http.*;
import static java.util.Collections.enumeration;

import mockit.internal.injection.*;

/**
 * Detects and resolves dependencies belonging to the {@code javax.servlet} API, namely {@code ServletContext}
 * and {@code HttpSession}.
 */
final class ServletDependencies
{
   static boolean isApplicable(@Nonnull Class<?> dependencyType)
   {
      return dependencyType == HttpSession.class || dependencyType == ServletContext.class;
   }

   @Nonnull private final InjectionState injectionState;

   ServletDependencies(@Nonnull InjectionState injectionState) { this.injectionState = injectionState; }

   @Nonnull
   Object createAndRegisterDependency(@Nonnull Class<?> dependencyType)
   {
      if (dependencyType == ServletContext.class) {
         return createAndRegisterServletContext();
      }

      return createAndRegisterHttpSession();
   }

   @Nonnull
   private ServletContext createAndRegisterServletContext()
   {
      ServletContext context = new ServletContext() {
         private final Map<String, String> init = new HashMap<String, String>();
         private final Map<String, Object> attrs = new HashMap<String, Object>();

         @Override public String getContextPath() { return ""; }
         @Override public ServletContext getContext(String uriPath) { return null; }
         @Override public int getMajorVersion() { return 3; }
         @Override public int getMinorVersion() { return 0; }
         @Override public int getEffectiveMajorVersion() { return 3; }
         @Override public int getEffectiveMinorVersion() { return 0; }
         @Override public String getMimeType(String file) { return null; }
         @Override public String getRealPath(String path) { return null; }
         @Override public Set<String> getResourcePaths(String path) { return null; }
         @Override public URL getResource(String path) { return getClass().getResource(path); }
         @Override public InputStream getResourceAsStream(String path) { return getClass().getResourceAsStream(path); }
         @Override public RequestDispatcher getRequestDispatcher(String path) { return null; }
         @Override public RequestDispatcher getNamedDispatcher(String name) { return null; }
         @Override public String getServletContextName() { return null; }
         @Override public String getServerInfo() { return "JMockit 1.x"; }
         @Override public ClassLoader getClassLoader() { return getClass().getClassLoader(); }

         // Deprecated/logging methods: do nothing.
         @Override public Servlet getServlet(String name) { return null; }
         @Override public Enumeration<Servlet> getServlets() { return null; }
         @Override public Enumeration<String> getServletNames() { return null; }
         @Override public void log(String msg) {}
         @Override public void log(Exception exception, String msg) { }
         @Override public void log(String message, Throwable throwable) {}

         // Context initialization parameters.
         @Override public Enumeration<String> getInitParameterNames() { return enumeration(init.keySet()); }
         @Override public String getInitParameter(String name) { return init.get(name); }
         @Override public boolean setInitParameter(String name, String value) { return init.put(name, value) == null; }

         // Context attributes.
         @Override public Enumeration<String> getAttributeNames() { return enumeration(attrs.keySet()); }
         @Override public Object getAttribute(String name) { return attrs.get(name); }
         @Override public void setAttribute(String name, Object value) { attrs.put(name, value); }
         @Override public void removeAttribute(String name) { attrs.remove(name); }

         // Un-implemented methods, which may get a non-empty implementation eventually.
         @Override public ServletRegistration.Dynamic addServlet(String name, String className) { return null; }
         @Override public ServletRegistration.Dynamic addServlet(String name, Servlet servlet) { return null; }
         @Override public ServletRegistration.Dynamic addServlet(String nm, Class<? extends Servlet> c) { return null; }
         @Override public <T extends Servlet> T createServlet(Class<T> clazz) { return null; }
         @Override public ServletRegistration getServletRegistration(String servletName) { return null; }
         @Override public Map<String, ? extends ServletRegistration> getServletRegistrations() { return null; }
         @Override public FilterRegistration.Dynamic addFilter(String name, String className) { return null; }
         @Override public FilterRegistration.Dynamic addFilter(String name, Filter filter) { return null; }
         @Override public FilterRegistration.Dynamic addFilter(String name, Class<? extends Filter> cl) { return null; }
         @Override public <T extends Filter> T createFilter(Class<T> clazz) { return null; }
         @Override public FilterRegistration getFilterRegistration(String filterName) { return null; }
         @Override public Map<String, ? extends FilterRegistration> getFilterRegistrations() { return null; }
         @Override public SessionCookieConfig getSessionCookieConfig() { return null; }
         @Override public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {}
         @Override public Set<SessionTrackingMode> getDefaultSessionTrackingModes() { return null; }
         @Override public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() { return null; }
         @Override public void addListener(String className) {}
         @Override public <T extends EventListener> void addListener(T t) {}
         @Override public void addListener(Class<? extends EventListener> listenerClass) {}
         @Override public <T extends EventListener> T createListener(Class<T> clazz) { return null; }
         @Override public JspConfigDescriptor getJspConfigDescriptor() { return null; }
         @Override public void declareRoles(String... roleNames) {}
         @Override public String getVirtualServerName() { return null; }
      };

      InjectionPoint injectionPoint = new InjectionPoint(ServletContext.class);
      InjectionState.saveGlobalDependency(injectionPoint, context);
      return context;
   }

   @Nonnull
   private HttpSession createAndRegisterHttpSession()
   {
      HttpSession session = new HttpSession() {
         private final String id = String.valueOf(Math.abs(new Random().nextInt()));
         private final long creationTime = System.currentTimeMillis();
         private final Map<String, Object> attrs = new HashMap<String, Object>();
         private int maxInactiveInterval;
         private boolean invalidated;

         @Override public String getId() { return id; }

         @Override public int getMaxInactiveInterval() { return maxInactiveInterval; }
         @Override public void setMaxInactiveInterval(int interval) { maxInactiveInterval = interval; }

         @Override public long getCreationTime() { checkValid(); return creationTime; }
         @Override public long getLastAccessedTime() { checkValid(); return creationTime; }
         @Override public boolean isNew() { checkValid(); return false; }

         @Override public Enumeration<String> getAttributeNames() { checkValid(); return enumeration(attrs.keySet()); }
         @Override public Object getAttribute(String name) { checkValid(); return attrs.get(name); }
         @Override public void setAttribute(String name, Object value) { checkValid(); attrs.put(name, value); }
         @Override public void removeAttribute(String name) { checkValid(); attrs.remove(name); }

         @Override
         public void invalidate()
         {
            checkValid();
            attrs.clear();
            invalidated = true;
         }

         private void checkValid()
         {
            if (invalidated) {
               throw new IllegalStateException("Session is invalid");
            }
         }

         @Override
         public ServletContext getServletContext()
         {
            ServletContext context = InjectionState.getGlobalDependency(new InjectionPoint(ServletContext.class));

            if (context == null) {
               context = createAndRegisterServletContext();
            }

            return context;
         }

         // Deprecated methods: do nothing.
         @Override public Object getValue(String name) { return null; }
         @Override public void putValue(String name, Object value) {}
         @Override public void removeValue(String name) {}
         @Override public String[] getValueNames() { return null; }
         @SuppressWarnings("deprecation") @Override public HttpSessionContext getSessionContext() { return null; }
      };

      InjectionPoint injectionPoint = new InjectionPoint(HttpSession.class);
      injectionState.saveInstantiatedDependency(injectionPoint, session);
      return session;
   }
}
