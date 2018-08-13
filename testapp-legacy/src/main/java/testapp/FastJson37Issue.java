package testapp;

import com.alibaba.fastjson.JSON;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class FastJson37Issue {

    public static class BoolVal {
        private boolean v;
        public void setV(boolean v) {
            this.v = v;
        }

        @Override
        public String toString() {
            return String.valueOf(v);
        }
    }

    public static class LongVal {
        private long v;
        public void setV(long v) {
            this.v = v;
        }

        @Override
        public String toString() {
            return String.valueOf(v);
        }
    }

    public static class IntegerVal {
        private int v;

        public void setV(int v) {
            this.v = v;
        }

        @Override
        public String toString() {
            return String.valueOf(v);
        }
    }

    public static class FloatVal {
        private float v;

        public void setV(float v) {
            this.v = v;
        }

        @Override
        public String toString() {
            return String.valueOf(v);
        }
    }

    public static void gh1422() {
        String strOk = "{\"v\": 111}";
        BoolVal ok = JSON.parseObject(strOk, BoolVal.class);
        System.out.println("ok:" + ok);

        String strBad = "{\"v\":111}";
        BoolVal bad = JSON.parseObject(strBad, BoolVal.class);
        System.out.println("bad" + bad);
    }

    public static void gh1423() {
        BigInteger n = new BigInteger(String.valueOf(Long.MAX_VALUE)).add(new BigInteger("1"));
        Map<String, BigInteger> map = new HashMap<>();
        map.put("v", n);
        String strBad = JSON.toJSONString(map);
        System.out.println("prepare to parse: " + strBad);

        System.out.println("We expect the following line to raise NumberFormatException, but it will print out something:");
        System.out.println(JSON.parseObject(strBad, LongVal.class));

        System.out.println("While Long.parseLong(String) call does raise NumberFormatException:");
        System.out.println(Long.parseLong(n.toString()));
    }

    public static void main(String[] args) {
        String fastJsonVersion = JSON.VERSION;
        System.out.println("fastjson version: " + fastJsonVersion);

        Map<String, Long> intOverflowMap = new HashMap<>();
        long intOverflow = Integer.MAX_VALUE;
        intOverflowMap.put("v", intOverflow + 1);
        String sIntOverflow = JSON.toJSONString(intOverflowMap);
        System.out.println("prepare to parse overflow int val: " + sIntOverflow);
        try {
            JSON.parseObject(sIntOverflow, IntegerVal.class);
        } catch (Exception e) {
            System.out.println("We captured the Exception: " + e.getMessage());
        }

        Map<String, Double> floatOverflowMap = new HashMap<>();
        double floatOverflow = Float.MAX_VALUE;
        floatOverflowMap.put("v", floatOverflow + floatOverflow);
        String sFloatOverflow = JSON.toJSONString(floatOverflowMap);
        System.out.println("prepare to parse overflow float val: " + sIntOverflow);
        FloatVal floatVal = JSON.parseObject(sFloatOverflow, FloatVal.class);
        System.out.println("We expect an exception raised, but found it parsed out an invalid value: " + floatVal);
    }

}
