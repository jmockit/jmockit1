/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit5;

import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.engine.config.*;
import org.junit.jupiter.engine.extension.*;

import mockit.*;

public final class FakeExtensionRegistry extends MockUp<ExtensionRegistry>
{
   @Mock
   public static ExtensionRegistry createRegistryWithDefaultExtensions(Invocation inv, JupiterConfiguration configuration) {
      ExtensionRegistry registry = inv.proceed();
      assert registry != null;

      Extension extension = new JMockitExtension();
      registry.registerExtension(extension, extension);

      return registry;
   }
}
