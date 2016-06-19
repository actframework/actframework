package act.app.util;

import act.TestBase;
import act.app.App;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

public class SimpleTypeInstanceFactoryTest extends TestBase {
    @Test
    public void test() {
        eq(Boolean.FALSE, get(Boolean.class));
        eq(Boolean.FALSE, get(boolean.class));
        eq('\0', get(Character.class));
        eq('\0', get(char.class));
        eq(0, get(Byte.class));
        eq(0, get(byte.class));
        eq(0, get(Short.class));
        eq(0, get(short.class));
        eq(0, get(Integer.class));
        eq(0, get(int.class));
        eq(0F, get(Float.class));
        eq(0F, get(float.class));
        eq(0L, get(Long.class));
        eq(0L, get(long.class));
        eq(0D, get(Double.class));
        eq(0D, get(double.class));
        eq(BigInteger.valueOf(0L), get(BigInteger.class));
        eq(BigDecimal.valueOf(0L), get(BigDecimal.class));
        eq("", get(String.class));
        assertNull(get(App.class));
    }

    private Object get(Class c) {
        return SimpleTypeInstanceFactory.newInstance(c);
    }
}
