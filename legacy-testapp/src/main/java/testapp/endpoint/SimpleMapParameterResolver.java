package testapp.endpoint;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.Action;
import testapp.model.RGB;

import java.util.Map;
import java.util.TreeMap;

/**
 * Used to verify the parameter binding for Map data
 */
@UrlContext("/smpr")
@SuppressWarnings("unused")
public class SimpleMapParameterResolver {

    @Action("xxx")
    public Map<Map<String, Integer>, Map<Integer, String>> xxx(Map<Map<String, Integer>, Map<Integer, String>> xxx) {
        return xxx;
    }

    @Action("bool_v")
    public Map<String, Boolean> boolVal(Map<String, Boolean> v) {
        return new TreeMap<String, Boolean>(v);
    }

    @Action("byte_v")
    public Map<String, Byte> byteVal(Map<String, Byte> v) {
        return new TreeMap<String, Byte>(v);
    }

    @Action("char_v")
    public Map<String, Character> charVal(Map<String, Character> v) {
        return new TreeMap<String, Character>(v);
    }

    @Action("short_v")
    public Map<String, Short> shortVal(Map<String, Short> v) {
        return new TreeMap<String, Short>(v);
    }

    @Action("int_v")
    public Map<String, Integer> intVal(Map<String, Integer> v) {
        return new TreeMap<String, Integer>(v);
    }

    @Action("float_v")
    public Map<String, Float> floatVal(Map<String, Float> v) {
        return new TreeMap<String, Float>(v);
    }


    @Action("long_v")
    public Map<String, Long> longVal(Map<String, Long> v) {
        return new TreeMap<String, Long>(v);
    }

    @Action("double_v")
    public Map<String, Double> doubleVal(Map<String, Double> v) {
        return new TreeMap<String, Double>(v);
    }

    @Action("string_v")
    public Map<String, String> stringVal(Map<String, String> v) {
        return new TreeMap<String, String>(v);
    }


    @Action("enum_v")
    public Map<String, RGB> enumVal(Map<String, RGB> v) {
        return new TreeMap<String, RGB>(v);
    }

    // --- testing simple type keys ----

    @Action("bool_k")
    public Map<Boolean, String> boolKey(Map<Boolean, String> v) {
        return new TreeMap<Boolean, String>(v);
    }

    @Action("byte_k")
    public Map<Byte, String> byteKey(Map<Byte, String> v) {
        return new TreeMap<Byte, String>(v);
    }

    @Action("char_k")
    public Map<Character, String> charKey(Map<Character, String> v) {
        return new TreeMap<Character, String>(v);
    }

    @Action("short_k")
    public Map<Short, String> shortKey(Map<Short, String> v) {
        return new TreeMap<Short, String>(v);
    }

    @Action("int_k")
    public Map<Integer, String> intKey(Map<Integer, String> v) {
        return new TreeMap<Integer, String>(v);
    }

    @Action("float_k")
    public Map<Float, String> floatKey(Map<Float, String> v) {
        return new TreeMap<Float, String>(v);
    }


    @Action("long_k")
    public Map<Long, String> longKey(Map<Long, String> v) {
        return new TreeMap<Long, String>(v);
    }

    @Action("double_k")
    public Map<Double, String> doubleKey(Map<Double, String> v) {
        return new TreeMap<Double, String>(v);
    }

    @Action("string_k")
    public Map<String, String> stringKey(Map<String, String> v) {
        return new TreeMap<String, String>(v);
    }

    @Action("enum_k")
    public Map<RGB, String> enumKey(Map<RGB, String> v) {
        return new TreeMap<RGB, String>(v);
    }
}
