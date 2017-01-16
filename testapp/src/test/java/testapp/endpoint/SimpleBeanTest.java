package testapp.endpoint;

import org.junit.Test;

import java.io.IOException;

public class SimpleBeanTest extends EndpointTester {

    @Test
    public void itShallCreateDefaultConstructorForSimpleBeanWithoutDefaultConstructor() throws IOException {
        url("/sbean/def_const/sbean_no_def_const").get();
        checkRespCode();
    }

    @Test
    public void itShallNotCreateDefaultConstructorForNonSimpleBeanWithoutDefaultConstructor() throws IOException {
        url("/sbean/def_const/nsbean_no_def_const").get();
        checkRespCode();
    }

    @Test
    public void itShallNotCreateDefaultConstructorForSimpleBeanWithDefaultConstructor() throws IOException {
        url("/sbean/def_const/sbean_def_const").get();
        checkRespCode();
    }

    @Test
    public void itShallNotCreateDefaultConstructorForNonSimpleBeanWithDefaultConstructor() throws Exception {
        url("/sbean/def_const/nsbean_def_const").get();
        checkRespCode();
    }

    @Test
    public void itShallCreateDefaultConstructorForDerivedSimpleBeanWithoutDefaultConstructor() throws Exception {
        url("/sbean/def_const/dsbean_no_def_const").get();
        checkRespCode();
    }

    @Test
    public void itShallCreateGetterForProperty() throws IOException {
        url("/sbean/getter/create").get();
        checkRespCode();
    }

    @Test
    public void itShallNotCreateGetterForNonSbean() throws IOException {
        url("/sbean/getter/create_nsbean").get();
        checkRespCode();
    }

    @Test
    public void itShallTurnFieldReadIntoGetterForSBean() throws IOException {
        url("/sbean/getter/field_read").get();
        checkRespCode();
    }

    @Test
    public void checkBooleanPropertyGetter() throws IOException {
        url("/sbean/getter/boolean_getter").get();
        checkRespCode();
    }

    @Test
    public void itShallNotGenerateGetterForBooleanPropertyIfAlreadyExists() throws IOException {
        url("/sbean/getter/boolean_getter_exists").get();
        checkRespCode();
    }

    @Test
    public void itShallNotGenerateSetterForBooleanPropertyIfAlreadyExists() throws IOException {
        url("/sbean/setter/boolean_setter_exists").get();
        checkRespCode();
    }

    @Test
    public void itShallTreatClassAsSimpleBeanIfItImplementsInterfaceExtendsSimpleBean() throws IOException {
        url("/sbean/intf_extends_sbean").get();
        checkRespCode();
    }
}
