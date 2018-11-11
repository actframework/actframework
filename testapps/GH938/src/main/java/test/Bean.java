package test;

public class Bean {

    public static class Attr {

        public static class Test {
            private String a;

            public Test(String a) {
                this.a = a;
            }

            public String getA() {
                return a;
            }

            public void setA(String a) {
                this.a = a;
            }
        }

        private Test test;

        private String a;

        public Attr(Test test) {
            this.test = test;
        }

        public Test getTest() {
            return test;
        }

        public void setTest(Test test) {
            this.test = test;
        }

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }
    }

    public Bean(String a) {
        this.a = a;
        Attr.Test test = new Attr.Test(a);
        this.mfield = new Attr(test);
    }
    private String a;
    private Attr mfield;

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }

    public Attr getMfield() {
        return mfield;
    }

    public void setMfield(Attr attr) {
        this.mfield = attr;
    }
}