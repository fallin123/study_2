package com.voyah.lib_base.mvp.net;


import android.util.Log;


import com.voyah.lib_base.mvp.app.XXInitProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class HttpManager {

    private static final boolean showLog = true;
    private static final boolean useUnsafeHttps = true;
    private static final HashMap<String, Object> SERVICE_MAP = new HashMap<>();
    public static String[] sslCertPath;
    private static final Converter.Factory fastJsonConverterFactory = GsonConverterFactory.create();
    private static final CallAdapter.Factory rxJavaCallAdapterFactory = RxJava3CallAdapterFactory.create();

    private static final int DEFAULT_TIMEOUT = 60;

    private static final int DEFAULT_UPLOAD_TIMEOUT = 60;

    private static <T> T createService(Class<T> serviceClass, String baseUrl) {
        Retrofit mRetrofitClient = generateRetrofitClient(baseUrl);
        return mRetrofitClient.create(serviceClass);
    }

    public synchronized static <T> T getService(Class<T> serviceClass, String baseUrl) {
        //缓存
        if (SERVICE_MAP.containsKey(serviceClass.getName())) {
            return (T) SERVICE_MAP.get(serviceClass.getName());
        } else {
            Object obj = createService(serviceClass, baseUrl);
            SERVICE_MAP.put(serviceClass.getName(), obj);
            return (T) obj;
        }
    }

    public synchronized static <T> T getService(Class<T> serviceClass, String baseUrl, Interceptor appInterceptor, Class<? extends ICreateRetrofitClient> createRetrofitClientClass) {
        if (createRetrofitClientClass == null) {
            return getService(serviceClass, baseUrl);
        }
        if (SERVICE_MAP.containsKey(serviceClass.getName())) {
            return (T) SERVICE_MAP.get(serviceClass.getName());
        } else {
            try {
                ICreateRetrofitClient iCreateRetrofitClient = createRetrofitClientClass.newInstance();
                Object obj = iCreateRetrofitClient.createClient(baseUrl, appInterceptor).create(serviceClass);
                SERVICE_MAP.put(serviceClass.getName(), obj);
                return (T) obj;
            } catch (InstantiationException e) {
                e.printStackTrace();
                return getService(serviceClass, baseUrl);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return getService(serviceClass, baseUrl);
            }
        }
    }

    public synchronized static <T> T getFileService(Class<T> serviceClass, String baseUrl, Interceptor appInterceptor, Class<? extends ICreateRetrofitClient> createRetrofitClientClass) {
        if (createRetrofitClientClass == null) {
            return getService(serviceClass, baseUrl);
        }
        try {
            ICreateRetrofitClient iCreateRetrofitClient = createRetrofitClientClass.newInstance();
            Object obj = iCreateRetrofitClient.createClient(baseUrl, appInterceptor).create(serviceClass);
            SERVICE_MAP.put(serviceClass.getName(), obj);
            return (T) obj;
        } catch (InstantiationException e) {
            e.printStackTrace();
            return getService(serviceClass, baseUrl);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return getService(serviceClass, baseUrl);
        }
    }

    public static Retrofit generateRetrofitClient(String baseUrl) {
        //Client配置
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (useUnsafeHttps) {
            builder.sslSocketFactory(getSSLSocketFactory(),new X509TrustManager() {

                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {}

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {}

                // Called reflectively by X509TrustManagerExtensions.
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] chain, String authType, String host) {}

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[] {};
                }
            });
            builder.hostnameVerifier(getHostnameVerifier());
        } else if (sslCertPath != null) {
            builder.sslSocketFactory(getCertificateFactory(sslCertPath),new X509TrustManager() {

                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {}

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {}

                // Called reflectively by X509TrustManagerExtensions.
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] chain, String authType, String host) {}

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[] {};
                }
            });
        }
        builder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        builder.readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        builder.writeTimeout(DEFAULT_UPLOAD_TIMEOUT, TimeUnit.SECONDS);
        if (showLog) {
            HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor(new LoggingInterceptor());
            logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logInterceptor);
        }
        builder.retryOnConnectionFailure(true);
        // TODO: 2020/8/12 需要context
        builder.cache(new Cache(new File(XXInitProvider.getAppContext().getCacheDir(), ""), 1024 * 1024 * 100));
        OkHttpClient mApiClient = builder.build();

        return new Retrofit.Builder()
                .client(mApiClient)
                .baseUrl(baseUrl)
                .addConverterFactory(fastJsonConverterFactory)
                .addCallAdapterFactory(rxJavaCallAdapterFactory)
                .build();
    }


    private static class LoggingInterceptor implements HttpLoggingInterceptor.Logger {
        @Override
        public void log(String message) {
            //显示网络请求log
            Log.d("dddd", message);
        }
    }

    public static SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, getTrustManager(), new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TrustManager[] getTrustManager() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };
        return trustAllCerts;
    }

    private static HostnameVerifier getHostnameVerifier() {
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        };
        return hostnameVerifier;
    }

    private static KeyManager[] prepareKeyManager(InputStream bksFile, String password) {
        try {
            if (bksFile == null || password == null) {
                return null;
            }

            KeyStore clientKeyStore = KeyStore.getInstance("BKS");
            clientKeyStore.load(bksFile, password.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(clientKeyStore, password.toCharArray());
            return keyManagerFactory.getKeyManagers();

        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SSLSocketFactory getCertificateFactory(String... certificatePaths) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            for (int i = 0; i < certificatePaths.length; i++) {
                String certificateAlias = Integer.toString(i + 1);
                String certificatePath = certificatePaths[i];
                InputStream certificate = XXInitProvider.getAppContext().getAssets().open(certificatePath);
                keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));
                try {
                    if (certificate != null) {
                        certificate.close();
                    }
                } catch (IOException e) {
                }
            }
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            sslContext.init(null,
                    trustManagerFactory.getTrustManagers(), new SecureRandom());
            return (sslContext.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
