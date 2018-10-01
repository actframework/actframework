package testapp.endpoint;

import act.controller.Controller;
import act.controller.annotation.UrlContext;
import act.util.JsonView;
import org.osgl.mvc.annotation.Action;
import testapp.model.RGB;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Used to test simple array|list|set type parameter resolving
 */
@UrlContext("/sapr")
@SuppressWarnings("unused")
public class SimpleArrayParameterResolver extends Controller.Util {
    @Action("bool_pa")
    public boolean[] boolPA(boolean[] v) {
        return v;
    }

    @Action("bool_wa")
    public Boolean[] boolWA(Boolean[] v) {
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
    @JsonView
    public byte[] bytePA(byte[] v) {
        return v;
    }

    @Action("byte_wa")
    public Byte[] byteWA(Byte[] v) {
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
    public char[] charPA(char[] v) {
        return v;
    }

    @Action("char_wa")
    public Character[] charWA(Character[] v) {
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
    public short[] shortPA(short[] v) {
        return v;
    }

    @Action("short_wa")
    public Short[] shortWA(Short[] v) {
        return v;
    }

    @Action("short_list")
    public List<Short> shortList(List<Short> v) {
        return v;
    }

    @Action("short_set")
    public Set<Short> shortSet(Set<Short> v) {
        return new TreeSet<Short>(v);
    }

    @Action("int_pa")
    public int[] intPA(int[] v) {
        return v;
    }

    @Action("int_wa")
    public Integer[] intWA(Integer[] v) {
        return v;
    }

    @Action("int_list")
    public List<Integer> intList(List<Integer> v) {
        return v;
    }

    @Action("int_set")
    public Set<Integer> intSet(Set<Integer> v) {
        return new TreeSet<Integer>(v);
    }

    @Action("float_pa")
    public float[] floatP(float[] v) {
        return v;
    }

    @Action("float_wa")
    public Float[] takeFloat(Float[] v) {
        return v;
    }

    @Action("float_list")
    public List<Float> floatList(List<Float> v) {
        return v;
    }

    @Action("float_set")
    public Set<Float> floatSet(Set<Float> v) {
        return new TreeSet<Float>(v);
    }

    @Action("long_pa")
    public long[] longP(long[] v) {
        return v;
    }

    @Action("long_wa")
    public Long[] takeLong(Long[] v) {
        return v;
    }

    @Action("long_list")
    public List<Long> longList(List<Long> v) {
        return v;
    }

    @Action("long_set")
    public Set<Long> longSet(Set<Long> v) {
        return new TreeSet<Long>(v);
    }

    @Action("double_pa")
    public double[] doublePA(double[] v) {
        return v;
    }

    @Action("double_wa")
    public Double[] doubleWA(Double[] v) {
        return v;
    }

    @Action("double_list")
    public List<Double> doubleList(List<Double> v) {
        return v;
    }

    @Action("double_set")
    public Set<Double> doubleSet(Set<Double> v) {
        return new TreeSet<Double>(v);
    }

    @Action("b_int_wa")
    public BigInteger[] bigIntegerWA(BigInteger[] v) {
        return v;
    }

    @Action("b_int_list")
    public List<BigInteger> bigIntegerList(List<BigInteger> v) {
        return v;
    }

    @Action("b_int_set")
    public Set<BigInteger> bigIntegerSet(Set<BigInteger> v) {
        return new TreeSet<BigInteger>(v);
    }

    @Action("b_dec_wa")
    public BigDecimal[] bigDecimal(BigDecimal[] v) {
        return v;
    }

    @Action("b_dec_list")
    public List<BigDecimal> bigDecimalList(List<BigDecimal> v) {
        return v;
    }

    @Action("b_dec_set")
    public Set<BigDecimal> bigDecimalSet(Set<BigDecimal> v) {
        return new TreeSet<BigDecimal>(v);
    }

    @Action("string_wa")
    public String[] string(String[] v) {
        return v;
    }

    @Action("string_list")
    public List<String> stringList(List<String> v) {
        return v;
    }

    @Action("string_set")
    public Set<String> stringSet(Set<String> v) {
        return new TreeSet<String>(v);
    }

    @Action("enum_wa")
    public RGB[] elementType(RGB[] v) {
        return v;
    }


    @Action("enum_list")
    public List<RGB> enumList(List<RGB> v) {
        return v;
    }

    @Action("enum_set")
    public Set<RGB> enumSet(Set<RGB> v) {
        return new TreeSet<RGB>(v);
    }

    public void x(Set v) {
    }

    public void y(Map<String, Integer> y) {}
}
