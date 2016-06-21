/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */

/**
 * Provides integration with <em>Spring Framework</em>'s
 * {@linkplain org.springframework.beans.factory.BeanFactory bean factories}.
 * <p/>
 * This integration allows calls to {@link org.springframework.beans.factory.BeanFactory#getBean(String)} to be resolved
 * with {@link mockit.Tested @Tested} objects and their injected dependencies (including
 * {@link mockit.Injectable @Injectable} instances) from the currently executing test class.
 */
package mockit.integration.springframework;
