package testapp.endpoint;

import org.junit.Test;

public class GHIssue325 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/325/data/foo=3;bar=6/key/bar").get();
        bodyEq("6");
    }

    @Test
    public void test2() throws Exception {
        url("/gh/325/person/john;age=33;score=99;id=728/name").get();
        bodyEq("john");
    }

    @Test
    public void test2_1() throws Exception {
        url("/gh/325/person/john;age=33;score=99;id=728/score").get();
        bodyEq("99");
    }

    @Test
    public void test2_2() throws Exception {
        url("/gh/325/person/john;age=33;score=99;id=728/age").get();
        bodyEq("33");
    }

    @Test
    public void test3() throws Exception {
        url("/gh/325/list/x=1,2,1;y=2,7,0/y").get();
        bodyEq("9");
    }

    @Test
    public void test3_1() throws Exception {
        url("/gh/325/list/x=1,2,1;y=2,7,0/x").get();
        bodyEq("4");
    }

}
