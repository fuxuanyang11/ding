package com.ziwenl.self_service.service

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.ziwenl.baselibrary.utils.LibContextProvider
import com.ziwenl.baselibrary.utils.bus.BusConst
import com.ziwenl.baselibrary.utils.bus.EventBusUtil
import com.ziwenl.baselibrary.utils.cache.CacheConst
import com.ziwenl.baselibrary.utils.cache.CacheUtil
import com.ziwenl.self_service.R
import com.ziwenl.self_service.utils.AccessibilityActionUtil
import com.ziwenl.self_service.utils.Callback
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.DataOutputStream
import java.io.IOException


/**
 * PackageName : com.ziwenl.self_service.service
 * Author : Ziwen Lan
 * Date : 2020/7/14
 * Time : 13:59
 * Introduction : 无障碍服务
 */
class SelfAccessibilityService : AccessibilityService() {
    private var mJob: Job? = null

    companion object {
        var selfAccessibilityService: SelfAccessibilityService? = null
        private var mIsPunchOut = false

        fun stop() {
            if (selfAccessibilityService != null) {
                selfAccessibilityService?.stopSelf()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    selfAccessibilityService?.disableSelf()
                }
            }
        }
         fun isEmpty(): Boolean {
            return selfAccessibilityService == null
        }



        /**
         * 解锁
         */
        fun unlock(punchCard: (() -> Unit?)?) {
            if (isEmpty()) {
                Timber.d(
                    LibContextProvider.appContext.resources.getText(R.string.self_service_is_not_activated)
                        .toString()
                )
                return
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Timber.d(
                    LibContextProvider.appContext.resources.getText(R.string.the_current_system_version_does_not_support)
                        .toString()
                )
                return
            }
            //1.向右滑动进入负一屏
            val path = Path()
            val screenHeight = CacheUtil.get(CacheConst.KEY_SCREEN_HEIGHT, 0)
            val screenWidth = CacheUtil.get(CacheConst.KEY_SCREEN_WIDTH, 0)
            val y = screenHeight / 12 * 9f
            path.moveTo(screenWidth / 2f, (screenHeight - 100).toFloat())
            path.lineTo(screenWidth / 2f, (screenHeight - 500).toFloat())
            AccessibilityActionUtil.move(
                selfAccessibilityService!!,
                path,
                1400,
                500,
                object : Callback {
                    override fun onSuccess() {
                        Timber.d("滑动成功")
                        //解锁成功
                        Thread.sleep(3000)
                        punchCard?.let { it() }
//                        Thread.sleep(1500)
                        //2.点击负一屏万能遥控以达到换出密码输入页的目的
//                        if (AccessibilityActionUtil.findAndPerformClickNodeInfo(
//                                selfAccessibilityService!!,
//                                "com.mfashiongallery.emag:id/remote_control",
//                                "",
//                                "万能遥控器"
//                            )
//                        ) {
//                            Thread.sleep(1000)
                            //3.模拟锁屏密码输入完成解锁
//                            val password =
//                                CacheUtil.get(CacheConst.KEY_LOCK_SCREEN_PASSWORD, "")
//                            var isClickPassowrdSuccesss = true
//                            for (s in password) {
//                                if (!AccessibilityActionUtil.findAndPerformClickNodeInfo(
//                                        selfAccessibilityService!!,
//                                        "com.android.systemui:id/digit_text",
//                                        s.toString(),
//                                        ""
//                                    )
//                                ) {
//                                    isClickPassowrdSuccesss = false
//                                    break
//                                }
//                            }
//                            if (isClickPassowrdSuccesss) {
//                                //解锁成功
//                                Thread.sleep(3000)
//                                punchCard?.let { it() }
//                            } else {
//                                Timber.d(
//                                    LibContextProvider.appContext.resources.getText(R.string.simulate_unlock_failure)
//                                        .toString()
//                                )
//                            }
//                        } else {
//                            Timber.d(
//                                LibContextProvider.appContext.resources.getText(R.string.unable_click_universal_remote)
//                                    .toString()
//                            )
//                        }
                    }

                    override fun onError() {
                        Timber.d(
                            LibContextProvider.appContext.resources.getText(R.string.failed_enter_negative_screen)
                                .toString()
                        )
                    }
                })
        }

        /**
         * 锁屏
         */
        fun lockScreen(context: Context, millis: Long) {
            Timber.d("打卡成功,%s后锁屏", millis)
            Thread.sleep(millis)
            val devicePolicyManager =
                context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (devicePolicyManager.isAdminActive(
                    ComponentName(
                        context,
                        LockScreenDeviceAdminReceiver::class.java
                    )
                )
            ) {
                devicePolicyManager.lockNow()
            }
        }

        /**
         * 打下班卡
         */
        fun punchOut() {
            mIsPunchOut = true
        }

