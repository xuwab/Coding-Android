package net.coding.program;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.roughike.bottombar.BottomBar;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.tencent.android.tpush.XGPushManager;
import com.tencent.android.tpush.service.XGPushService;

import net.coding.program.common.Global;
import net.coding.program.common.LoginBackground;
import net.coding.program.common.Unread;
import net.coding.program.common.UnreadNotify;
import net.coding.program.common.htmltext.URLSpanNoUnderline;
import net.coding.program.common.network.MyAsyncHttpClient;
import net.coding.program.common.network.util.Login;
import net.coding.program.common.ui.BaseActivity;
import net.coding.program.event.EventMessage;
import net.coding.program.event.EventNotifyBottomBar;
import net.coding.program.event.EventShowBottom;
import net.coding.program.login.MarketingHelp;
import net.coding.program.login.ZhongQiuGuideActivity;
import net.coding.program.maopao.MainMaopaoFragment_;
import net.coding.program.message.UsersListFragment_;
import net.coding.program.model.AccountInfo;
import net.coding.program.project.MainProjectFragment_;
import net.coding.program.project.ProjectFragment;
import net.coding.program.project.init.InitProUtils;
import net.coding.program.setting.MainSettingFragment_;
import net.coding.program.task.MainTaskFragment_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.DimensionPixelSizeRes;
import org.androidannotations.api.builder.FragmentBuilder;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import network.coding.net.checknetwork.CheckNetworkIntentService;

@EActivity(R.layout.activity_main_parent)
public class MainActivity extends BaseActivity {

    public static final String TAG = "MainActivity";
    public static final String BroadcastPushStyle = "BroadcastPushStyle";
    private static boolean sNeedWarnEmailNoValidLogin = false;
    private static boolean sNeedWarnEmailNoValidRegister = false;
    @Extra
    String mPushUrl;
    @ViewById
    BottomBar bottomBar;
    @ViewById
    ViewGroup container;
    @DimensionPixelSizeRes(R.dimen.main_container_merge_bottom)
    int bottomMerge;

    BroadcastReceiver mUpdatePushReceiver;
    private long exitTime = 0;
    private boolean mKeyboardUp;

    public static void setNeedWarnEmailNoValidLogin() {
        sNeedWarnEmailNoValidLogin = true;
    }

    public static void setNeedWarnEmailNoValidRegister() {
        sNeedWarnEmailNoValidRegister = true;
    }

