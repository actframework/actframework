package testapp.endpoint;

import act.controller.Controller;
import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.Action;
import testapp.model.RGB;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.validation.constraints.NotNull;

/**
 * Used to test simple type parameter resolving
 */
@UrlContext("/spr")
public class SimpleParameterResolver extends Controller.Util {

    @Action("bool_p")
    public boolean boolP(boolean v) {
        return v;
    }

    @Action("bool_w")
    public Boolean bool(Boolean v) {
        return v;
    }

    @Action("byte_p")
    public byte byteP(byte v) {
        return v;
    }

    @Action("byte_w")
    public Byte takeByte(Byte v) {
        return v;
    }

    @Action("char_p")
    public char takeCharP(char v) {
        return v;
    }

    @Action("v_is_required")
    public void foo(@NotNull int v) {
    }

    @Action("char_w")
    public Character takeChar(Character v) {
        return v;
    }

    @Action("short_p")
    public short shortP(short v) {
        return v;
    }

    @Action("short_w")
    public Short takeShort(Short v) {
        return v;
    }

    @Action("int_p")
    public int takeIntP(int v) {
        return v;
    }

    @Action("int_w")
    public Integer takeInt(Integer v) {
        return v;
    }

    @Action("float_p")
    public float floatP(float v) {
        return v;
    }

    @Action("float_w")
    public Float takeFloat(Float v) {
        return v;
    }

    @Action("long_p")
    public long longP(long v) {
        return v;
    }

    @Action("long_w")
    public Long takeLong(Long v) {
        return v;
    }

    @Action("double_p")
    public double doubleP(double v) {
        return v;
    }

    @Action("double_w")
    public Double takeDouble(Double v) {
        return v;
    }

    @Action("b_int")
    public BigInteger bigInteger(BigInteger v) {
        return v;
    }

    @Action("b_dec")
    public BigDecimal bigDecimal(BigDecimal v) {
        return v;
    }

    @Action("string")
    public String string(String v) {
        return v;
    }

    @Action("enum")
    public RGB elementType(RGB v) {
        return v;
    }

}
