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

	// ʵ�ʵ�padding�ľ����������ƫ�ƾ���ı���
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

	// ���ڱ�֤startY��ֵ��һ��������touch�¼���ֻ����¼һ��
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

		// ����headView�Ŀ�͸�
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
					
					// ������Ļ��λ��
					startY = (int) event.getY();	
					Log.v(TAG, "��downʱ���¼��ǰλ��" + " startY:"+startY);
				}
				else if(lastVisiableItemIndex == lastItemIndex-1 && !isRecored)
				{
					isRecored = true;
					
					startY =(int) event.getY();
					Log.v(TAG, "��downʱ���¼��ǰλ��" + " startY:"+startY);
				}
				break;

			case MotionEvent.ACTION_UP:
				if (state != REFRESHING && state != LOADING) 
				{
					if (state == DONE) 
					{
						// ʲô������
					}
					
					if (state == PULL_To_REFRESH) 
					{
						state = DONE;
						changeHeaderViewByState();
						Log.v(TAG, "������ˢ��״̬����done״̬");
					}
					if(state == PULL_TO_LOAD)
					{
						state=DONE;
						changeHeaderViewByState();
						Log.v(TAG, "��������������״̬����done״̬");
					}
					
					if (state == RELEASE_To_REFRESH) 
					{
						state = REFRESHING;
						changeHeaderViewByState();
						onRefresh();
						Log.v(TAG, "���ɿ�ˢ��״̬����ˢ��״̬�ٵ�done״̬");
					}
					
					if (state == RELEASE_TO_LOAD) 
					{
						state = LOADING;
						changeHeaderViewByState();
						onLoading();
						Log.v(TAG, "���ɿ�����״̬����ˢ��״̬�ٵ�done״̬");
					}
				}

				isRecored = false;
				isBack = false;
				break;

			case MotionEvent.ACTION_MOVE:
				int tempY = (int) event.getY();
				//Log.v(TAG, "tempY: " + tempY);
				
				/** 							 
				 * ��ָ�ƶ�������tempY���ݻ᲻�ϱ仯,��������firstItemIndex,�����ﶥ��,
				 * ��Ҫ��¼��ָ������Ļ��λ��: startY = tempY ,������λ�ñȽ�ʹ��
				 * 
				 * �����ָ����������,tempY�����仯,��tempY-startY>0,������Ҫ��ʾheader����
				 * 
				 * ��ʱ��Ҫ����״̬��state = PULL_To_REFRESH
				 */
				if (!isRecored && firstItemIndex == 0) 
				{
					isRecored = true;
					startY = tempY;
					Log.v(TAG, "��moveʱ���¼��λ��" + " startY:"+startY);
				}
				if(!isRecored && lastVisiableItemIndex == lastItemIndex-1)
				{
					isRecored = true;
					startY =tempY;
					Log.v(TAG, "��moveʱ���¼��λ��" + " startY:"+startY);
				}

				if (state != REFRESHING && isRecored && state != LOADING) 
				{
					/**
					 * ��֤������padding�Ĺ����У���ǰ��λ��һֱ����head��
					 * ����������б�����Ļ�Ļ����������Ƶ�ʱ���б��ͬʱ���й���
					 */					

					// ��������ȥˢ����
					if (state == RELEASE_To_REFRESH) 
					{
						setSelection(0);

						// �������ˣ��Ƶ�����Ļ�㹻�ڸ�head�ĳ̶ȣ����ǻ�û���Ƶ�ȫ���ڸǵĵز�
						if (((tempY - startY) / RATIO < headContentHeight) && (tempY - startY) > 0) 
						{
							state = PULL_To_REFRESH;
							changeHeaderViewByState();
							Log.v(TAG, "���ɿ�ˢ��״̬ת�䵽����ˢ��״̬");
						}
						
						// һ�����Ƶ�����,û����ʾheader����ʱ,Ӧ�ûָ�DONE״̬,������ʺ�С
						else if (tempY - startY <= 0) 
						{
							state = DONE;
							changeHeaderViewByState();
							Log.v(TAG, "---���ɿ�ˢ��״̬ת�䵽done״̬");
						}						
						else 
						{
							// �������ˣ����߻�û�����Ƶ���Ļ�����ڸ�head�ĵز�
							// ���ý����ر�Ĳ�����ֻ�ø���paddingTop��ֵ������
						}
					}
					
					if (state == RELEASE_TO_LOAD) 
					{
						setSelection(lastVisiableItemIndex);

						// �������ˣ��Ƶ�����Ļ�㹻�ڸ�head�ĳ̶ȣ����ǻ�û���Ƶ�ȫ���ڸǵĵز�
						if (((startY-tempY) / RATIO < footContentHeight) && (startY-tempY) > 0) 
						{
							state = PULL_TO_LOAD;
							changeHeaderViewByState();
							Log.v(TAG, "���ɿ�����״̬ת�䵽������������״̬");
						}
						
						// һ�����Ƶ�����,û����ʾheader����ʱ,Ӧ�ûָ�DONE״̬,������ʺ�С
						else if (startY -tempY <= 0) 
						{
							state = DONE;
							changeHeaderViewByState();
							Log.v(TAG, "---���ɿ�����״̬ת�䵽done״̬");
						}						
						else 
						{
							// �������ˣ����߻�û�����Ƶ���Ļ�����ڸ�head�ĵز�
							// ���ý����ر�Ĳ�����ֻ�ø���paddingTop��ֵ������
						}
					}
					// ��û�е�����ʾ�ɿ�ˢ�µ�ʱ��,DONE������PULL_To_REFRESH״̬
					if (state == PULL_To_REFRESH) 
					{
						setSelection(0);

						/**
						 * ���������Խ���RELEASE_TO_REFRESH��״̬
						 * 
						 * ����headContentHeightʱ,����������ȫ��ʾheader����
						 * ����headContentHeightʱ,���ǳ���header���ָ���
						 * 
						 * ��header�����ܹ���ȫ��ʾ���߳�����ʾ,
						 * ��Ҫ����״̬: state = RELEASE_To_REFRESH
						 */
						if ((tempY - startY) / RATIO >= headContentHeight) 
						{
							state = RELEASE_To_REFRESH;
							isBack = true;
							changeHeaderViewByState();
							Log.v(TAG, "��done��������ˢ��״̬ת�䵽�ɿ�ˢ��");
						}
						
						// ���Ƶ�����,û����ʾheader����ʱ,Ӧ�ûָ�DONE״̬
						else if (tempY - startY <= 0) 
						{
							state = DONE;
							changeHeaderViewByState();
							Log.v(TAG, "��done��������ˢ��״̬ת�䵽done״̬");
						}
					}
					if (state == PULL_TO_LOAD) 
					{
						//setSelection(0);
						setSelection(lastVisiableItemIndex);
						/**
						 * ���������Խ���RELEASE_TO_REFRESH��״̬
						 * 
						 * ����headContentHeightʱ,����������ȫ��ʾheader����
						 * ����headContentHeightʱ,���ǳ���header���ָ���
						 * 
						 * ��header�����ܹ���ȫ��ʾ���߳�����ʾ,
						 * ��Ҫ����״̬: state = RELEASE_To_REFRESH
						 */
						if ((startY-tempY) / RATIO >= footContentHeight) 
						{
							state = RELEASE_TO_LOAD;
							isBack = true;
							changeHeaderViewByState();
							Log.v(TAG, "������������״̬ת�䵽�ɿ�����");
						}
						
						// ���Ƶ�����,û����ʾheader����ʱ,Ӧ�ûָ�DONE״̬
						else if (startY -tempY <= 0) 
						{
							state = DONE;
							changeHeaderViewByState();
							Log.v(TAG, "������������״̬ת�䵽done״̬");
						}
					}

					// done״̬��
					if (state == DONE) 
					{
						if (tempY - startY > 0 && firstItemIndex == 0) 
						{
							/** 							 
							 * ��ָ�ƶ�������tempY���ݻ᲻�ϱ仯,��������firstItemIndex,�����ﶥ��,
							 * ��Ҫ��¼��ָ������Ļ��λ��: startY = tempY ,������λ�ñȽ�ʹ��
							 * 
							 * �����ָ����������,tempY�����仯,��tempY-startY>0,������Ҫ��ʾheader����
							 * 
							 * ��ʱ��Ҫ����״̬��state = PULL_To_REFRESH
							 */
							//Log.v(TAG, "----------------PULL_To_REFRESH " + (tempY - startY));							
							state = PULL_To_REFRESH;
							changeHeaderViewByState();
						}
						if(startY - tempY > 0 && lastVisiableItemIndex == lastItemIndex-1 )
						{
							state =PULL_TO_LOAD;
							changeHeaderViewByState();
							Log.v(TAG,"��done״̬ת����������������״̬");
						}
					}

					// ����headView��paddingTop
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
					// ��������headView��paddingTop
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

	// ��״̬�ı�ʱ�򣬵��ø÷������Ը��½���
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
	
				tipsTextview.setText("�ɿ�ˢ��");
	
				Log.v(TAG, "��ǰ״̬���ɿ�ˢ��");
				break;
			case RELEASE_TO_LOAD:
				uparrowImageView.setVisibility(View.VISIBLE);
				loadtipsTextView.setVisibility(View.VISIBLE);
				
				uparrowImageView.clearAnimation();
				uparrowImageView.startAnimation(animation);
				
				loadtipsTextView.setText("�ɿ���������");
				Log.v(TAG, "��ǰ״̬���ɿ�����");
				break;
			case PULL_To_REFRESH:
				//progressBar.setVisibility(View.GONE);
				tipsTextview.setVisibility(View.VISIBLE);
				lastUpdatedTextView.setVisibility(View.VISIBLE);
				arrowImageView.clearAnimation();
				arrowImageView.setVisibility(View.VISIBLE);
				
				/**
				 *  �Ƿ����»��أ�����RELEASE_To_REFRESH״̬ת������
				 */
				if (isBack) 
				{					
					isBack = false;
					arrowImageView.clearAnimation();
					arrowImageView.startAnimation(reverseAnimation);	
					tipsTextview.setText("����ˢ��");
					//Log.v(TAG, "isBack: " + isBack);
				} 
				else 
				{					
					tipsTextview.setText("����ˢ��");
					//Log.v(TAG, "isBack: " + isBack);
				}
				
				Log.v(TAG, "��ǰ״̬������ˢ��");
				break;
			case PULL_TO_LOAD:
				//progressBar.setVisibility(View.GONE);
				loadtipsTextView.setVisibility(View.VISIBLE);
				//lastUpdatedTextView.setVisibility(View.VISIBLE);
				uparrowImageView.clearAnimation();
				uparrowImageView.setVisibility(View.VISIBLE);
				
				/**
				 *  �Ƿ����»��أ�����RELEASE_To_REFRESH״̬ת������
				 */
				if (isBack) 
				{					
					isBack = false;
					uparrowImageView.clearAnimation();
					uparrowImageView.startAnimation(reverseAnimation);	
					loadtipsTextView.setText("������������");
					//Log.v(TAG, "isBack: " + isBack);
				} 
				else 
				{					
					loadtipsTextView.setText("������������");
					//Log.v(TAG, "isBack: " + isBack);
				}
				
				Log.v(TAG, "��ǰ״̬��������������");
				break;
			case REFRESHING:
				Log.v(TAG, "REFRESHING...");
				headView.setPadding(0, 0, 0, 0);
	
				//progressBar.setVisibility(View.VISIBLE);
				arrowImageView.clearAnimation();
				arrowImageView.setVisibility(View.GONE);
				tipsTextview.setText("����ˢ��...");
				lastUpdatedTextView.setVisibility(View.VISIBLE);
	
				Log.v(TAG, "��ǰ״̬,����ˢ��...");
				break;
			case LOADING:
				Log.v(TAG, "LOADING...");
				footView.setPadding(0, 0, 0, 0);
	
				//progressBar.setVisibility(View.VISIBLE);
				uparrowImageView.clearAnimation();
				uparrowImageView.setVisibility(View.GONE);
				loadtipsTextView.setText("���ڼ���...");
				//lastUpdatedTextView.setVisibility(View.VISIBLE);
	
				Log.v(TAG, "��ǰ״̬,���ڼ���...");
				break;
			case DONE:
				headView.setPadding(0, -1 * headContentHeight, 0, 0);
				footView.setPadding(0, 0, 0,-1 * footContentHeight);
				//progressBar.setVisibility(View.GONE);
				arrowImageView.clearAnimation();
				uparrowImageView.clearAnimation();
				arrowImageView.setImageResource(R.drawable.arrow_down);
				uparrowImageView.setImageResource(R.drawable.arrow_up);
				tipsTextview.setText("����ˢ��");
				loadtipsTextView.setText("������������");
				lastUpdatedTextView.setVisibility(View.VISIBLE);
	
				Log.v(TAG, "��ǰ״̬��done");
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
		SimpleDateFormat format = new SimpleDateFormat("yyyy��MM��dd��  HH:mm");
		String date = format.format(new Date());
		lastUpdatedTextView.setText("�������:" + date);
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

	// �˷���ֱ���հ��������ϵ�һ������ˢ�µ�demo���˴��ǡ����ơ�headView��width�Լ�height
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
		SimpleDateFormat format=new SimpleDateFormat("yyyy��MM��dd��  HH:mm");
		String date=format.format(new Date());
		lastUpdatedTextView.setText("�������:" + date);
		super.setAdapter(adapter);
	}
}