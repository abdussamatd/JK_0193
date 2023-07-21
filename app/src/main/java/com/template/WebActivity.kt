package com.template

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebActivity : AppCompatActivity() {

    companion object {
        const val URL = "url"
        private const val KEY_CURRENT_URL = "currentUrl"
        private const val KEY_COOKIES = "WebActivityCookies"
        private const val COOKIES = "cookies"
    }

    private lateinit var webView: WebView
    private var currentUrl: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        webView = findViewById(R.id.web_view)

        val url = intent.getStringExtra(URL)

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        webView.webChromeClient = WebChromeClient()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return if (request.isForMainFrame) {
                    false
                } else {
                    view.loadUrl(request.url.toString())
                    true
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
            }
        }

        if (savedInstanceState != null) {
            currentUrl = savedInstanceState.getString(KEY_CURRENT_URL)
            webView.restoreState(savedInstanceState)
        } else {
            if (url != null) {
                currentUrl = url
                webView.loadUrl(url)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CURRENT_URL, currentUrl)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentUrl = savedInstanceState.getString(KEY_CURRENT_URL)
    }

    override fun onStop() {
        super.onStop()
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(webView.url)
        val sharedPreferences = getSharedPreferences(KEY_COOKIES, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(COOKIES, cookies)
        editor.apply()
    }

    override fun onStart() {
        super.onStart()
        val sharedPreferences = getSharedPreferences(KEY_COOKIES, MODE_PRIVATE)
        val cookies = sharedPreferences.getString(COOKIES, null)
        if (cookies != null) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setCookie(webView.url, cookies)
            cookieManager.flush()
        }
    }
}