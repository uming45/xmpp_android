package com.juns.wechat.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.juns.wechat.bean.UserBean;
import com.juns.wechat.manager.AccountManager;
import com.juns.wechat.net.request.HttpActionImpl;
import com.style.net.core.NetDataBeanCallback;
import com.juns.wechat.net.response.TokenBean;
import com.juns.wechat.util.SipClient;
import com.juns.wechat.util.SyncDataUtil;
import com.juns.wechat.chat.xmpp.XmppManagerImpl;
import com.juns.wechat.chat.xmpp.XmppManagerUtil;


/*******************************************************
 * Copyright (C) 2014-2015 Yunyun Network <yynetworks@yycube.com>
 * description ：定义一个Service来处理登录及注册Xmpp账户
 *
 * @since 1.6
 * Created by 王宗文 on 2015/11/20
 *******************************************************/
public class XmppService extends Service {
    public static final String TAG = "XmppService";

    private static final long REFRESH_TIME = 6 * 60 * 60 * 1000;

    /**
     * 登录Action,由于登录时这个用户的用户和密码可以从{@link UserBean}中取得，所以不需要传用户名和密码
     */
    public static final String ACTION_LOGIN = "login";
    public static final String ACTION_DESTROY = "destroy";

    private AccountManager userManager;


    @Override
    public void onCreate() {
        super.onCreate();
        userManager = AccountManager.getInstance();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new XmppBinder();
    }

    public class XmppBinder extends Binder{
        public XmppService getService(){
            return XmppService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       handleAction(intent);
        return START_STICKY;
    }

    /**
     * 处理各种action
     * @param intent
     */
    private void handleAction(Intent intent){
        if(intent == null) return;
        String action = intent.getAction();
        if(TextUtils.isEmpty(action)) return;
        if(ACTION_LOGIN.equals(action)){
            login();
        }
    }

    /**
     * 开启一个线程执行登录
     * 更多详情请查看{@link XmppManagerImpl#login(String, String)}
     */
    public void login(){
        if(AccountManager.getInstance().getTokenRefreshTime() + REFRESH_TIME > System.currentTimeMillis()){
           init();
        }else {
            HttpActionImpl.getInstance().refreshToken(TAG, new NetDataBeanCallback<TokenBean>(TokenBean.class) {
                @Override
                protected void onCodeSuccess(TokenBean data) {
                    AccountManager.getInstance().setToken(data.token);
                    init();
                }

                @Override
                protected void onCodeFailure(String msg) {

                }
            });
        }
    }

    private void init(){
        XmppManagerUtil.asyncLogin(userManager.getUserName(), userManager.getUserPassWord());
        SyncDataUtil.getInstance().syncData();
        initSipSession();
    }

    private void initSipSession(){
        //SipClient.getInstance().initSession();
    }

    private void destroySipSession(){
        SipClient.getInstance().destroySession();
    }

    public static void login(Context context){
        Intent service = new Intent(context, XmppService.class);
        service.setAction(ACTION_LOGIN);
        context.startService(service);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        userManager = null;
        destroySipSession();
    }
}
