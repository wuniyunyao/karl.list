package cn.com.karl.list;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AbsListView.OnScrollListener;
//import android.widget.ProgressBar;
import android.widget.TextView;

public class MyListView extends ListView implements OnScrollListener 
{
	private static final String TAG = "am10";
	private final static int RELEASE_To_REFRESH = 0;
	private final static int PULL_To_REFRESH = 1;
	private final static int REFRESHING = 2;
	private final static int DONE = 3;

	private final static int RELEASE_TO_LOAD = 4;
	private final static int PULL_TO_LOAD = 5;
	private final static int LOADING = 6;
	//private final static int LOADDONE = 7;

	// 实际的padding的距离与界面上偏移距离的比例
	private final static int RATIO = 3;
	
	private LayoutInflater inflater;
	private LinearLayout headView;
	private LinearLayout footView;
	private TextView tipsTextview;
	private TextView loadtipsTextView;
	private TextView lastUpdatedTextView;
	private ImageView arrowImageView;
	private ImageView uparrowImageView;
	//private ProgressBar progressBar;

	private RotateAnimation animation;
	private RotateAnimation reverseAnimation;

	// 用于保证startY的值在一个完整的touch事件中只被记录一次
	private boolean isRecored;
	
	private int startY;
	private int firstItemIndex;
	private int lastItemIndex;
	private int lastVisiableItemIndex;
	
	private int headContentWidth;
	private int headContentHeight;
	private int footContentWidth;
	private int footContentHeight;

	private int state;

	private boolean isBack;

	private OnRefreshListener refreshListener;
	private OnLoadingListener loadingListener;

	private boolean isRefreshable;
	private boolean isLoadable;
	
	public MyListView(Context context) 
	{
		super(context);
		init(context);
	}

	public MyListView(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		init(context);
	}

