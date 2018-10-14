package testapp.endpoint;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import act.controller.annotation.UrlContext;
import act.util.SimpleBean;
import org.osgl.$;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.util.C;
import org.osgl.util.S;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import javax.validation.*;

@UrlContext("/bind/custom")
public class CustomBinder {

    public enum Country {
        ZH(ZhPrince.values()) {
            @Override
            public State resolve(String stateName) {
                return ZhPrince.valueOf(stateName);
            }
        },
        AU(AuState.values()) {
            @Override
            public State resolve(String stateName) {
                return AuState.valueOf(stateName);
            }
        };

        private List<State> states;

        Country(State ... states) {
            this.states = C.listOf(states);
        }

        public List<State> getStates() {
            return this.states;
        }

        public boolean isValid(State state) {
            return this == state.getCountry();
        }

        public abstract State resolve(String stateName);
    }

    public interface State {
        String getName();
        Country getCountry();
    }

    public enum ZhPrince implements State {
        SiChuan, GuangDong, ShanDong, HeBei, ZheJiang, XingJiang, XiZang;

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Country getCountry() {
            return Country.ZH;
        }
    }

    public enum AuState implements State {
        NSW, VIC, QLD, WA, SA, NT, TAS;

        @Override
        public String getName() {
            return name();
        }

        @Override
        public Country getCountry() {
            return Country.AU;
        }
    }

    @Target({FIELD, PARAMETER})
    @Retention(RUNTIME)
    @Bind(CountryStateConstraint.StateBinder.class)
    public @interface CountryState {
        /**
         * A `countryFieldName` is used to get the country value from a bean to be validated
         *
         * Default value is `country`
         *
         * @return the country field name
         */
        String countryFieldName() default "country";
    }

    @Target({TYPE, FIELD, PARAMETER})
    @Retention(RUNTIME)
    @Constraint(validatedBy = CountryStateValidator.class)
    @Documented

    public @interface CountryStateConstraint {

        /**
         * A `stateFieldName` is used to get the state value from a bean to be validated
         *
         * Default value is `state`
         *
         * @return the state field name
         */
        String stateFieldName() default "state";

        /**
         * A `countryFieldName` is used to get the country value from a bean to be validated
         *
         * Default value is `country`
         *
         * @return the country field name
         */
        String countryFieldName() default "country";

        String message() default "{CountryStateConstraint.message}";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        class StateBinder extends Binder<State> {
            @Override
            public State resolve(State bean, String model, ParamValueProvider params) {
                String prefix = S.beforeLast(model, "state");
                String countryFieldName = attribute("countryFieldName");
                String countryParamName = prefix + countryFieldName;

                Country country = Country.valueOf(params.paramVal(countryParamName));
                return country.resolve(params.paramVal(model));
            }
        }
    }

    public static class CountryStateValidator implements ConstraintValidator<CountryStateConstraint, Object> {

        private String stateFieldName;
        private String countryFieldName;

        public CountryStateValidator() {}

        @Override
        public void initialize(CountryStateConstraint constraintAnnotation) {
            this.stateFieldName = constraintAnnotation.stateFieldName();
            this.countryFieldName = constraintAnnotation.countryFieldName();
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext ctx) {
            State state = $.getProperty(value, stateFieldName);
            if (null == state) {
                // we don't validate on `null` value
                return true;
            }
            Country country = $.getProperty(value, countryFieldName);
            if (null == country || !country.isValid(state)) {
                ctx.disableDefaultConstraintViolation();
                ctx.buildConstraintViolationWithTemplate(ctx.getDefaultConstraintMessageTemplate())
                        .addPropertyNode(stateFieldName)
                        .addPropertyNode(countryFieldName)
                        .addConstraintViolation();
                return false;
            }
            return true;
        }
    }

    @CountryStateConstraint
    public static class Address implements SimpleBean {

        public Country country;

        @CountryState
        public State state;

        public Address(Country country, State state) {
            this.country = country;
            this.state = state;
        }
    }

    @PostAction
    public Address create(Country country, @CountryState State state) {
        return new Address(country, state);
    }

    @PostAction("pojo")
    public Address createPojo(@Valid Address address) {
        return address;
    }

}