    private void setListenerToRootView() {
        final View rootView = getWindow().getDecorView().findViewById(R.id.frameLayout);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            // getActionBarHeight() + getStatusBarHeight() + bottomBar();
            final int headerHeight = Global.dpToPx(150);
            int rootViewHeight = rootView.getRootView().getHeight();
            int rootHeight = rootView.getHeight();
            int heightDiff = rootViewHeight - rootHeight;
            if (heightDiff > headerHeight) {
                if (!mKeyboardUp) {
                    mKeyboardUp = true;
                    showBottomBar(!mKeyboardUp);
                }
            } else {
                if (mKeyboardUp) {
                    mKeyboardUp = false;
                    setBottomBar();
                }
            }

        });
    }

    private void setBottomBar() {
        bottomBar.postDelayed(() -> {
            showBottomBar(!mKeyboardUp);
        }, 300);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ZhongQiuGuideActivity.showHolidayGuide(this);

        if (mPushUrl != null) {
            URLSpanNoUnderline.openActivityByUri(this, mPushUrl, true);
            mPushUrl = null;
            getIntent().getExtras().remove("mPushUrl");
        }

        MarketingHelp.showMarketing(this);

        warnMailNoValidLogin();
        warnMailNoValidRegister();


        startExtraService();
        EventBus.getDefault().register(this);

        requestPermission();
    }

    @UiThread(delay = 2000)
    void requestPermission() {
        requestPermissionReal();
    }

    private void requestPermissionReal() {
        RxPermissions permissions = new RxPermissions(this);
        permissions.requestEach(Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(permission -> {
                    if (permission.granted) {
                        startPushService();
                        LoginBackground loginBackground = new LoginBackground(this);
                        loginBackground.update();
                    } else if (permission.shouldShowRequestPermissionRationale) {
                        showDialog("", "开启 \"电话\" 权限后才能收到推送\n开启\"存储空间\"权限是为了能下载文件到外部存储",
                                (dialog, which) -> requestPermissionReal(), null);
                    } else {
                    }
                });
//                .subscribe(granted -> {
//                    if (granted) {
//                        startPushService();
//                        LoginBackground loginBackground = new LoginBackground(this);
//                        loginBackground.update();
//                    } else {
//                        showDialog("", "开启 \"电话\" 权限后才能收到推送\n开启\"存储空间\"权限是为了能下载文件到外部存储",
//                                (dialog, which) -> requestPermissionReal(), null);
//                    }
//                });
    }

    protected void startExtraService() {
        // 检查客户端是否有更新
        Intent intent = new Intent(this, UpdateService.class);
        intent.putExtra(UpdateService.EXTRA_BACKGROUND, true);
        intent.putExtra(UpdateService.EXTRA_WIFI, true);
        intent.putExtra(UpdateService.EXTRA_DEL_OLD_APK, true);
        startService(intent);

        // 检查客户端的网络状况
        startNetworkCheckService();
    }

    private void startPushService() {
        // 信鸽 push 服务会发 broadcast
        mUpdatePushReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateNotifyService();
            }
        };
        IntentFilter intentFilter = new IntentFilter(BroadcastPushStyle);
        registerReceiver(mUpdatePushReceiver, intentFilter);

