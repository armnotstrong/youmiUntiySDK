package com.wawagame.app.youmiad;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import net.youmi.android.AdManager;
import net.youmi.android.listener.Interface_ActivityListener;
import net.youmi.android.listener.OffersWallDialogListener;
import net.youmi.android.normal.banner.BannerManager;
import net.youmi.android.normal.banner.BannerViewListener;
import net.youmi.android.normal.common.ErrorCode;
import net.youmi.android.normal.spot.SpotListener;
import net.youmi.android.normal.spot.SpotManager;
import net.youmi.android.normal.video.VideoAdListener;
import net.youmi.android.normal.video.VideoAdManager;
import net.youmi.android.normal.video.VideoAdSettings;
import net.youmi.android.offers.OffersManager;
import net.youmi.android.offers.PointsChangeNotify;
import net.youmi.android.offers.PointsManager;

public class MainActivity extends UnityPlayerActivity implements PointsChangeNotify {
	
	// 以下常量为各种功能的标识，值随意起
	// ------------------------------------------
	// 无积分广告
	private final static int SHOW_SPOT_AD = 100;
	
	private final static int HIDE_SPOT_AD = 101;
	
	private final static int SHOW_VIDEO_AD = 102;
	
	private final static int SHOW_BANNER_AD = 103;
	
	private final static int HIDE_BANNER_AD = 104;
	
	// ------------------------------------------
	// 积分墙
	private final static int SHOW_OFFER_WALL = 200;
	
	private final static int SHOW_OFFER_WALL_DIALOG = 201;
	
	private Context mContext;
	
	private PermissionHelper mPermissionHelper;

    protected final String mSyncGameObject= "youmiADS";
    protected final Boolean mIsDebug = true;
	/**
	 * 广告条视图
	 */
	private View mBannerView;
	
	private static Handler sHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		
		// 当系统为6.0以上时，需要申请权限
		mPermissionHelper = new PermissionHelper(this);
		mPermissionHelper.setOnApplyPermissionListener(new PermissionHelper.OnApplyPermissionListener() {
			@Override
			public void onAfterApplyAllPermission() {
				runAppLogic();
			}
		});
		if (Build.VERSION.SDK_INT < 23) {
			// 如果系统版本低于23，直接跑应用的逻辑
			runAppLogic();
		} else {
			// 如果权限全部申请了，那就直接跑应用逻辑
			if (mPermissionHelper.isAllRequestedPermissionGranted()) {
				runAppLogic();
			} else {
				// 如果还有权限为申请，而且系统版本大于23，执行申请权限逻辑
				mPermissionHelper.applyPermissions();
			}
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		mPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mPermissionHelper.onActivityResult(requestCode, resultCode, data);
	}
	
	/**
	 * 运行应用逻辑
	 */
	private void runAppLogic() {
		// 初始化SDK
		initSDK();
		// 设置插屏广告
		setupSpotAd();
		// 设置视频广告
		setupVideoAd();
		// 初始化积分墙
		initOfferWall();
		
		initHandler();
	}
	
