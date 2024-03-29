package com.love.happiness.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.RemoteException
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.github.arturogutierrez.Badges
import com.github.arturogutierrez.BadgesNotSupportedException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.love.happiness.*
import com.love.happiness._core.BaseActivity
import com.yandex.metrica.YandexMetrica
import com.yandex.metrica.YandexMetricaConfig
import kotlinx.android.synthetic.main.activity_web_view.*
import me.leolin.shortcutbadger.ShortcutBadger


/**
 * Created by Andriy Deputat email(andriy.deputat@gmail.com) on 3/13/19.
 */
class SplashActivity : BaseActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    private lateinit var dataSnapshot: DataSnapshot

    private lateinit var mRefferClient: InstallReferrerClient
    private lateinit var database: DatabaseReference
    val badgeCount = 1

    lateinit var prefs: SharedPreferences

    override fun getContentView(): Int = R.layout.activity_web_view

    override fun initUI() {
        webView = web_view
        progressBar = progress_bar

        prefs = getSharedPreferences("com.love.happiness", Context.MODE_PRIVATE)
    }




    override fun setUI() {
        logEvent("splash-screen")
        val config = YandexMetricaConfig.newConfigBuilder("8411d416-ad43-4789-b318-cceec9487686").build()
        // Initializing the AppMetrica SDK.
        YandexMetrica.activate(applicationContext, config)
        // Automatic tracking of user activity.
        YandexMetrica.enableActivityAutoTracking(this.application)
        webView.webViewClient = object : WebViewClient() {
            /**
             * Check if url contains key words:
             * /money - needed user (launch WebViewActivity or show in browser)
             * /main - bot or unsuitable user (launch ContentActivity)
             */
            @SuppressLint("deprecated")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.contains("/money")) {
                    // task url for web view or browser
//                    val taskUrl = dataSnapshot.child(TASK_URL).value as String
                    val value = dataSnapshot.child(SHOW_IN).value as String
                    var taskUrl = dataSnapshot.child(TASK_URL).value as String

                    taskUrl = prefs.getString("endurl", taskUrl).toString()

                    if (value == WEB_VIEW) {
                            startActivity(
                                    Intent(this@SplashActivity, WebViewActivity::class.java)
                                .putExtra(EXTRA_TASK_URL, taskUrl)
                            )
                        finish()
                    } else if (value == BROWSER) {
                        // launch browser with task url
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("")
                        )

                        logEvent("task-url-browser")
                        startActivity(browserIntent)
                        finish()
                    }
                } else if (url.contains("/main")) {
                    startActivity(Intent(this@SplashActivity, TheStartingActivity::class.java))
                    finish()
                }
                progressBar.visibility = View.GONE
                return false
            }
        }

        progressBar.visibility = View.VISIBLE




        val success = ShortcutBadger.applyCount(this, badgeCount)
        if (!success) {
            startService(
                    Intent(this, BadgeIntentService::class.java).putExtra("badgeCount", badgeCount)
            )
        }

        try {
            Badges.setBadge(this, badgeCount)
        } catch (badgesNotSupportedException: BadgesNotSupportedException) {
            Log.d("SplashActivityBadge", badgesNotSupportedException.message)
        }

        database = FirebaseDatabase.getInstance().reference

        getValuesFromDatabase({
            dataSnapshot = it

            // load needed url to determine if user is suitable
            webView.loadUrl(it.child(SPLASH_URL).value as String)
        }, {
            Log.d("SplashErrActivity", "didn't work fetchremote")
            progressBar.visibility = View.GONE
        })
    }
}