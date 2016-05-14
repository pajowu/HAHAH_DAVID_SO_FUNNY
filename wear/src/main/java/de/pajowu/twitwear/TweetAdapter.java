package de.pajowu.twitwear;
 
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
 
import java.util.List;
import com.twitter.sdk.android.core.models.Tweet;
import android.util.Log;
public class TweetAdapter extends RecyclerView.Adapter<TweetAdapter.MyViewHolder> {
 
    private List<Tweet> tweetList;
 
    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView tweet, user;
 
        public MyViewHolder(View view) {
            super(view);
            tweet = (TextView) view.findViewById(R.id.tweet);
            user = (TextView) view.findViewById(R.id.user);
        }
    }
 
 
    public TweetAdapter(List<Tweet> tweetList) {
        this.tweetList = tweetList;
    }
 
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tweet_list_row, parent, false);
 
        return new MyViewHolder(itemView);
    }
 
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Tweet tweet = tweetList.get(position);
        holder.tweet.setText(tweet.text);
        holder.user.setText(tweet.user.name);
    }
 
    @Override
    public int getItemCount() {
        return tweetList.size();
    }
}