	/**
	 * 初始化SDK
	 */
	private void initSDK() {
        String youmiAppid = null;
        String youmiAppSecret = null;

        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            youmiAppid = bundle.getString("youmiAppid");
            youmiAppSecret = bundle.getString("youmiAppSecret");
        }catch (Exception e){
            Log.e("youmi", "unable to load meta-data " + e.getMessage());
        }
		// 初始化接口，应用启动的时候调用，参数：appId, appSecret, 是否开启调试模式, 是否开启有米日志
        AdManager.getInstance(mContext).init(youmiAppid, youmiAppSecret, true, true);
//        AdManager.getInstance(mContext).init("701531e6440b8ce1", "2ef8b6be3a315520", true, true);
	}
	
	/**
	 * 设置插屏广告
	 */
	private void setupSpotAd() {
		// 设置插屏图片类型，默认竖图
		//		// 横图
		//		SpotManager.getInstance(mContext).setImageType(SpotManager
		// .IMAGE_TYPE_HORIZONTAL);
		// 竖图
		SpotManager.getInstance(mContext).setImageType(SpotManager.IMAGE_TYPE_VERTICAL);
		// 设置动画类型，默认高级动画
		//		// 无动画
		//		SpotManager.getInstance(mContext).setAnimationType(SpotManager
		// .ANIMATION_TYPE_NONE);
		//		// 简单动画
		//		SpotManager.getInstance(mContext).setAnimationType(SpotManager
		// .ANIMATION_TYPE_SIMPLE);
		// 高级动画
		SpotManager.getInstance(mContext)
				.setAnimationType(SpotManager.ANIMATION_TYPE_ADVANCED);
	}
	
	/**
	 * 展示插屏广告
	 */
	private void internalShowSpotAd() {
		SpotManager.getInstance(mContext).showSpot(mContext, new SpotListener() {
			@Override
			public void onShowSuccess() {
                UnityPlayer.UnitySendMessage(mSyncGameObject,"OnShowSpotAd","Success");
                showToastOnUiThread("插屏展示成功", Toast.LENGTH_SHORT);
			}
			
			@Override
			public void onShowFailed(int errorCode) {
                UnityPlayer.UnitySendMessage(mSyncGameObject,"OnShowSpotAdFailed",String.valueOf(errorCode));
				switch (errorCode) {
				case ErrorCode.NON_NETWORK:
					showToastOnUiThread("插屏展示失败 - 网络异常", Toast.LENGTH_LONG);
					break;
				case ErrorCode.NON_AD:
					showToastOnUiThread("插屏展示失败 - 暂无插屏广告", Toast.LENGTH_LONG);
					break;
				case ErrorCode.RESOURCE_NOT_READY:
					showToastOnUiThread("插屏展示失败 - 插屏资源还没准备好", Toast.LENGTH_LONG);
					break;
				case ErrorCode.SHOW_INTERVAL_LIMITED:
					showToastOnUiThread("插屏展示失败 - 请勿频繁展示", Toast.LENGTH_LONG);
					break;
				case ErrorCode.WIDGET_NOT_IN_VISIBILITY_STATE:
					showToastOnUiThread("插屏展示失败 - 请设置插屏为可见状态", Toast.LENGTH_LONG);
					break;
				default:
					showToastOnUiThread("插屏展示失败 - 请稍后再试", Toast.LENGTH_LONG);
					break;
				}
			}
			
			@Override
			public void onSpotClosed() {
                UnityPlayer.UnitySendMessage(mSyncGameObject,"OnShowSpotAd","Closed");
				showToastOnUiThread("插屏被关闭", Toast.LENGTH_SHORT);
			}
			
			@Override
			public void onSpotClicked(boolean isWebPage) {
                UnityPlayer.UnitySendMessage(mSyncGameObject,"OnShowSpotAd","Clicked");
				showToastOnUiThread("插屏被点击", Toast.LENGTH_SHORT);
			}
		});
	}
	
	/**
	 * 隐藏插屏广告
	 */
	private void internalHideSpotAd() {
		SpotManager.getInstance(mContext).hideSpot();
	}
	
	/**
	 * 设置视频广告
	 */
	private void setupVideoAd() {
		// 设置服务器回调 userId，一定要在请求广告之前设置，否则无效
		VideoAdManager.getInstance(mContext).setUserId("userId");
		// 请求视频广告
		VideoAdManager.getInstance(mContext).requestVideoAd(mContext);
	}
	
	/**
	 * 展示视频广告
	 */
	private void internalShowVideoAd() {
		// 设置视频广告
		final VideoAdSettings videoAdSettings = new VideoAdSettings();
		videoAdSettings.setInterruptTips("退出视频播放将无法获得奖励" + "\n确定要退出吗？");
		
		VideoAdManager.getInstance(mContext)
				.showVideoAd(mContext, videoAdSettings, new VideoAdListener() {
					@Override
					public void onPlayStarted() {

                        UnityPlayer.UnitySendMessage(mSyncGameObject, "OnShowVideo","PlayStarted");
						showToastOnUiThread("开始播放视频", Toast.LENGTH_SHORT);
					}
					
					@Override
					public void onPlayInterrupted() {
                        UnityPlayer.UnitySendMessage(mSyncGameObject, "OnShowVideo","Interrupted");
						showToastOnUiThread("播放视频被中断", Toast.LENGTH_LONG);
					}
					
					@Override
					public void onPlayFailed(int errorCode) {
                        UnityPlayer.UnitySendMessage(mSyncGameObject, "OnShowVideoFailed",String.valueOf(errorCode));
						switch (errorCode) {
						case ErrorCode.NON_NETWORK:
							showToastOnUiThread("视频播放失败 - 网络异常", Toast.LENGTH_LONG);
							break;
						case ErrorCode.NON_AD:
							showToastOnUiThread("视频播放失败 - 视频暂无广告", Toast.LENGTH_LONG);
							break;
						case ErrorCode.RESOURCE_NOT_READY:
							showToastOnUiThread("视频播放失败 - 视频资源还没准备好", Toast.LENGTH_LONG);
							break;
						case ErrorCode.SHOW_INTERVAL_LIMITED:
							showToastOnUiThread("视频播放失败 - 视频展示间隔限制", Toast.LENGTH_LONG);
							break;
						case ErrorCode.WIDGET_NOT_IN_VISIBILITY_STATE:
							showToastOnUiThread("视频播放失败 - 视频控件处在不可见状态", Toast.LENGTH_LONG);
							break;
						default:
							showToastOnUiThread("视频播放失败 - 请稍后再试", Toast.LENGTH_LONG);
							break;
						}
					}
					
					@Override
					public void onPlayCompleted() {
                        UnityPlayer.UnitySendMessage(mSyncGameObject, "OnShowVideo","PlayCompleted");
						showToastOnUiThread("视频播放成功", Toast.LENGTH_SHORT);
					}
				});
	}
	
	/**
	 * 展示广告条
	 */
	private void internalShowBannerAd() {
		if (mBannerView == null) {
			// 获取广告条
			mBannerView = BannerManager.getInstance(mContext)
					.getBannerView(mContext, new BannerViewListener() {
						@Override
						public void onRequestSuccess() {
                            UnityPlayer.UnitySendMessage(mSyncGameObject, "OnShowBanner","Success");
							showToastOnUiThread("请求广告条成功", Toast.LENGTH_SHORT);
						}
						
						@Override
						public void onSwitchBanner() {
                            UnityPlayer.UnitySendMessage(mSyncGameObject, "OnShowBanner","Switch");
							showToastOnUiThread("广告条切换", Toast.LENGTH_SHORT);
						}
						
						@Override
						public void onRequestFailed() {
                            UnityPlayer.UnitySendMessage(mSyncGameObject, "OnShowBanner","Failed");
							showToastOnUiThread("请求广告条失败", Toast.LENGTH_LONG);
						}
					});
			// 使用WindowManager来展示广告条
			WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
			WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
			layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
			layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
			layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
			layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
			layoutParams.alpha = 1.0F;
			layoutParams.format = 1;
			layoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT; // 这里示例为：在右下角展示广告条
			windowManager.addView(mBannerView, layoutParams);
		}
	}
	
	/**
	 * 隐藏广告条
	 */
	private void internalHideBannerAd() {
		if (mBannerView != null) {
			((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).removeView(mBannerView);
			mBannerView = null;
		}
	}
	
	/**
	 * 初始化积分墙
	 */
	private void initOfferWall() {
		// 如果使用积分广告，请务必调用积分广告的初始化接口:
		OffersManager.getInstance(mContext).onAppLaunch();
		// (可选)注册积分监听-随时随地获得积分的变动情况
		PointsManager.getInstance(mContext).registerNotify(this);
	}
	
	/**
	 * 展示全屏积分墙
	 */
	private void internalShowOfferWall() {
		// 展示积分墙全屏对话框
		OffersManager.getInstance(mContext).showOffersWall(new Interface_ActivityListener() {
			
			@Override
			public void onActivityDestroy(Context context) {
				showToastOnUiThread("全屏积分墙退出了", Toast.LENGTH_LONG);
			}
		});
	}
	
	private void internalShowOfferWallDialog() {
		OffersManager.getInstance(mContext).showOffersWallDialog(this, new OffersWallDialogListener() {
			@Override
			public void onDialogClose() {
				showToastOnUiThread("积分墙对话框关闭了", Toast.LENGTH_LONG);
			}
		});
	}
	
	private void initHandler() {
		sHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case SHOW_SPOT_AD:
					internalShowSpotAd();
					break;
				case HIDE_SPOT_AD:
					internalHideSpotAd();
					break;
				case SHOW_VIDEO_AD:
					internalShowVideoAd();
					break;
				case SHOW_BANNER_AD:
					internalShowBannerAd();
					break;
				case HIDE_BANNER_AD:
					internalHideBannerAd();
					break;
				case SHOW_OFFER_WALL:
					internalShowOfferWall();
					break;
				case SHOW_OFFER_WALL_DIALOG:
					internalShowOfferWallDialog();
					break;
				default:
					break;
				}
			}
		};
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		// 插屏广告
		SpotManager.getInstance(mContext).onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		// 插屏广告
		SpotManager.getInstance(mContext).onStop();
	}
	
	/**
	 * 展示插屏广告
	 */
	public void showSpotAd() {
		sendMsgToHandler(SHOW_SPOT_AD);
	}
	
	/**
	 * 隐藏插屏广告
	 */
	public boolean hideSpotAd() {
		if (SpotManager.getInstance(mContext).isSpotShowing()) {
			sendMsgToHandler(HIDE_SPOT_AD);
			return true;
		}
		return false;
	}
	
	/**
	 * 展示视频广告
	 */
	public void showVideoAd() {
		sendMsgToHandler(SHOW_VIDEO_AD);
	}
	
	/**
	 * 展示广告条
	 */
	public void showBannerAd() {
		sendMsgToHandler(SHOW_BANNER_AD);
	}
	
	/**
	 * 隐藏广告条
	 */
	public void hideBannerAd() {
		sendMsgToHandler(HIDE_BANNER_AD);
	}
	
	/**
	 * 展示全屏积分墙
	 */
	public void showOfferWall() {
		sendMsgToHandler(SHOW_OFFER_WALL);
	}
	
	/**
	 * 展示对话框积分墙
	 */
	public void showOfferWallDialog() {
		sendMsgToHandler(SHOW_OFFER_WALL_DIALOG);
	}
	
	/**
	 * 查询积分
	 *
	 * @return 整形积分值
	 */
	public float queryPoints() {
		return PointsManager.getInstance(mContext).queryPoints();
	}
	
	/**
	 * 奖励积分
	 *
	 * @param amount 奖励的积分
	 *
	 * @return 操作是否成功
	 */
	public boolean awardPoints(float amount) {
		return PointsManager.getInstance(mContext).awardPoints(amount);
	}
	
	/**
	 * 消耗积分
	 *
	 * @param amount 消耗的积分
	 *
	 * @return 操作是否成功
	 */
	public boolean spendPoints(float amount) {
		return PointsManager.getInstance(mContext).spendPoints(amount);
	}

	/**
	 * 退出应用
	 */
	public void exitApp() {
		// 插屏广告
		SpotManager.getInstance(mContext).onDestroy();
		// 展示广告条窗口的 onDestroy() 回调方法中调用
		BannerManager.getInstance(mContext).onDestroy();

		// 退出应用时调用，用于释放资源
		// 如果无法保证应用主界面的 onDestroy() 方法被执行到，请移动以下接口到应用的退出逻辑里面调用

		// 插屏广告（包括普通插屏广告、轮播插屏广告、原生插屏广告）
		SpotManager.getInstance(mContext).onAppExit();
		// 视频广告（包括普通视频广告、原生视频广告）
		VideoAdManager.getInstance(mContext).onAppExit();

		// 回收积分广告占用的资源
		OffersManager.getInstance(mContext).onAppExit();
	}
	
	/**
	 * 积分余额变动通知
	 *
	 * @param pointsBalance 当前积分余额
	 */
	@Override
	public void onPointBalanceChange(float pointsBalance) {
		showToastOnUiThread("积分余额发生变动了，当前积分：" + pointsBalance, Toast.LENGTH_SHORT);
		
		// 当积分余额变动时，通知unity3d进行界面更新，
		// 参数1:发送游戏对象的名称
		// 参数2:对象绑定的脚本接收该消息的方法
		// 参数3:本条消息发送的字符串信息
		UnityPlayer.UnitySendMessage("Main Camera", "UpdatePoints", String.valueOf(pointsBalance));
	}
	
	private synchronized void sendMsgToHandler(int type) {
		Message msg = sHandler.obtainMessage();
		msg.what = type;
		msg.sendToTarget();
	}
	
	public void showToastOnUiThread(final String str, final int duration) {
        Log.e("video","str:"+str);
        if(!mIsDebug){
            return;
        }
		if (Looper.myLooper() == Looper.getMainLooper()) {
			Toast.makeText(mContext, str, duration).show();
		} else {
			sHandler.post(new Runnable() {
				
				@Override
				public void run() {
					Toast.makeText(mContext, str, duration).show();
				}
			});
		}
	}
}