package integrationTests;

final class ClassWithNestedEnum
{
   enum NestedEnum {ELEM}

   static final class NestedClass
   {
      static void useEnumFromOuterClass() { NestedEnum.values(); }
      @SuppressWarnings("unused") ClassWithNestedEnum getOuter() { return null; }
   }
}
