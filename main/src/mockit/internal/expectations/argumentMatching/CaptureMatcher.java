package mockit.internal.expectations.argumentMatching;

import java.util.*;

import org.jetbrains.annotations.*;

public final class CaptureMatcher<T> implements ArgumentMatcher<CaptureMatcher<T>>
{
   @NotNull private final List<T> valueHolder;

   public CaptureMatcher(@NotNull List<T> valueHolder) { this.valueHolder = valueHolder; }

   @Override
   public boolean same(@NotNull CaptureMatcher<T> other) { return false; }

   @Override
   public boolean matches(@Nullable Object argValue)
   {
      //noinspection unchecked
      valueHolder.add((T) argValue);
      return true;
   }

   @Override
   public void writeMismatchPhrase(@NotNull ArgumentMismatch argumentMismatch) {}
}
