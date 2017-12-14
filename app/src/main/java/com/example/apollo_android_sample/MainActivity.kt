package com.example.apollo_android_sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.cache.http.ApolloHttpCache
import com.apollographql.apollo.cache.http.DiskLruHttpCacheStore
import com.apollographql.apollo.exception.ApolloException
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.suspendCoroutine

class MainActivity : AppCompatActivity() {

    private lateinit var apolloClient: ApolloClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val client = OkHttpClient.Builder().authenticator { _, response ->
            response.request().newBuilder()
                    .addHeader("Authorization", "Bearer <access-token設定して下さい>")
                    .build()
        }.build()

        // Cache
        val size = 1024L * 1024L
        val cacheStore = DiskLruHttpCacheStore(cacheDir, size)

        // Build the Apollo Client
        apolloClient = ApolloClient.builder()
                .serverUrl("https://api.github.com/graphql")
                .httpCache(ApolloHttpCache(cacheStore))
                .okHttpClient(client)
                .build()

        button.setOnClickListener {
            launch(UI) {
                val result = Model.query(apolloClient)
                text_user_name.text = result
            }
        }
    }

    object Model {
        suspend fun query(apolloClient: ApolloClient): String = suspendCoroutine { c ->
            apolloClient.query(ViewerQuery())
                    .httpCachePolicy(HttpCachePolicy.CACHE_FIRST.expireAfter(20, TimeUnit.MINUTES))
                    .enqueue(object : ApolloCall.Callback<ViewerQuery.Data>() {
                        override fun onResponse(response: Response<ViewerQuery.Data>) {
                            c.resume((response.data() as ViewerQuery.Data).viewer.login)
                        }

                        override fun onFailure(e: ApolloException) {
                            c.resume("Error!!")
                            e.printStackTrace()
                        }
                    })
        }
    }
}