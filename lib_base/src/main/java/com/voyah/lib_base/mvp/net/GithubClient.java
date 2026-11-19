package com.voyah.lib_base.mvp.net;


import android.text.TextUtils;

import com.google.gson.Gson;
import com.voyah.lib_base.mvp.base.BaseApplication;
import com.voyah.lib_base.mvp.bean.AesBean;
import com.voyah.lib_base.mvp.utils.AESUtils;
import com.voyah.lib_base.mvp.utils.CrashLogFileUtils;
import com.voyah.lib_base.mvp.utils.DBUtil;
import com.voyah.lib_base.mvp.utils.DeviceInfoModel;
import com.voyah.lib_base.mvp.utils.LogUtil;
import com.voyah.lib_base.mvp.utils.RSAUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Buffer;
import okio.BufferedSource;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class GithubClient implements ICreateRetrofitClient {
    private static final boolean showLog = true;
    private static final String TAG = "HttpsClient";
    Interceptor appInterceptor1 = new Interceptor() {

        @NotNull
        @Override
        public Response intercept(@NotNull Chain chain) throws IOException {
            Request request = chain.request();
            String url = request.url().toString();
            if (!url.contains("sign/get")) {
                //加密
                RequestBody requestBody = request.body();
                Buffer requestBuffer = new Buffer();
                if (null != requestBody) {
                    requestBody.writeTo(requestBuffer);
                }
                String oldBodyStr = requestBuffer.readUtf8();
                requestBuffer.close();
                if (!TextUtils.isEmpty(oldBodyStr)) {
                    if (showLog) {
                        LogUtil.e(TAG, "请求的url: " + request.url() + "\n请求参数：" + oldBodyStr);
                    }
                    try {
                        oldBodyStr = oldBodyStr + "&" + System.currentTimeMillis();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
//                String newBodyStr = AESUtils.encrypt(oldBodyStr, DBUtil.AES_PRIVATE);
//
//                Map<String, String> map = new HashMap<>();
//                map.put("req", newBodyStr);
//                String requestJson = String.valueOf(new JSONObject(map));
                RequestBody newBody = RequestBody.create(MediaType.parse("application/json;charset=UTF-8"), oldBodyStr);
//                String sign_encrypt = "";
//                String sign = DBUtil.getSginName();
                try {
//                    PublicKey rsaPublicKey = RSAUtils.string2PublicKey(DBUtil.RES_PUBLIC);
//                    sign_encrypt = RSAUtils.byte2Base64(RSAUtils.publicEncrypt(sign.getBytes(), rsaPublicKey));
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                if (TextUtils.isEmpty(oldBodyStr)) {
//                    request = request.newBuilder()
//                            .addHeader("Content-Type", "text/plain")
////                            .addHeader("sign_encrypt", sign_encrypt.trim())
//                            .addHeader("userToken", DBUtil.getUserToken())
//                            .addHeader("platform", "android")
//                            .addHeader("version", BaseApplication.getApplicationInstance().getVersionCodeTime(2))
//                            .addHeader("systemVersion", "" + android.os.Build.VERSION.SDK_INT)
//                            .addHeader("deviceType", DeviceInfoModel.getInstance().getPhoneModel())
//                            .addHeader("osversion", DeviceInfoModel.getInstance().getOS())
//                            .addHeader("device", "app")
//                            .addHeader("deviceSn", BaseApplication.deviceSN)
//                            .build();
//                } else {
                    request = request.newBuilder()
                            .method(request.method(), newBody)
                            .addHeader("Content-Type", "text/plain")
//                            .addHeader("sign_encrypt", sign_encrypt.trim())
                            .addHeader("Authorization", DBUtil.getUserToken())
                            .addHeader("platform", "android")
                            .addHeader("version", BaseApplication.getApplicationInstance().getVersionCodeTime(2))
                            .addHeader("systemVersion", "" + android.os.Build.VERSION.SDK_INT)
                            .addHeader("deviceType", DeviceInfoModel.getInstance().getPhoneModel())
                            .addHeader("osversion", DeviceInfoModel.getInstance().getOS())
                            .addHeader("device", "app")
                            .addHeader("deviceSn", BaseApplication.deviceSN)
                            .build();
//                }
            } else {
                request = request.newBuilder()
                        .addHeader("version", BaseApplication.getApplicationInstance().getVersionCodeTime(2))
                        .build();
            }
            if (showLog) {
                StringBuilder sb = new StringBuilder();
                sb.append("请求的url: " + url).append("\n");
                sb.append("请求的token: " + DBUtil.getUserToken()).append("\n");
                sb.append("############# 请求头部信息:#############").append("\n");
                for (String header : request.headers().names()) {
                    sb.append(header + ": " + request.header(header)).append("\n");
                }
                sb.append("#############################################").append("\n");
                LogUtil.e(TAG, sb.toString());
            }

            //响应
            Response response = chain.proceed(request);

            if (!url.contains("sign/get")) {
                if (response.code() == 200 || response.code() == 400) {
                    //解密
                    ResponseBody oldResponseBody = response.body();
                    String oldResponseBodyStr = oldResponseBody.string();
                    MediaType mediaType = oldResponseBody.contentType();
                    //                oldResponseBody.close();
                    //System.out.println("请求内容：返回：" + oldResponseBodyStr);
//                    String newResponseBodyStr = null;
                    try {
//                        AesBean bean = new Gson().fromJson(oldResponseBodyStr, AesBean.class);
//                        newResponseBodyStr = AESUtils.decrypt(bean.getResp(), DBUtil.AES_PRIVATE);
//                        newResponseBodyStr = bean.getResp();
                        try {
                            if (showLog) {
//                                LogUtil.e(TAG, "请求url: " + response.request().url() + "\n请求返回：解密后" + oldResponseBodyStr);
                                LogUtil.e(TAG, "请求url: " + response.request().url() + "\n请求返回：" + oldResponseBodyStr);
                            }
                        } catch (Exception e) {
                            LogUtil.e(TAG, "请求url: " + response.request().url() + "\n数据量太大");
                        }
                        ResponseBody newResponseBody = ResponseBody.create(mediaType, oldResponseBodyStr);
                        response = response.newBuilder().body(newResponseBody).build();
                        //异常接口
                        JSONObject jsonObject = new JSONObject(oldResponseBodyStr);
                        jsonObject.get("code");
                        String code = jsonObject.getString("code");
                        if (!String.valueOf(code).equals("0")) {
                            String content = "异常接口:" + url + "\n" + oldResponseBodyStr;
                            CrashLogFileUtils.INSTANCE.writerNetworkCrashInfo(content);
                        }

                    } catch (Exception e) {
                        try {
                            JSONObject jsonObject = new JSONObject(oldResponseBodyStr);
                            jsonObject.get("code");
                            if (showLog) {
                                LogUtil.e(TAG, "请求返回：异常请求url: " + response.request().url() + "\n请求返回：异常请求code：" + jsonObject.get("code") + "===" + jsonObject.get("msg"));
                            }
                            String code = jsonObject.getString("code");
                            //if (code.equals("-9998")) {
                            ResponseBody newResponseBody = ResponseBody.create(mediaType, oldResponseBodyStr);
                            response = response.newBuilder().body(newResponseBody).build();
                            //}
                        } catch (JSONException jsonException) {

                        }

                    }

                } else {
                    CrashLogFileUtils.INSTANCE.writerNetworkCrashInfo("url:" + url + "\nresponse:" + response.code());
                }
            }
//            Charset UTF8 = StandardCharsets.UTF_8;
//            BufferedSource source = response.body().source();
//            source.request(Long.MAX_VALUE);
//            Charset charset = UTF8;
//            MediaType contentType = response.body().contentType();
//            if (contentType != null) {
//                try {
//                    charset = contentType.charset(UTF8);
//                } catch (UnsupportedCharsetException e) {
//                    e.printStackTrace();
//                }
//            }
            return response;
        }

    };

    @Override
    public Retrofit createClient(String baseUrl, Interceptor appInterceptor) {
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(40, TimeUnit.SECONDS);
            builder.readTimeout(60, TimeUnit.SECONDS);
            builder.writeTimeout(60, TimeUnit.SECONDS);
            //   builder.addInterceptor(new NetworkIntercepter());
            builder.addInterceptor(appInterceptor);
            builder.addInterceptor(appInterceptor1);
            if (showLog) {
                HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor();
                logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                builder.addInterceptor(logInterceptor);
            }
            //        builder.retryOnConnectionFailure(true);
            File dir = new File("cache");
            long size = 1024 * 1024;
            Cache cache = new Cache(dir, size);
            // 缓存设置为 100Mb
            builder.cache(cache);
            OkHttpClient mApiClient = builder.build();
            Retrofit retrofit = new Retrofit.Builder()
                    .client(mApiClient)
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                    .build();
            return retrofit;
        } catch (Exception e) {
            Retrofit retrofit = new Retrofit.Builder()
                    //                    .client(mApiClient)
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                    .build();
            return retrofit;
        }
    }

    public class NetworkIntercepter implements Interceptor {
        @Override
        public Response intercept(Chain chain) {
            long start = System.currentTimeMillis();
            Response response = null;
            String responseBody = null;
            String responseCode = null;
            String url = null;
            String requestBody = null;
            try {
                Request request = chain.request();
                url = request.url().toString();
                requestBody = getRequestBody(request);
                response = chain.proceed(request);
                responseBody = response.body().string();
                responseCode = String.valueOf(response.code());
                MediaType mediaType = response.body().contentType();
                response = response.newBuilder().body(ResponseBody.create(mediaType, responseBody)).build();

            } catch (Exception e) {

            } finally {
                long end = System.currentTimeMillis();
                String duration = String.valueOf(end - start);
                LogUtil.e(TAG, url + "\n" + requestBody);

            }

            return response;
        }

        private String getRequestBody(Request request) {
            String requestContent = "";
            if (request == null) {
                return requestContent;
            }
            RequestBody requestBody = request.body();
            if (requestBody == null) {
                return requestContent;
            }
            try {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);
                Charset charset = StandardCharsets.UTF_8;
                requestContent = buffer.readString(charset);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return requestContent;
        }
    }
}
