package com.base.library.base

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.base.library.R
import com.base.library.interfaces.MyXPopupListener
import com.base.library.mvp.BView
import com.base.library.util.getCacheObservable
import com.base.library.util.putCacheObservable
import com.blankj.utilcode.util.LogUtils
import com.gyf.immersionbar.ImmersionBar
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.ConfirmPopupView
import com.lxj.xpopup.impl.LoadingPopupView
import com.lxj.xpopup.interfaces.OnCancelListener
import com.lxj.xpopup.interfaces.OnConfirmListener
import com.lxj.xpopup.interfaces.XPopupCallback
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.base_activity_layout.*
import kotlinx.android.synthetic.main.base_titlebar.*

/**
 * 基础的BaseActivity
 */
abstract class BaseActivity : AppCompatActivity(), BView {

    abstract fun initArgs(intent: Intent?)
    abstract fun initView()
    abstract fun initData()

    private var xPopupLoading: LoadingPopupView? = null
    private var xPopup: ConfirmPopupView? = null

    val mHandler: Handler by lazy { Handler() }
    val mApplication: BApplication by lazy { application as BApplication }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initArgs(intent)
        initView()
        ImmersionBar.with(this).titleBar(bLL).init() // 沉浸式
        window.decorView.post { mHandler.post { initData() } }
    }

    fun initContentView(layoutResID: Int) {
        setContentView(R.layout.base_activity_layout)
        val contentView = LayoutInflater.from(this).inflate(layoutResID, baseLayout, false)
        baseLayout.addView(contentView)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        disDialog()
        mHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun getContext() = this

    override fun showDialog(xPopupCallback: XPopupCallback?, loading: String?) {
        if (xPopupLoading == null) {
            xPopupLoading = XPopup.Builder(this)
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .setPopupCallback(xPopupCallback).asLoading(loading)
        }
        //隐藏状态才显示
        if (xPopupLoading?.isDismiss == true) xPopupLoading?.show()
    }

    override fun showDialog(
        title: String?,
        content: String?,
        cancelBtnText: String?,
        confirmBtnText: String?,
        confirmListener: OnConfirmListener?,
        cancelListener: OnCancelListener?,
        xPopupCallback: XPopupCallback?,
        isHideCancel: Boolean
    ) {
        disDialog()
        xPopup = XPopup.Builder(this).setPopupCallback(xPopupCallback)
            .dismissOnBackPressed(false).dismissOnTouchOutside(false)
            .asConfirm(
                title,
                content,
                cancelBtnText,
                confirmBtnText,
                confirmListener,
                cancelListener,
                isHideCancel
            )
        xPopup?.show()
    }

    // 关闭提示框
    override fun getDismissListener(): MyXPopupListener = object : MyXPopupListener {
        override fun onDis() {
            disDialog()
        }
    }

    // 关闭提示框 并且 销毁页面
    override fun getDismissFinishListener(): MyXPopupListener = object : MyXPopupListener {
        override fun onDis() {
            dismissWith()
        }
    }

    //关闭提示框
    override fun disDialog() {
        xPopup?.dismiss()
        xPopupLoading?.dismiss()
    }

    //关闭提示框之后销毁页面
    override fun dismissWith() {
        xPopupLoading?.dismiss()
        xPopup?.dismissWith { finish() }
    }

    //保存缓存
    override fun putCache(key: String, content: String, time: Int) {
        putCacheObservable(key, content, time)
            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
            .subscribe { LogUtils.d(it) }
    }

    //获取缓存
    override fun getCache(key: String, consumer: Consumer<String>) {
        getCacheObservable(key)
            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
            .subscribe(consumer)
    }

    //P层的数据回调,可以做一些日志保存
    override fun other(content: String, behavior: String, level: String) {
    }

}