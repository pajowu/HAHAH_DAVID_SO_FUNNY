package de.pajowu.twitwear;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.util.Log;
import android.content.Intent;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import com.google.common.collect.Lists;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.MessageEvent;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.services.StatusesService;
import com.twitter.sdk.android.core.models.Tweet;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import android.widget.LinearLayout;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener {

    private static final String START_ACTIVITY = "/start_activity";
    private static final String WEAR_MESSAGE_PATH = "/message";
    private static final String LOAD_PREVIOUS = "/previous";
    private static final String LOAD_NEXT = "/next";
    private static final String SEND_TIMELINE = "/send";

    private static final String TAG = "TWITWEAR";

    private GoogleApiClient mApiClient;

    private Button mSendButton;
    private Button mRefreshButton;
    private TwitterLoginButton loginButton;
    private Gson mGson;

    private List<Tweet> homeTimeline = new ArrayList<Tweet>();
    Integer count = 200;
    Long maxId;
    Long sinceId;
    Boolean trimUser = false;
    Boolean excludeReplies = false;
    Boolean contributeDetails = false;
    Boolean includeEntities = false;
    TwitterApiClient twitterApiClient;

    private SharedPreferences preferences;
    LinearLayout send_refresh_layout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        initGoogleApiClient();
    }

    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks( this )
                .build();

        mApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    private Boolean isAuthenticated() {
        return TwitterCore.getInstance().getAppSessionManager().getActiveSession() != null;
    }
    private void init() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        mSendButton = (Button) findViewById( R.id.btn_send );
        mSendButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendTimeline();
            }
        });

        mRefreshButton = (Button) findViewById( R.id.btn_refresh );
        mRefreshButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadPrevious();
            }
        });


        mGson = new Gson();
        loginButton = (TwitterLoginButton) findViewById(R.id.login_button);
        loginButton.setCallback(new Callback<TwitterSession>() {
           @Override
           public void success(Result<TwitterSession> result) {
                twitterApiClient = TwitterCore.getInstance().getApiClient();
                loginButton.setVisibility(View.GONE);
                send_refresh_layout.setVisibility(View.VISIBLE);
                loadNext();
               // Do something with result, which provides a TwitterSession for making API calls
           }

           @Override
           public void failure(TwitterException exception) {
               // Do something on failure
           }
        });
        Type listType = new TypeToken<List<Tweet>>(){}.getType();
        String timeline = preferences.getString("timeline", "");
        maxId = preferences.getLong("maxid", 0);
        sinceId = preferences.getLong("sinceid", 0);
        send_refresh_layout = (LinearLayout) findViewById(R.id.send_refresh_layout);
        if (timeline != "") {
            homeTimeline.clear();
            homeTimeline.addAll((List<Tweet>)mGson.fromJson(timeline, listType));
        }
        if (isAuthenticated()) {
            twitterApiClient = TwitterCore.getInstance().getApiClient();
            loginButton.setVisibility(View.GONE);
            send_refresh_layout.setVisibility(View.VISIBLE);
        } else {
            loginButton.setVisibility(View.VISIBLE);
            send_refresh_layout.setVisibility(View.GONE);
        }
    }

    private void sendMessage( final String path, final String text ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }
            }
        }).start();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener( mApiClient, this );
        sendMessage(START_ACTIVITY, "");
    }

    @Override
    public void onConnectionSuspended(int i) { }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result to the login button.
        loginButton.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onMessageReceived( final MessageEvent messageEvent ) {
        Log.d(TAG, messageEvent.getPath());
        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                if( messageEvent.getPath().equalsIgnoreCase( LOAD_PREVIOUS ) ) {
                    loadPrevious();
                } else if ( messageEvent.getPath().equalsIgnoreCase( LOAD_NEXT ) ) {
                    loadNext();
                } else if ( messageEvent.getPath().equalsIgnoreCase( SEND_TIMELINE ) ) {
                    sendTimeline();
                }
            }
        });
    }


    public void loadPrevious() {
        sinceId = null;
        if (maxId == null) {
            maxId = null;
        } else {
            maxId = maxId - 1;
        }
        refresh();
    }
    public void loadNext() {
        maxId = null;
        refresh();
    }
    public List<Tweet> getTimeline() {
        return homeTimeline;
    } 
    private void refresh() {
        Log.d(TAG, "refresh");
        if (twitterApiClient == null) {
            if (isAuthenticated()) {
                twitterApiClient = TwitterCore.getInstance().getApiClient();
            } else {
                return;
            }
        }
        StatusesService statusesService = twitterApiClient.getStatusesService();
        statusesService.homeTimeline(count, maxId, sinceId, trimUser, excludeReplies, 
            contributeDetails, includeEntities, new Callback<List<Tweet>>() {
            @Override
            public void success(Result<List<Tweet>> result) {
                homeTimeline.addAll(result.data);
                HashSet hs = new HashSet();
                hs.addAll(homeTimeline);
                homeTimeline.clear();
                homeTimeline.addAll(hs);
                Collections.sort(homeTimeline, new TweetComparator());
                if (homeTimeline.size() >= 1) {
                    Tweet last_tweet = homeTimeline.get(homeTimeline.size() - 1);
                    maxId = last_tweet.id;
                    Tweet new_tweet = homeTimeline.get(0);
                    sinceId = last_tweet.id;
                }
                sendTimeline();
            }

            public void failure(TwitterException exception) {
                Log.d(TAG, "TwitEx", exception);
                //Do something on failure
            }
        });
    }
    private void saveData() {
        String json = mGson.toJson(homeTimeline);
        preferences.edit().putString("timeline", json).apply();
        preferences.edit().putLong("maxid", maxId).apply();
        preferences.edit().putLong("sinceid", sinceId).apply();
    }
    private void sendTimeline() {
        // Code is shitty, fix later
        List<List<Tweet>> homeTimelineParts = Lists.partition(homeTimeline, 50);
        for (List<Tweet> part : homeTimelineParts) {
            String json = mGson.toJson(part);
            Log.d("TWITWEAR", json);
            sendMessage(WEAR_MESSAGE_PATH, json);
        }
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

    @Override
    protected void onResume() {
        super.onResume();
        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
