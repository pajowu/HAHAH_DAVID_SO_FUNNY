package de.pajowu.twitwear;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Collections;
import java.util.Comparator;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.MessageEvent;

import com.twitter.sdk.android.core.models.Tweet;
import java.util.List;

import com.google.gson.Gson;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import android.support.v7.widget.RecyclerView;
import java.util.ArrayList;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.util.Log;
public class MainActivity extends Activity implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks {

    private static final String WEAR_MESSAGE_PATH = "/message";
    private static final String LOAD_PREVIOUS = "/previous";
    private static final String LOAD_NEXT = "/next";
    private static final String SEND_TIMELINE = "/send";
    private static final String TAG = "TWITWEAR";
    private GoogleApiClient mApiClient;
    //TextView jsonView;
    private Gson mGson;
    private List<Tweet> mList = new ArrayList<>();
    private Type mListType;
    CustomRecyclerView recyclerView;
    TweetAdapter mAdapter;
    LinearLayoutManager mLayoutManager;
    Boolean loading = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //jsonView = (TextView) findViewById(R.id.json);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        init();
    }

    private void init() {
        mGson = new Gson();
        mListType = new TypeToken<List<Tweet>>(){}.getType();
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks( this )
                .build();

        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();

        recyclerView = (CustomRecyclerView) findViewById(R.id.recycler_view);
        TextView emptyView = (TextView) findViewById(R.id.empty_view);
        mAdapter = new TweetAdapter(mList);
        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setEmptyView(emptyView);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rV, int dx, int dy) {
                super.onScrolled(rV, dx, dy);

                int visibleItemCount = recyclerView.getChildCount();
                int totalItemCount = mLayoutManager.getItemCount();
                int firstVisibleItemIndex = mLayoutManager.findFirstVisibleItemPosition();
                if ((totalItemCount - visibleItemCount) <= firstVisibleItemIndex) {
                    // Loading NOT in progress and end of list has been reached
                    // also triggered if not enough items to fill the screen
                    // if you start loading
                    if (!loading) {
                        Log.d(TAG, "Bottom "+loading);
                        loading = true;
                        sendMessage(LOAD_PREVIOUS, "");
                    }
                } else if (firstVisibleItemIndex == 0){
                    // top of list reached
                    // if you start loading
                    if (!loading) {
                        Log.d(TAG, "Top "+loading);
                        //loading = true;
                        //sendMessage(LOAD_NEXT, "");
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onMessageReceived( final MessageEvent messageEvent ) {
        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                if( messageEvent.getPath().equalsIgnoreCase( WEAR_MESSAGE_PATH ) ) {
                    loading = false;
                    String data = new String(messageEvent.getData());
                    Log.d(TAG, data);
                    HashSet hs = new HashSet();
                    hs.addAll(mList);
                    hs.addAll((List<Tweet>)mGson.fromJson(data, mListType));
                    mList.clear();
                    mList.addAll(hs);
                    Collections.sort(mList, new TweetComparator());
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener( mApiClient, this );
        sendMessage(SEND_TIMELINE, "");
    }

    @Override
    protected void onStop() {
        if ( mApiClient != null ) {
            Wearable.MessageApi.removeListener( mApiClient, this );
            if ( mApiClient.isConnected() ) {
                mApiClient.disconnect();
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if( mApiClient != null )
            mApiClient.unregisterConnectionCallbacks( this );
        super.onDestroy();
    }

    @Override
    public void onConnectionSuspended(int i) {}

    private void sendMessage( final String path, final String text ) {
        Log.d(TAG, path+text);
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    Log.d(TAG, "send "+path+text);
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }
            }
        }).start();
    }
    public static class TweetComparator implements Comparator<Tweet> {
        @Override
        public int compare(Tweet lhs, Tweet rhs) {
            Long id1 = lhs.id;
            Long id2 = rhs.id;
            if (id1.compareTo(id2) < 0) {
                return 1;
            } else if (id1.compareTo(id2) > 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
