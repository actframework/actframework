package stress_test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.osgl.$;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {

    public static void main(String[] args) throws Exception {
        long ts = $.ms();
        final OkHttpClient http = new OkHttpClient.Builder().build();
        final AtomicInteger backlog = new AtomicInteger(100000);
        final AtomicInteger errors = new AtomicInteger(0);
        final AtomicInteger exceptions = new AtomicInteger(0);
        ExecutorService exec = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 150; ++i) {
            exec.submit(new Runnable() {
                @Override
                public void run() {
                    while (backlog.getAndDecrement() > 0) {
                        String url = "http://localhost:5460/y";
                        //String url = "http://localhost:8080/y";
                        //String url = "http://localhost:5460/y";
                        //String url = "http://localhost:8080/x";
                        Request request = new Request.Builder().url(url).get().build();
                        try {
                            Response resp = http.newCall(request).execute();
//                            String str = resp.body().string();
//                            try {
//                                JSONObject json = JSON.parseObject(str);
//                                if (!json.getString("name").endsWith("b")) {
//                                    errors.incrementAndGet();
//                                }
//                            } catch (Exception e0) {
//                                exceptions.incrementAndGet();
//                            } finally {
//                                resp.close();
//                            }
                        } catch (Exception e) {
                            exceptions.incrementAndGet();
                        }
                    }
                }
            });
        }
        exec.shutdown();
        if (!exec.awaitTermination(1000, TimeUnit.SECONDS)) {
            System.out.println("execution service timeout");
        }
        System.out.printf("errors: %s\n", errors.get());
        System.out.printf("exceptions: %s\n", exceptions.get());
        System.out.printf("It takes %sms to finish\n", $.ms() - ts);
    }

}