	private void init(Context context) 
	{
		//setCacheColorHint(context.getResources().getColor(R.color.transparent));
		inflater = LayoutInflater.from(context);

		headView = (LinearLayout) inflater.inflate(R.layout.head, null);
		footView = (LinearLayout) inflater.inflate(R.layout.foot, null);
		
		arrowImageView = (ImageView) headView.findViewById(R.id.head_arrowImageView);
		arrowImageView.setMinimumWidth(70);
		arrowImageView.setMinimumHeight(50);
		
		uparrowImageView = (ImageView) footView.findViewById(R.id.foot_arrowImageView);
		uparrowImageView.setMinimumWidth(70);
		uparrowImageView.setMinimumHeight(50);
		
		//progressBar = (ProgressBar) headView.findViewById(R.id.head_progressBar);		
		tipsTextview = (TextView) headView.findViewById(R.id.head_tipsTextView);
		loadtipsTextView = (TextView) footView.findViewById(R.id.foot_tipsTextView);
		lastUpdatedTextView = (TextView) headView.findViewById(R.id.head_lastUpdatedTextView);

		// 估算headView的宽和高
		measureView(headView);
		measureView(footView);
		headContentHeight = headView.getMeasuredHeight();
		headContentWidth = headView.getMeasuredWidth();
		footContentHeight = footView.getMeasuredHeight();
		footContentWidth = footView.getMeasuredWidth();
		
		
		headView.setPadding(0, -1 * headContentHeight, 0, 0);
		footView.setPadding(0, 0, 0,-1 * footContentHeight);
		this.setPadding(0, 0, 0, 10);
		
		headView.invalidate();
		footView.invalidate();
		
		Log.v(TAG, "width:" + headContentWidth + " height:" + headContentHeight);
		Log.v(TAG, "width:" + headContentWidth + " height:" + footContentHeight);
		addHeaderView(headView, null, false);
		addFooterView(footView,null,false);
		setOnScrollListener(this);

		animation = new RotateAnimation(0, -180,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		animation.setInterpolator(new LinearInterpolator());
		animation.setDuration(250);
		animation.setFillAfter(true);

		reverseAnimation = new RotateAnimation(-180, 0,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		reverseAnimation.setInterpolator(new LinearInterpolator());
		reverseAnimation.setDuration(200);
		reverseAnimation.setFillAfter(true);

		state = DONE;
		isRefreshable = false;
	}

	public void onScroll(AbsListView arg0, int firstVisiableItem, int arg2,	int arg3) 
	{
		firstItemIndex = firstVisiableItem;
		lastVisiableItemIndex = firstItemIndex+arg2-1;
		lastItemIndex = arg3;
		//Log.v(TAG, "lastVisiableItemIndex:"+lastVisiableItemIndex);
		//Log.v(TAG, "lastItemIndex:"+lastItemIndex);
	}

	public void onScrollStateChanged(AbsListView arg0, int arg1) 
	{
	}

	public boolean onTouchEvent(MotionEvent event) 
	{
		// Log.v(TAG, "isRefreshable: "+isRefreshable);
		if (isRefreshable) 
		{
			switch (event.getAction()) 
			{
			case MotionEvent.ACTION_DOWN:
				if (firstItemIndex == 0 && !isRecored) 
				{
					isRecored = true;
					
					// 触摸屏幕的位置
					startY = (int) event.getY();	
					Log.v(TAG, "在down时候记录当前位置" + " startY:"+startY);
				}
				else if(lastVisiableItemIndex == lastItemIndex-1 && !isRecored)
				{
					isRecored = true;
					
					startY =(int) event.getY();
					Log.v(TAG, "在down时候记录当前位置" + " startY:"+startY);
				}
				break;

			case MotionEvent.ACTION_UP:
				if (state != REFRESHING && state != LOADING) 
				{
					if (state == DONE) 
					{
						// 什么都不做
					}
					
					if (state == PULL_To_REFRESH) 
					{
						state = DONE;
						changeHeaderViewByState();
						Log.v(TAG, "由下拉刷新状态，到done状态");
					}
					if(state == PULL_TO_LOAD)
					{
						state=DONE;
						changeHeaderViewByState();
						Log.v(TAG, "由上拉继续加载状态，到done状态");
					}
					
					if (state == RELEASE_To_REFRESH) 
					{
						state = REFRESHING;
						changeHeaderViewByState();
						onRefresh();
						Log.v(TAG, "由松开刷新状态，到刷新状态再到done状态");
					}
					
					if (state == RELEASE_TO_LOAD) 
					{
						state = LOADING;
						changeHeaderViewByState();
						onLoading();
						Log.v(TAG, "由松开加载状态，到刷新状态再到done状态");
					}
				}

				isRecored = false;
				isBack = false;
				break;

			case MotionEvent.ACTION_MOVE:
				int tempY = (int) event.getY();
				//Log.v(TAG, "tempY: " + tempY);
				
				/** 							 
				 * 手指移动过程中tempY数据会不断变化,当滑动到firstItemIndex,即到达顶部,
				 * 需要记录手指所在屏幕的位置: startY = tempY ,后面作位置比较使用
				 * 
				 * 如果手指继续向下推,tempY继续变化,当tempY-startY>0,即是需要显示header部分
				 * 
				 * 此时需要更改状态：state = PULL_To_REFRESH
				 */
				if (!isRecored && firstItemIndex == 0) 
				{
					isRecored = true;
					startY = tempY;
					Log.v(TAG, "在move时候记录下位置" + " startY:"+startY);
				}
				if(!isRecored && lastVisiableItemIndex == lastItemIndex-1)
				{
					isRecored = true;
					startY =tempY;
					Log.v(TAG, "在move时候记录下位置" + " startY:"+startY);
				}

				if (state != REFRESHING && isRecored && state != LOADING) 
				{
					/**
					 * 保证在设置padding的过程中，当前的位置一直是在head，
					 * 否则如果当列表超出屏幕的话，当在上推的时候，列表会同时进行滚动
					 */					

					// 可以松手去刷新了
					if (state == RELEASE_To_REFRESH) 
					{
						setSelection(0);

						// 往上推了，推到了屏幕足够掩盖head的程度，但是还没有推到全部掩盖的地步
						if (((tempY - startY) / RATIO < headContentHeight) && (tempY - startY) > 0) 
						{
							state = PULL_To_REFRESH;
							changeHeaderViewByState();
							Log.v(TAG, "由松开刷新状态转变到下拉刷新状态");
						}
						
						// 一下子推到顶了,没有显示header部分时,应该恢复DONE状态,这里机率很小
						else if (tempY - startY <= 0) 
						{
							state = DONE;
							changeHeaderViewByState();
							Log.v(TAG, "---由松开刷新状态转变到done状态");
						}						
						else 
						{
							// 往下拉了，或者还没有上推到屏幕顶部掩盖head的地步
							// 不用进行特别的操作，只用更新paddingTop的值就行了
						}
					}
					
					if (state == RELEASE_TO_LOAD) 
					{
						setSelection(lastVisiableItemIndex);

						// 往上推了，推到了屏幕足够掩盖head的程度，但是还没有推到全部掩盖的地步
						if (((startY-tempY) / RATIO < footContentHeight) && (startY-tempY) > 0) 
						{
							state = PULL_TO_LOAD;
							changeHeaderViewByState();
							Log.v(TAG, "由松开加载状态转变到上拉继续加载状态");
						}
						
						// 一下子推到顶了,没有显示header部分时,应该恢复DONE状态,这里机率很小
						else if (startY -tempY <= 0) 
						{
							state = DONE;
							changeHeaderViewByState();
							Log.v(TAG, "---由松开加载状态转变到done状态");
						}						
						else 
						{
							// 往下拉了，或者还没有上推到屏幕顶部掩盖head的地步
							// 不用进行特别的操作，只用更新paddingTop的值就行了
						}
					}
					// 还没有到达显示松开刷新的时候,DONE或者是PULL_To_REFRESH状态
					if (state == PULL_To_REFRESH) 
					{
						setSelection(0);

						/**
						 * 下拉到可以进入RELEASE_TO_REFRESH的状态
						 * 
						 * 等于headContentHeight时,即是正好完全显示header部分
						 * 大于headContentHeight时,即是超出header部分更多
						 * 
						 * 当header部分能够完全显示或者超出显示,
						 * 需要更改状态: state = RELEASE_To_REFRESH
						 */
						if ((tempY - startY) / RATIO >= headContentHeight) 
						{
							state = RELEASE_To_REFRESH;
							isBack = true;
							changeHeaderViewByState();
							Log.v(TAG, "由done或者下拉刷新状态转变到松开刷新");
						}
						
						// 上推到顶了,没有显示header部分时,应该恢复DONE状态
						else if (tempY - startY <= 0) 
						{
							state = DONE;
							changeHeaderViewByState();
							Log.v(TAG, "由done或者下拉刷新状态转变到done状态");
						}
					}
					if (state == PULL_TO_LOAD) 
					{
						//setSelection(0);
						setSelection(lastVisiableItemIndex);
						/**
						 * 下拉到可以进入RELEASE_TO_REFRESH的状态
						 * 
						 * 等于headContentHeight时,即是正好完全显示header部分
						 * 大于headContentHeight时,即是超出header部分更多
						 * 
						 * 当header部分能够完全显示或者超出显示,
						 * 需要更改状态: state = RELEASE_To_REFRESH
						 */
						if ((startY-tempY) / RATIO >= footContentHeight) 
						{
							state = RELEASE_TO_LOAD;
							isBack = true;
							changeHeaderViewByState();
							Log.v(TAG, "上拉继续加载状态转变到松开加载");
						}
						
						// 上推到顶了,没有显示header部分时,应该恢复DONE状态
						else if (startY -tempY <= 0) 
						{
							state = DONE;
							changeHeaderViewByState();
							Log.v(TAG, "上拉继续加载状态转变到done状态");
						}
					}

					// done状态下
					if (state == DONE) 
					{
						if (tempY - startY > 0 && firstItemIndex == 0) 
						{
							/** 							 
							 * 手指移动过程中tempY数据会不断变化,当滑动到firstItemIndex,即到达顶部,
							 * 需要记录手指所在屏幕的位置: startY = tempY ,后面作位置比较使用
							 * 
							 * 如果手指继续向下推,tempY继续变化,当tempY-startY>0,即是需要显示header部分
							 * 
							 * 此时需要更改状态：state = PULL_To_REFRESH
							 */
							//Log.v(TAG, "----------------PULL_To_REFRESH " + (tempY - startY));							
							state = PULL_To_REFRESH;
							changeHeaderViewByState();
						}
						if(startY - tempY > 0 && lastVisiableItemIndex == lastItemIndex-1 )
						{
							state =PULL_TO_LOAD;
							changeHeaderViewByState();
							Log.v(TAG,"由done状态转换到上拉继续加载状态");
						}
					}

					// 更新headView的paddingTop
					if (state == PULL_To_REFRESH) 
					{
						//Log.v(TAG, "----------------PULL_To_REFRESH2 " + (tempY - startY));
						headView.setPadding(0, -1 * headContentHeight + (tempY - startY) / RATIO, 0, 0);
					}
					if (state == PULL_TO_LOAD) 
					{
						Log.v(TAG, "----------------PULL_TO_LOAD " + (startY-tempY)+"footContentHeight"+footContentHeight);
						footView.setPadding(0, 0, 0,-1 * footContentHeight + (startY - tempY) / RATIO);
					}
					// 继续更新headView的paddingTop
					if (state == RELEASE_To_REFRESH) 
					{
						headView.setPadding(0, (tempY - startY) / RATIO	- headContentHeight, 0, 0);
					}
					if (state == RELEASE_TO_LOAD) 
					{
						footView.setPadding(0, 0, 0,(startY - tempY) / RATIO - footContentHeight);
					}
				}
				break;
			}
		}
		return super.onTouchEvent(event);
	}

	// 当状态改变时候，调用该方法，以更新界面
	private void changeHeaderViewByState() 
	{
		switch (state) 
		{
			case RELEASE_To_REFRESH:
				arrowImageView.setVisibility(View.VISIBLE);
				//progressBar.setVisibility(View.GONE);
				tipsTextview.setVisibility(View.VISIBLE);
				lastUpdatedTextView.setVisibility(View.VISIBLE);
	
				arrowImageView.clearAnimation();
				arrowImageView.startAnimation(animation);
	
				tipsTextview.setText("松开刷新");
	
				Log.v(TAG, "当前状态，松开刷新");
				break;
			case RELEASE_TO_LOAD:
				uparrowImageView.setVisibility(View.VISIBLE);
				loadtipsTextView.setVisibility(View.VISIBLE);
				
				uparrowImageView.clearAnimation();
				uparrowImageView.startAnimation(animation);
				
				loadtipsTextView.setText("松开继续加载");
				Log.v(TAG, "当前状态，松开加载");
				break;
			case PULL_To_REFRESH:
				//progressBar.setVisibility(View.GONE);
				tipsTextview.setVisibility(View.VISIBLE);
				lastUpdatedTextView.setVisibility(View.VISIBLE);
				arrowImageView.clearAnimation();
				arrowImageView.setVisibility(View.VISIBLE);
				
				/**
				 *  是否向下滑回，是由RELEASE_To_REFRESH状态转变来的
				 */
				if (isBack) 
				{					
					isBack = false;
					arrowImageView.clearAnimation();
					arrowImageView.startAnimation(reverseAnimation);	
					tipsTextview.setText("下拉刷新");
					//Log.v(TAG, "isBack: " + isBack);
				} 
				else 
				{					
					tipsTextview.setText("下拉刷新");
					//Log.v(TAG, "isBack: " + isBack);
				}
				
				Log.v(TAG, "当前状态，下拉刷新");
				break;
			case PULL_TO_LOAD:
				//progressBar.setVisibility(View.GONE);
				loadtipsTextView.setVisibility(View.VISIBLE);
				//lastUpdatedTextView.setVisibility(View.VISIBLE);
				uparrowImageView.clearAnimation();
				uparrowImageView.setVisibility(View.VISIBLE);
				
				/**
				 *  是否向下滑回，是由RELEASE_To_REFRESH状态转变来的
				 */
				if (isBack) 
				{					
					isBack = false;
					uparrowImageView.clearAnimation();
					uparrowImageView.startAnimation(reverseAnimation);	
					loadtipsTextView.setText("上拉继续加载");
					//Log.v(TAG, "isBack: " + isBack);
				} 
				else 
				{					
					loadtipsTextView.setText("上拉继续加载");
					//Log.v(TAG, "isBack: " + isBack);
				}
				
				Log.v(TAG, "当前状态，上拉继续加载");
				break;
			case REFRESHING:
				Log.v(TAG, "REFRESHING...");
				headView.setPadding(0, 0, 0, 0);
	
				//progressBar.setVisibility(View.VISIBLE);
				arrowImageView.clearAnimation();
				arrowImageView.setVisibility(View.GONE);
				tipsTextview.setText("正在刷新...");
				lastUpdatedTextView.setVisibility(View.VISIBLE);
	
				Log.v(TAG, "当前状态,正在刷新...");
				break;
			case LOADING:
				Log.v(TAG, "LOADING...");
				footView.setPadding(0, 0, 0, 0);
	
				//progressBar.setVisibility(View.VISIBLE);
				uparrowImageView.clearAnimation();
				uparrowImageView.setVisibility(View.GONE);
				loadtipsTextView.setText("正在加载...");
				//lastUpdatedTextView.setVisibility(View.VISIBLE);
	
				Log.v(TAG, "当前状态,正在加载...");
				break;
			case DONE:
				headView.setPadding(0, -1 * headContentHeight, 0, 0);
				footView.setPadding(0, 0, 0,-1 * footContentHeight);
				//progressBar.setVisibility(View.GONE);
				arrowImageView.clearAnimation();
				uparrowImageView.clearAnimation();
				arrowImageView.setImageResource(R.drawable.arrow_down);
				uparrowImageView.setImageResource(R.drawable.arrow_up);
				tipsTextview.setText("下拉刷新");
				loadtipsTextView.setText("上拉继续加载");
				lastUpdatedTextView.setVisibility(View.VISIBLE);
	
				Log.v(TAG, "当前状态，done");
				break;
		}
	}

	public void setOnRefreshListener(OnRefreshListener refreshListener) 
	{
		this.refreshListener = refreshListener;
		isRefreshable = true;
	}

	public void setOnLoadingListener(OnLoadingListener onLoadingListener)
	{
		this.loadingListener = onLoadingListener;
		isLoadable = true;
	}
	public interface OnRefreshListener 
	{
		public void onRefresh();
	}

	public interface OnLoadingListener
	{
		public void onLoading();
	}
	public void onRefreshComplete() 
	{
		state = DONE;
		SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日  HH:mm");
		String date = format.format(new Date());
		lastUpdatedTextView.setText("最近更新:" + date);
		changeHeaderViewByState();
	}

	private void onRefresh() 
	{
		if (refreshListener != null) 
		{
			refreshListener.onRefresh();
		}
	}
	public void onLoadingComplete()
	{
		state=DONE;
		changeHeaderViewByState();
	}
	
	private void onLoading()
	{
		if(loadingListener != null)
		{
			loadingListener.onLoading();
		}
	}

	// 此方法直接照搬自网络上的一个下拉刷新的demo，此处是“估计”headView的width以及height
	@SuppressWarnings("deprecation")
	private void measureView(View child) 
	{
		ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) 
        {
        	//Log.v(TAG, "LayoutParams is null.");
            p = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        
        if (lpHeight > 0) 
        {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } 
        else 
        {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }

        child.measure(childWidthSpec, childHeightSpec);
	}

	public void setAdapter(BaseAdapter adapter) 
	{
		SimpleDateFormat format=new SimpleDateFormat("yyyy年MM月dd日  HH:mm");
		String date=format.format(new Date());
		lastUpdatedTextView.setText("最近更新:" + date);
		super.setAdapter(adapter);
	}
}