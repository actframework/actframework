package testapp.endpoint;

import act.app.App;
import act.controller.Controller;
import act.controller.annotation.UrlContext;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.exception.UnexpectedNoSuchMethodException;
import org.osgl.inject.InjectException;
import org.osgl.mvc.annotation.GetAction;
import testapp.sbean.*;

@UrlContext("/sbean")
public class SimpleBeanTestBed extends Controller.Util {

    @GetAction("def_const/sbean_no_def_const")
    public void itShallCreateDefaultConstructorForSimpleBeanWithoutDefaultConstructor(App app) {
        app.getInstance(SimpleBeanWithoutDefaultConstructor.class);
    }

    @GetAction("def_const/nsbean_no_def_const")
    public void itShallNotCreateDefaultConstructorForNonSimpleBeanWithoutDefaultConstructor(App app) {
        try {
            app.getInstance(NotSimpleBeanWithoutDefaultConstructor.class);
            throw new UnexpectedException();
        } catch (InjectException e) {
            // this is correct behavior ignore it
        }
    }

    @GetAction("def_const/sbean_def_const")
    public void itShallNotCreateDefaultConstructorForSimpleBeanWithDefaultConstructor(App app) {
        app.getInstance(SimpleBeanWithDefaultConstructor.class);
    }

    @GetAction("def_const/nsbean_def_const")
    public void itShallNotCreateDefaultConstructorForNonSimpleBeanWithDefaultConstructor(App app) {
        app.getInstance(NotSimpleBeanWithDefaultConstructor.class);
    }

    @GetAction("def_const/dsbean_no_def_const")
    public void itShallCreateDefaultConstructorForDerivedSimpleBeanWithoutDefaultConstructor(App app) {
        app.getInstance(DerivedSimpleBean.class);
    }

    @GetAction("getter/create")
    public void itShallCreateGetterForProperty(App app) {
        SimpleBeanWithoutDefaultConstructor bean = new SimpleBeanWithoutDefaultConstructor("foo");
        if (!"foo".equals($.invokeVirtual(bean, "getPropertyWithoutGetter"))){
            throw new UnexpectedException();
        };
    }

    @GetAction("getter/create_nsbean")
    public void itShallNotCreateGetterForNonSbean() {
        NotSimpleBeanWithoutDefaultConstructor bean = new NotSimpleBeanWithoutDefaultConstructor("foo");
        try {
            $.invokeVirtual(bean, "getPropertyWithoutGetter");
            throw new UnexpectedException();
        } catch (UnexpectedNoSuchMethodException e) {
            if (!"foo".equals(bean.propertyWithoutGetter)) {
                throw new UnexpectedException();
            };
        }
    }

    @GetAction("getter/field_read")
    public void itShallTurnFieldReadIntoGetterForSBean() {
        SimpleBeanWithoutDefaultConstructor bean = new SimpleBeanWithoutDefaultConstructor("foo");
        bean.setMagicNumber(2);
        if (4 != bean.magicNumber) {
            throw new UnexpectedException();
        }
    }

    @GetAction("getter/boolean_getter")
    public void checkBooleanPropertyGetter() {
        SimpleBeanWithoutDefaultConstructor bean = new SimpleBeanWithoutDefaultConstructor("foo");
        if (bean.active) {
            throw new UnexpectedException();
        }
    }

    @GetAction("getter/boolean_getter_exists")
    public void itShallNotGenerateGetterForBooleanPropertyIfAlreadyExists() {
        SimpleBeanWithoutDefaultConstructor bean = new SimpleBeanWithoutDefaultConstructor("foo");
        if (!bean.magicFlag) {
            throw new UnexpectedException();
        }
    }

    @GetAction("setter/boolean_setter_exists")
    public void itShallNotGenerateSetterForBooleanPropertyIfAlreadyExists() {
        DerivedSimpleBean bean = new DerivedSimpleBean("foo");
        bean.weirdFlag = false;
        if (!bean.weirdFlag) {
            throw new UnexpectedException();
        }
    }

    @GetAction("intf_extends_sbean")
    public void itShallTreatClassAsSimpleBeanIfItImplementsInterfaceExtendsSimpleBean(App app) {
        app.getInstance(SimpleModel.SomeModel.class);
    }

}
