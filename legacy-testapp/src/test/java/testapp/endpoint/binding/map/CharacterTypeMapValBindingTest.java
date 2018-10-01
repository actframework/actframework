package testapp.endpoint.binding.map;

import org.osgl.util.C;

import java.util.Map;

public class CharacterTypeMapValBindingTest extends SimpleTypeMapValBindingTestBase<Character> {

    public CharacterTypeMapValBindingTest() {
        super("char_v", "char_k");
    }

    @Override
    public Map<String, Character> nonEmptyMap() {
        return C.Map("a", 'a', "b", 'b', "c", 'c');
    }

}