//        XGPushConfig.enableDebug(this, true);
        // qq push

        if (!MyApp.isDebug()) {
            updateNotifyService();
            pushInXiaomi();
        }
    }

    private void startNetworkCheckService() {
        Intent intent = new Intent(this, CheckNetworkIntentService.class);
        String extra = Global.getExtraString(this);
        intent.putExtra("PARAM_APP", extra);

        intent.putExtra("PARAM_GK", MyApp.sUserObject.global_key);
        String sid = MyAsyncHttpClient.getCookie(this, Global.HOST);
        intent.putExtra("PARAM_COOKIE", sid);

        startService(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateNotifyFromService();
    }

    private void warnMailNoValidLogin() {
        if (sNeedWarnEmailNoValidLogin) {
            sNeedWarnEmailNoValidLogin = false;

            String emailString = MyApp.sUserObject.email;
            boolean emailValid = MyApp.sUserObject.isEmailValidation();
            if (!emailString.isEmpty() && !emailValid) {
                new AlertDialog.Builder(this, R.style.MyAlertDialogStyle)
                        .setTitle("激活邮件")
                        .setMessage(R.string.alert_activity_email2)
                        .setPositiveButton("重发激活邮件", (dialog, which) -> {
                            Login.resendActivityEmail(MainActivity.this);
                        })
                        .setNegativeButton("取消", null)
                        .show();

            }
        }
    }

    private void warnMailNoValidRegister() {
        if (sNeedWarnEmailNoValidRegister) {
            sNeedWarnEmailNoValidRegister = false;

            new AlertDialog.Builder(this, R.style.MyAlertDialogStyle)
                    .setTitle("提示")
                    .setMessage(R.string.alert_activity_email)
                    .setPositiveButton("确定", null)
                    .show();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mUpdatePushReceiver != null) {
            unregisterReceiver(mUpdatePushReceiver);
            mUpdatePushReceiver = null;
        }

        EventBus.getDefault().unregister(this);
    }

    // 信鸽文档推荐调用，防止在小米手机上收不到推送
    private void pushInXiaomi() {
        Context context = getApplicationContext();
        Intent service = new Intent(context, XGPushService.class);
        context.startService(service);
    }

    private void updateNotifyService() {
        boolean needPush = AccountInfo.getNeedPush(this);

        if (needPush) {
            String globalKey = MyApp.sUserObject.global_key;
            XGPushManager.registerPush(this, globalKey);
        } else {
            XGPushManager.registerPush(this, "*");
        }
    }

    @AfterViews
    final void initMainActivity() {
        setActionBarTitle("");

        Global.display(this);

        setListenerToRootView();

        bottomBar.setOnTabSelectListener(tabId -> switchTab(tabId));
    }

    protected void switchTab(int tabId) {
        isOpenDrawerLayout(tabId == R.id.tabTask);
        updateNotifyFromService();
        switch (tabId) {
            case R.id.tabProject:
                switchProject();
                break;

            case R.id.tabTask:
                switchFragment(MainTaskFragment_.FragmentBuilder_.class);
                break;

            case R.id.tabMaopao:
                switchFragment(MainMaopaoFragment_.FragmentBuilder_.class);
                break;

            case R.id.tabMessage:
                switchFragment(UsersListFragment_.FragmentBuilder_.class);
                break;

            case R.id.tabMy:
                switchSetting();
                break;
        }
    }

    protected void switchSetting() {
        switchFragment(MainSettingFragment_.FragmentBuilder_.class);
    }

    protected void switchProject() {
        switchFragment(MainProjectFragment_.FragmentBuilder_.class);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    final protected void switchFragment(Class<?> cls) {
        String tag = cls.getName();
        Fragment showFragment = getSupportFragmentManager().findFragmentByTag(tag);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (showFragment == null) {
            try {
                showFragment = (Fragment) ((FragmentBuilder) cls.newInstance()).build();
                fragmentTransaction.add(R.id.container, showFragment, tag);
            } catch (Exception e) {
                Global.errorLog(e);
            }
        } else {
            fragmentTransaction.show(showFragment);
        }

        List<Fragment> allFragments = getSupportFragmentManager().getFragments();
        if (allFragments != null) {
            for (Fragment item : allFragments) {
                if (item != showFragment) {
                    fragmentTransaction.hide(item);
                }
            }
        }

        fragmentTransaction.commit();
    }

    //当项目设置里删除项目后，重新跳转到主界面，并刷新ProjectFragment
    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getStringExtra("action");
        if (!TextUtils.isEmpty(action) && action.equals(InitProUtils.FLAG_REFRESH)) {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            for (Fragment item : fragments) {
                if (item instanceof ProjectFragment) {
                    if (item.isAdded()) {
                        ((ProjectFragment) item).onRefresh();
                    }
                    break;
                }
            }
        }
        super.onNewIntent(intent);
    }

    // 判断是否打开DrawerLayout
    private void isOpenDrawerLayout(boolean isOpen) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer == null) return;
        if (isOpen) {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        } else {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END);
        } else {
            exitApp();
        }
    }

    private void exitApp() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            showButtomToast(R.string.exit_app);
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventBottomBarNotify(EventNotifyBottomBar notify) {
        updateNotify();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventBottomBar(EventShowBottom showBottom) {
        showBottomBar(showBottom.showBottom);
    }

    private void showBottomBar(boolean show) {
        bottomBar.setVisibility(show ? View.VISIBLE : View.GONE);

        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) container.getLayoutParams();
        lp.bottomMargin = show ? bottomMerge : 0;
        container.setLayoutParams(lp);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventLoginOut(EventMessage eventMessage) {
        if (eventMessage.type == EventMessage.Type.loginOut) {
            finish();
        }
    }

    public void updateNotifyFromService() {
        UnreadNotify.update(this);
    }

    public void updateNotify() {
        Unread unread = MyApp.sUnread;
        bottomBar.getTabWithId(R.id.tabProject).setBadgeCount(unread.getProjectCount() > 0 ? 0 : -1);
        int notifyCount = unread.getNotifyCount();
        if (notifyCount <= 0) {
            notifyCount = -1;
        }
        bottomBar.getTabWithId(R.id.tabMessage).setBadgeCount(notifyCount);
    }
}
