package mockit;

import java.lang.reflect.*;

public interface ServiceDoSomething {
   int doSomething();

   class ServiceDoSomethingProvider {
      static ServiceDoSomething newProxyClassAndInstance(Class<?>... interfacesToImplement) {
         ClassLoader loader = ServiceDoSomething.class.getClassLoader();

         return (ServiceDoSomething) Proxy.newProxyInstance(loader, interfacesToImplement, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws NoSuchMethodException {
               throw new RuntimeException("Should be mocked out");
            }
         });
      }

      static ServiceDoSomething proxyInstance;
   }
}
