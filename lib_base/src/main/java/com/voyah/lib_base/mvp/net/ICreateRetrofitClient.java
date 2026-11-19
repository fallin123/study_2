package com.voyah.lib_base.mvp.net;

import okhttp3.Interceptor;
import retrofit2.Retrofit;

public interface ICreateRetrofitClient {
    Retrofit createClient(String baseUrl, Interceptor interceptor);
}
