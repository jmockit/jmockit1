/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package jmockit.tutorial.domain;

import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

@RunWith(Suite.class)
@SuiteClasses({
   MyBusinessService_MockupsAPI_Test.class,
   MyBusinessService_ExpectationsAPI_Test.class
})
public final class DomainTestSuite
{
}
