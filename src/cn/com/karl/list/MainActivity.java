package cn.com.karl.list;


import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
//import java.util.Map;




import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.actionbarsherlock.view.SubMenu;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;





import cn.com.karl.list.MyListView.OnRefreshListener;
import cn.com.karl.list.MyListView.OnLoadingListener;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.sina.weibo.SinaWeibo;
import cn.sharesdk.sina.weibo.SinaWeibo.ShareParams;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
//import android.app.Activity;
//import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
//import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
//import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;


@SuppressLint("HandlerLeak")
public class MainActivity extends SlidingFragmentActivity
{

	public static final short SINA_STATELOAD = 10;
	public static final short SINA_FIRST_STATELOAD = 11;
	public static final short SINA_LOADMORE = 12;
	//�Ƿ��Ѿ���Ȩ
	protected boolean flag = false;
	protected LinkedList<String> data;
	protected LinkedList<String> imageurl;
	protected LinkedList<String> tpimgageurl =new LinkedList<String>();
	protected LinkedList<String> tempdata = new LinkedList<String>();
	
	protected static long sinceId = 0;
	protected static long maxId = 0;
	protected MyListView listView;
	protected Platform weibo = null;
	protected static String userId = null;
	protected static final int MSG_REFRESH = 2;
	protected static final int MSG_DISPLAY = 1;
	protected static final int MSG_LOAD = 0;
	protected static final int MSG_LOADMORE = 3;

	
	
	//ListView������
	protected BaseAdapter adapter = new BaseAdapter() 
	{
		ViewHolder viewHolder = null;
		LinkedList<Bitmap> cache = new LinkedList<Bitmap>(){};
		public View getView(int position, View convertView, ViewGroup parent) 
		{
			if(convertView == null)
			{
				convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.item, null);
			
				viewHolder = new ViewHolder();
				viewHolder.mprofileImageView = (ImageView) convertView.findViewById(R.id.imageView_item);
				viewHolder.mtextView = (TextView) convertView.findViewById(R.id.textView_item);
				viewHolder.mcontentImageView = (ImageView) convertView.findViewById(R.id.imageView_content);
								
				convertView.setTag(viewHolder);
			}else {  
	            viewHolder = (ViewHolder) convertView.getTag();  
	        }  
			if(imageurl.get(position)!=null)
			{	
				Bitmap bitmap = getImage(position);
				if(bitmap!=null)
				{
					viewHolder.mcontentImageView.setImageBitmap(bitmap);
					Log.v("xxxx","ֱ�Ӵӻ����ȡ��image��"+position);
				}
				else
				{
					downLoadTask dlTask =new downLoadTask();
					dlTask.execute(viewHolder, imageurl.get(position), position);
					Log.v("xxxx","�����첽����,imageurl:"+position);
				}
			}
			viewHolder.mtextView.setText(data.get(position));
			viewHolder.mtextView.setTextColor(android.graphics.Color.BLACK);
			Log.v("xxxx","�������֣�"+position);
			return convertView;
		}

		public long getItemId(int position) 
		{
			return position;
		}

		public Object getItem(int position) 
		{
			return data.get(position);
		}

		public int getCount() 
		{
			return data.size();
		}
		
		class downLoadTask extends AsyncTask<Object, Integer, Bitmap>
	    {    
			ViewHolder mHolder;  
	        int position; 
			@Override
			protected Bitmap doInBackground(Object...params) 
			{
				mHolder = (ViewHolder) params[0];  
	            String url = (String) params[1];  
	            position = (Integer) params[2];  
	            Bitmap drawable = ImageLoader.loadImage(url);//��ȡ����ͼƬ  
	            Log.v("xxxx","�������أ�bitmap��"+position);
	            return drawable;       
			}	

