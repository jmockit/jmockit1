package blog.blogEntry;

import java.util.*;
import javax.ejb.*;

/**
 * When a <tt>@Tested</tt> object has a dependency whose only known type is an interface, JMockit won't be able to
 * instantiate it, unless it can find a single implementation class in the runtime classpath.
 * The classpath won't be scanned, however; instead, the implementation class must already have been loaded by the JVM.
 * <p/>
 * This particular interface is here only to show this ability; in a real app it probably should be removed.
 */
@Local
public interface BlogEntryDao
{
   List<BlogEntry> find(int maxResults, int firstResult);
   Long count();
}
