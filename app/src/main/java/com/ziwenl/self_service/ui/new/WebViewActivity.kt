package com.ziwenl.self_service.ui.new

import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.FragmentActivity
import com.ziwenl.baselibrary.base.view.activity.BaseActivity
import com.ziwenl.self_service.R
import com.ziwenl.self_service.ui.setting.SettingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.webview_activity.*

/**
 * Created by yang on 2022/11/14
 */
class WebViewActivity: BaseActivity() {
    companion object {
        fun launch(activity: FragmentActivity) =
            activity.apply {
                startActivity(Intent(this, WebViewActivity::class.java))
            }
    }
    override val layoutId: Int = R.layout.webview_activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.main_activity)

        webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                view?.loadUrl(request?.url.toString())
                return true
            }
        }
        webview.loadUrl("www.pornhub.com")
    }
}