			@Override
			protected void onPostExecute(Bitmap result) 
			{
				if (result == null) {  
	                return;  
	            }  
	            //mHolder.mcontentImageView.setImageBitmap(result);  
	            cacheImage(position,result);
	            notifyDataSetChanged();  
	            //Log.v("xxxx","���ڰ󶨣�bitmap��"+position);
	        }    	
	    }
		class ViewHolder{
			ImageView mprofileImageView;
			TextView mtextView;
			ImageView mcontentImageView;
		}
		protected void cacheImage(int position,Bitmap bitmap)
		{
			if(position >= cache.size() )
			{
				for(int i=cache.size();i<position;i++)
				{
					cache.add(i,bitmap);
				}
				cache.add(position,bitmap);
			}
			cache.set(position,bitmap);
		}
		protected Bitmap getImage(int position)
		{
			if(position >= cache.size())
				return null;
			return cache.get(position);
		}
	};
	
	//΢����Ϣ������
	protected Handler hd = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == MSG_DISPLAY)
			{
				onDisplayUser();
			}
			else if(msg.what == MSG_LOAD)
			{
				onDisplayState();
			}
			else if(msg.what == MSG_REFRESH)
			{
				onDisplayState();
				listView.onRefreshComplete();
			}
			else if(msg.what == MSG_LOADMORE)
			{
				onDisplayMore();
				listView.onLoadingComplete();
			}
		}
	};
	/**
	 * ��ʾ�û���Ϣ
	 */
	protected void onDisplayUser(){
		//data.addFirst("��ȨToken:"+weibo.getDb().getToken());
		//data.addFirst("UserId:"+weibo.getDb().getUserId());
		//data.addFirst("�ǳƣ�"+weibo.getDb().get("nickname"));
		//adapter.notifyDataSetChanged();
		setTitle(weibo.getDb().get("nickname"));
		onFirstLoadState();
	}
	protected void onDisplayState(){
		Iterator<String> it = tempdata.iterator();
		Iterator<String> it2 = tpimgageurl.iterator();
		while(it.hasNext())
		{
			data.addFirst(it.next());
		}
		while(it2.hasNext())
		{
			imageurl.addFirst(it2.next());
		}
		adapter.notifyDataSetChanged();
		tempdata.clear();
	}
	protected void onDisplayMore(){
		Iterator<String> it = tempdata.iterator();
		Iterator<String> it2 = tpimgageurl.iterator();
		while(it.hasNext())
		{	
			data.add(it.next());
		}
		while(it2.hasNext())
		{
			imageurl.add(it2.next());
		}
		adapter.notifyDataSetChanged();
		tempdata.clear();
	}
	//�첽��ȡ΢����Ϣ
	protected PlatformActionListener paListener =new PlatformActionListener() {
		
		@Override
		public void onError(Platform arg0, int arg1, Throwable arg2) {
			weibo.removeAccount();
			Log.v("MainActivity","ƽ̨��������");
			Log.v("MainActivity","Throwable"+arg2);
		}
		
		@Override
		public void onComplete(Platform arg0, int arg1, HashMap<String, Object> newfeed) {
			Message msg = new Message();
			switch(arg1 & Platform.CUSTOMER_ACTION_MASK)
			{	
			case SINA_FIRST_STATELOAD:
				@SuppressWarnings("unchecked")
				ArrayList<HashMap<String,Object>> a=(ArrayList<HashMap<String,Object>>)newfeed.get("statuses");
				Iterator<HashMap<String, Object>> it = a.iterator();
				while(it.hasNext())
				{
					HashMap<String,Object> temp=it.next();					
					tempdata.addFirst((String)temp.get("text"));
					if(temp.containsKey("bmiddle_pic"))
					{
						tpimgageurl.addFirst((String)temp.get("bmiddle_pic"));			
					}
					else
					{
						tpimgageurl.addFirst(null);
					}
				}
				if(!a.isEmpty())
				{	
					sinceId = (Long)a.get(0).get("id");
					maxId = (Long)a.get(a.size()-1).get("id")-1;
				}
				msg.what = MSG_LOAD;
				break;
			case SINA_STATELOAD:
				@SuppressWarnings("unchecked")
				ArrayList<HashMap<String,Object>> b=(ArrayList<HashMap<String,Object>>)newfeed.get("statuses");
				Iterator<HashMap<String, Object>> it2 = b.iterator();
				while(it2.hasNext())
				{
					HashMap<String,Object> temp=it2.next();					
					tempdata.addFirst((String)temp.get("text"));
					if(temp.containsKey("bmiddle_pic"))
					{
						tpimgageurl.addFirst((String)temp.get("bmiddle_pic"));			
					}
					else
					{
						tpimgageurl.addFirst(null);
					}
				}
				if(!b.isEmpty())
				{	
					sinceId = (Long)b.get(0).get("id");
				}
				msg.what =MSG_REFRESH;
				break;
			case SINA_LOADMORE:
				@SuppressWarnings("unchecked")
				ArrayList<HashMap<String,Object>> c=(ArrayList<HashMap<String,Object>>)newfeed.get("statuses");
				Iterator<HashMap<String, Object>> it3 = c.iterator();
				while(it3.hasNext())
				{
					HashMap<String,Object> temp=it3.next();					
					tempdata.add((String)temp.get("text"));
					if(temp.containsKey("bmiddle_pic"))
					{
						tpimgageurl.add((String)temp.get("bmiddle_pic"));			
					}
					else
					{
						tpimgageurl.add(null);
					}
				}
				if(!c.isEmpty())
				{	
					maxId = (Long)c.get(c.size()-1).get("id")-1;
				}
				msg.what = MSG_LOADMORE;
				break;
			case Platform.ACTION_AUTHORIZING:				
				msg.what = MSG_DISPLAY;
				break;
			}
			hd.sendMessage(msg);
			Log.v("MainActivity","ƽ̨�������");
		}		
		@Override
		public void onCancel(Platform arg0, int arg1) {
			Log.v("MainActivity","ƽ̨����ȡ��");
		}
	};
	@SuppressLint("NewApi")
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		//����΢��ShareSDK��ʼ��
		ShareSDK.initSDK(this);
		weibo = ShareSDK.getPlatform(this, SinaWeibo.NAME);
		

		//��ʼ���໬�˵�
		setSlidingActionBarEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setBehindContentView(R.layout.menu_frame);
		SlidingMenu sm =getSlidingMenu();
		sm.setShadowWidthRes(R.dimen.shadow_width);
		sm.setShadowDrawable(R.drawable.shadow);
		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		sm.setFadeDegree(0.35f);
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		
		// ȡ����Ļ�ߴ��С
		//displayScreenSize();
		// ��ʼ����������
		data = new LinkedList<String>();
		imageurl = new LinkedList<String>();
	    
		listView = (MyListView) findViewById(R.id.listView);		
		listView.setAdapter(adapter);
		listView.setOnRefreshListener(new OnRefreshListener() 
		{
			public void onRefresh() 
			{
				//RefreshTask rTask = new RefreshTask();
				//rTask.execute(1000);
				onLoadState();
			}
		});
		listView.setOnLoadingListener(new OnLoadingListener()
		{
			@Override
			public void onLoading()
			{
				//LoadTask lTask = new LoadTask();
				//lTask.execute(1000);
				onLoadMore();
			}
		});
		if(weibo.isValid ()) 
		{
			weibo.removeAccount();
		}
	}
				
	
	class LoadTask extends AsyncTask<Integer,Integer,String>
	{
		@Override
		protected String doInBackground(Integer...params)
		{
			try
			{
				Thread.sleep(params[0]);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			data.addLast("�¼��ص�����");
			return null;
		}
		protected void onPostExecute(String result) 
		{
			super.onPostExecute(result);
			adapter.notifyDataSetChanged();
			listView.onLoadingComplete();
		}    
	}
	// AsyncTask�첽����
	class RefreshTask extends AsyncTask<Integer, Integer, String>
    {    
    	@Override
		protected String doInBackground(Integer... params) 
		{
			try 
			{
				Thread.sleep(params[0]);
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			// ��data��ǰ�������
			data.addFirst("ˢ�º������");
			//weibo.showUser(null);
			return null;
		}	

		@Override
		protected void onPostExecute(String result) 
		{
			super.onPostExecute(result);
			adapter.notifyDataSetChanged();
			listView.onRefreshComplete();
		}    	
    }

	// ȡ����Ļ�ߴ��С
	/*public void displayScreenSize()
	{
		// ��Ļ�����л�ʱ��÷���
		if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) 
		{		
			setTitle("landscape");
		}
		
		if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) 
		{
			setTitle("portrait");
		}
		
		// �����Ļ��С1
		WindowManager manager = getWindowManager();
		int width = manager.getDefaultDisplay().getWidth();
		int height = manager.getDefaultDisplay().getHeight();
		
		Log.v("am10", "width: " + width + " height: " + height);
		
		// �����Ļ��С2
		DisplayMetrics dMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
		int screenWidth = dMetrics.widthPixels;
		int screenHeight = dMetrics.heightPixels;
		
		Log.v("am10", "screenWidth: " + screenWidth + " screenHeight: " + screenHeight);
	}*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			toggle();
			return true;		
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public void onDestroy(){
		ShareSDK.stopSDK(this);
		super.onDestroy();
		sinceId = 0;
	}
	protected void onRegister(){
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		  MenuItem login = menu.add("login");
	        //login.setIcon(R.drawable.menu_apprec);
		  	login.setTitle("��¼");
	        login.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	        login.setOnMenuItemClickListener(new OnMenuItemClickListener() {
	            @Override
	            public boolean onMenuItemClick(MenuItem item) {
	                onLogin();
	            	return true;
	            }
	        });
	       SubMenu more=menu.addSubMenu("more");
	       more.add("���������");
	       more.add("��������΢��");
	       more.add("ע���˺�");
	       more.add("����");
	       MenuItem morebtn = more.getItem();
	       //morebtn.setIcon(R.drawable.menu_apprec);
	       morebtn.setTitle("��");
	       morebtn.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
	       more.getItem(0).setOnMenuItemClickListener(new OnMenuItemClickListener(){
	    	   @Override
	            public boolean onMenuItemClick(MenuItem item) {
	                onShare();
	            	return true;
	            }
	       });
		return super.onCreateOptionsMenu(menu);
	}
	@SuppressLint("NewApi")
	/**
	 * ��½΢��
	 * 
	 * @return true:��ǰδ��½����ʼִ�е�½����  false����ǰ�ѵ�¼������ִ�е�½����
	 */
	public boolean onLogin(){
		if(weibo.isValid ()) 
		{
			Toast.makeText(MainActivity.this, "���Ѿ���¼��", Toast.LENGTH_LONG).show();
			return false;
		} 
		weibo.setPlatformActionListener(paListener );
		userId = new String(weibo.getDb().getUserId());
		if(userId.isEmpty())
		{
			weibo.authorize();
		}
		return true;
	}
	/**
	 * ��ȡ΢����̬
	 *
	 */
	protected void onFirstLoadState(){
		String url = "https://api.weibo.com/2/statuses/friends_timeline.json";
		String method = "GET";
		short customerAction = SINA_FIRST_STATELOAD;
		HashMap<String, Object> values = new HashMap<String, Object>();
		values.put("access_token",weibo.getDb().getToken());
		weibo.customerProtocol(url, method, customerAction, values, null);
	}
	protected void onLoadState(){
		String url = "https://api.weibo.com/2/statuses/friends_timeline.json";
		String method = "GET";
		short customerAction = SINA_STATELOAD;
		HashMap<String, Object> values = new HashMap<String, Object>();
		values.put("access_token",weibo.getDb().getToken());
		values.put("since_id", sinceId);
		weibo.customerProtocol(url, method, customerAction, values, null);
	}
	protected void onLoadMore(){
		String url = "https://api.weibo.com/2/statuses/friends_timeline.json";
		String method = "GET";
		short customerAction = SINA_LOADMORE;
		HashMap<String, Object> values = new HashMap<String, Object>();
		values.put("access_token",weibo.getDb().getToken());
		values.put("max_id", maxId);
		Log.v("onLoadMore","maxId="+maxId);
		weibo.customerProtocol(url, method, customerAction, values, null);
	}
	/**
	 * ΢������
	 *
	 */
	public boolean onShare(){
		if(!weibo.isValid())
		{	
			Toast.makeText(MainActivity.this, "���½���ٷ���", Toast.LENGTH_LONG).show();
			return false;
		}
		Toast.makeText(MainActivity.this, "�ѷ������Է�����ı�", Toast.LENGTH_LONG).show();
		ShareParams sp = new ShareParams();
		sp.setText("���Է�����ı�");
		weibo.share(sp);
		return true;
	}
}