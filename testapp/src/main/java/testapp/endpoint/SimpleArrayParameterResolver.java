package testapp.endpoint;

import act.controller.Controller;
import org.osgl.mvc.annotation.Action;

import java.lang.annotation.ElementType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Used to test simple array type parameter resolving
 */
@Controller("/sapr")
@SuppressWarnings("unused")
public class SimpleArrayParameterResolver extends Controller.Util {
    @Action("bool_pa")
    public boolean[] boolP(boolean[] v) {
        return v;
    }

    @Action("bool_wa")
    public Boolean[] bool(Boolean[] v) {
        return v;
    }

    @Action("bool_list")
    public List<Boolean> boolList(List<Boolean> v) {
        return v;
    }

    @Action("bool_set")
    public Set<Boolean> boolSet(Set<Boolean> v) {
        return new TreeSet<Boolean>(v);
    }

    @Action("byte_pa")
    public byte[] byteP(byte[] v) {
        return v;
    }

    @Action("byte_wa")
    public Byte[] takeByte(Byte[] v) {
        return v;
    }

    @Action("byte_list")
    public List<Byte> byteList(List<Byte> v) {
        return v;
    }

    @Action("byte_set")
    public Set<Byte> byteSet(Set<Byte> v) {
        return new TreeSet<Byte>(v);
    }

    @Action("char_pa")
    public char[] takeCharP(char[] v) {
        return v;
    }

    @Action("char_wa")
    public Character[] takeChar(Character[] v) {
        return v;
    }


    @Action("char_list")
    public List<Character> charList(List<Character> v) {
        return v;
    }

    @Action("char_set")
    public Set<Character> charSet(Set<Character> v) {
        return new TreeSet<Character>(v);
    }

    @Action("short_pa")
    public short[] shortP(short[] v) {
        return v;
    }

    @Action("short_wa")
    public Short[] takeShort(Short[] v) {
        return v;
    }

    @Action("int_pa")
    public int[] takeIntP(int[] v) {
        return v;
    }

    @Action("int_wa")
    public Integer[] takeInt(Integer[] v) {
        return v;
    }

    @Action("float_pa")
    public float[] floatP(float[] v) {
        return v;
    }

    @Action("float_wa")
    public Float[] takeFloat(Float[] v) {
        return v;
    }

    @Action("long_pa")
    public long[] longP(long[] v) {
        return v;
    }

    @Action("long_wa")
    public Long[] takeLong(Long[] v) {
        return v;
    }

    @Action("double_pa")
    public double[] doubleP(double[] v) {
        return v;
    }

    @Action("double_wa")
    public Double[] takeDouble(Double[] v) {
        return v;
    }

    @Action("b_int_a")
    public BigInteger[] bigInteger(BigInteger[] v) {
        return v;
    }

    @Action("b_dec_a")
    public BigDecimal[] bigDecimal(BigDecimal[] v) {
        return v;
    }

    @Action("string_a")
    public String[] string(String[] v) {
        return v;
    }

    @Action("enum_a")
    public ElementType[] elementType(ElementType[] v) {
        return v;
    }

}