        /**
         * 通过指定包名打开APP
         * 钉钉包名 com.alibaba.android.rimet
         * LaunchHomeActivity
         */
        fun openRimet(context: Context, millis: Long): Boolean {
            if (millis != 0L) {
                Thread.sleep(millis)
            }
            val packageManager = context.packageManager
            val pi: PackageInfo?
            val packageName = "com.alibaba.android.rimet"
            pi = try {
                packageManager.getPackageInfo(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.e("未安装钉钉")
                return false
            }
            val resolveIntent = Intent(Intent.ACTION_MAIN, null)
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            if (pi != null) {
                resolveIntent.setPackage(pi.packageName)
            }
            val apps =
                packageManager.queryIntentActivities(resolveIntent, 0)
            val resolveInfo = apps.iterator().next()
            if (resolveInfo != null) {
                val className = resolveInfo.activityInfo.name
                Timber.e("className = %s", className)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val cn = ComponentName(packageName, className)
                intent.component = cn
                context.startActivity(intent)
                CacheUtil.put(CacheConst.KEY_ENABLE_RIMET, true)
                mIsPunchOut = true
                return true
            } else {
                Timber.e("openApp resolveInfo = null")
                return false
            }
        }

        fun openIMT(context: Context, millis: Long): Boolean {
            if (millis != 0L) {
                Thread.sleep(millis)
            }
            val packageManager = context.packageManager
            val pi: PackageInfo?
            val packageName = "com.moutai.mall"
            pi = try {
                packageManager.getPackageInfo(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                Toast.makeText(context, "未安装i茅台", Toast.LENGTH_SHORT).show()
                Timber.e("未安装i茅台")
                return false
            }
            val resolveIntent = Intent(Intent.ACTION_MAIN, null)
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            if (pi != null) {
                resolveIntent.setPackage(pi.packageName)
            }
            val apps =
                packageManager.queryIntentActivities(resolveIntent, 0)
            val resolveInfo = apps.iterator().next()
            if (resolveInfo != null) {
                val className = resolveInfo.activityInfo.name
                Timber.e("className = %s", className)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val cn = ComponentName(packageName, className)
                intent.component = cn
                context.startActivity(intent)
                return true
            } else {
                Timber.e("openApp resolveInfo = null")
                return false
            }
        }

    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        selfAccessibilityService = this
        EventBusUtil.post(BusConst.KEY_ACCESSIBILITY_IS_ENABLE, true)
        PunchCardService.lifeSupport(this)
    }

    override fun onInterrupt() {
        selfAccessibilityService = null
    }

    override fun onDestroy() {
        selfAccessibilityService = null
        EventBusUtil.post(BusConst.KEY_ACCESSIBILITY_IS_ENABLE, false)
        mJob?.cancel()
        super.onDestroy()
    }

    /**
     * 1：已成功点击智能工作助理按钮
     * 2：已成功点击打卡入口按钮
     */
    private var mOpenRimetPageNo = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event!!.packageName.toString()
        val eventType = event.eventType
//        Timber.d("AccessibilityEvent: ${AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED}    eventType = $eventType")
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Timber.d("packageName = $packageName")
            if ("com.alibaba.android.rimet" == packageName && mIsPunchOut) {
                mJob?.cancel()
                mJob = GlobalScope.launch(Dispatchers.Main) {
                    if (mOpenRimetPageNo == 0) {
                        Timber.d("寻找智能工作助理按钮")
                        if (performClickSmartWorkAssistantView()) {
                            mOpenRimetPageNo = 1
                        }
                    } else if (mOpenRimetPageNo == 1) {
                        Timber.d("寻找打卡入口按钮")
                        if (performClickPunchCardView("android.view.View")) {
                            mOpenRimetPageNo = 2
                            val path = Path()
                            val screenHeight = CacheUtil.get(CacheConst.KEY_SCREEN_HEIGHT, 0)
                            val screenWidth = CacheUtil.get(CacheConst.KEY_SCREEN_WIDTH, 0)
                            path.moveTo(screenWidth / 2f, screenHeight / 12 * 7f)
                            path.moveTo(screenWidth / 2f + 50, screenHeight / 12 * 7f + 50)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                AccessibilityActionUtil.move(
                                    this@SelfAccessibilityService,
                                    path,
                                    6000,
                                    200,
                                    object : Callback {
                                        override fun onSuccess() {
                                            Timber.d("打卡成功")
                                            mOpenRimetPageNo = 0
                                            mIsPunchOut = false

                                            Thread.sleep(5000)
                                            clickBackKey()
                                            Thread.sleep(500)
                                            clickBackKey()
                                            Thread.sleep(500)
                                            clickBackKey()
                                            lockScreen(this@SelfAccessibilityService, 4000)
                                        }

                                        override fun onError() {
                                            Timber.d("打卡失败")
                                        }
                                    })
                            }
                        }
                    }
                }
            }
            if ("com.android.systemui" == packageName) {
                //尝试唤醒打卡服务
                PunchCardService.lifeSupport(this)
            }
            if ("com.moutai.mall" == packageName) {
                clickMt()
            }
        }
    }




    fun clickMt() {
        if (performClickSmartWorkAssistantView2()) {
            Timber.d("clickMt======================")
            val path = Path()
            val screenHeight = CacheUtil.get(CacheConst.KEY_SCREEN_HEIGHT, 0)
            val screenWidth = CacheUtil.get(CacheConst.KEY_SCREEN_WIDTH, 0)


            path.moveTo(screenWidth / 2f, (screenHeight - 500).toFloat())
            path.lineTo(screenWidth / 2f, (0).toFloat())
            AccessibilityActionUtil.move(
                selfAccessibilityService!!,
                path,
                1400,
                500,
                object : Callback {
                    override fun onSuccess() {
                        Timber.d("onSuccess======================")
                    }

                    override fun onError() {
                        Timber.d("onError======================")
                    }

                })
        }

    }

    private var mSmartWorkAssistantViewPollingDuration = 0

    fun clickBackKey(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * 点击智能工作助理按钮
     */

    private fun performClickSmartWorkAssistantView(): Boolean {
        val smartWorkAssistantNoteInfo = AccessibilityActionUtil.findNodeInfo(
            this,//
            "com.alibaba.android.rimet:id/session_title",
            "智能工作助理",
            ""
        )

        if (AccessibilityActionUtil.performClickNodeInfo(smartWorkAssistantNoteInfo)) {
            mSmartWorkAssistantViewPollingDuration = 0
            return true
        } else {
            if (mSmartWorkAssistantViewPollingDuration >= 5000) {
                //五秒还没找到就结束
                mSmartWorkAssistantViewPollingDuration = 0
                return false
            } else {
                mSmartWorkAssistantViewPollingDuration += 500
                Thread.sleep(500)
                return performClickSmartWorkAssistantView()
            }
        }
    }

    private var mFindPunchCardViewPollingDuration = 0

    /**
     * 点击打卡按钮
     */
    private fun performClickPunchCardView(noteInfoClassName: String): Boolean {
        //点击打卡按钮实现考勤打卡
        val punchCardBtnNoteInfo =
            AccessibilityActionUtil.findNodeInfoByText(
                this,
                null,
                "打卡",
                noteInfoClassName
            )
        if (AccessibilityActionUtil.performClickNodeInfo(punchCardBtnNoteInfo)) {
            mFindPunchCardViewPollingDuration = 0
            return true
        } else {
            if (mFindPunchCardViewPollingDuration >= 5000) {
                //五秒还没找到就结束
                mFindPunchCardViewPollingDuration = 0
                return false
            } else {
                mFindPunchCardViewPollingDuration += 500
                Thread.sleep(500)
                return performClickPunchCardView(noteInfoClassName)
            }
        }
    }



    private var mSmartWorkAssistantViewPollingDuration2 = 0
    private fun performClickSmartWorkAssistantView2(): Boolean {

        Timber.d("performClickSmartWorkAssistantView2")
        val smartWorkAssistantNoteInfo = AccessibilityActionUtil.findNodeInfo(
            this,
            "com.moutai.mall:id/subtitle",
            "酿造高品质生活",
            ""
        )

        if (smartWorkAssistantNoteInfo != null) {
            Timber.d("performClickSmartWorkAssistantView2======================")
            mSmartWorkAssistantViewPollingDuration2 = 0
            return true
        } else {
            if (mSmartWorkAssistantViewPollingDuration2 >= 5000) {
                //五秒还没找到就结束
                mSmartWorkAssistantViewPollingDuration2 = 0
                return false
            } else {
                mSmartWorkAssistantViewPollingDuration2 += 500
                Thread.sleep(500)
                return performClickSmartWorkAssistantView2()
            }
        }
    }

//    private fun performClickSmartWorkAssistantView2(): Boolean {
//        Timber.d("performClickSmartWorkAssistantView2")
//        val smartWorkAssistantNoteInfo = AccessibilityActionUtil.findNodeInfo(
//            this,
//            "com.moutai.mall:id/subtitle",
//            "酿造高品质生活",
//            ""
//        )
//        smartWorkAssistantNoteInfo.getBoundsInScreen()
//        if (smartWorkAssistantNoteInfo != null) {
//            Timber.d("performClickSmartWorkAssistantView2======================")
//            mSmartWorkAssistantViewPollingDuration2 = 0
//            return true
//        } else {
//            if (mSmartWorkAssistantViewPollingDuration2 >= 5000) {
//                //五秒还没找到就结束
//                mSmartWorkAssistantViewPollingDuration2 = 0
//                return false
//            } else {
//                mSmartWorkAssistantViewPollingDuration2 += 500
//                Thread.sleep(500)
//                return performClickSmartWorkAssistantView2()
//            }
//        }
//    }

}