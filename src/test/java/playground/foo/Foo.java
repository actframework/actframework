package playground.foo;

public interface Foo<BAR_TYPE extends Bar, FOO_TYPE extends Foo<BAR_TYPE, FOO_TYPE>> {
}
