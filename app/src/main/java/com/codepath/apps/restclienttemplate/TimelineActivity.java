package com.codepath.apps.restclienttemplate;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.codepath.apps.restclienttemplate.models.Tweet;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class TimelineActivity extends AppCompatActivity {

    private TwitterClient client;
    private RecyclerView rvTweets;
    private TweetsAdapter adapter;
    private List<Tweet> tweets;

    private LinearLayoutManager linearLayoutManager;

    private SwipeRefreshLayout swipeContainer;

    private EndlessRecyclerViewScrollListener scrollListener;

    private long lastTweetSeen;
    private JsonHttpResponseHandler jsonHttpResponseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        client=TwitterApp.getRestClient(this);

        swipeContainer=findViewById(R.id.swipeContainer);

        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        rvTweets=findViewById(R.id.rvTweets);

        tweets=new ArrayList<>();
        adapter=new TweetsAdapter(this,tweets);
        linearLayoutManager=new LinearLayoutManager(this);

        rvTweets.setLayoutManager(linearLayoutManager);
        rvTweets.setAdapter(adapter);

        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                loadMoreData();
            }
        };
        // Adds the scroll listener to RecyclerView
        rvTweets.addOnScrollListener(scrollListener);

        jsonHttpResponseHandler=new JsonHttpResponseHandler(){

        };
        populateHomeTimeline();

        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d("TwitterClient","content is being refreshed");
                populateHomeTimeline();
            }
        });
    }

    // this is where we will make another API call to get the next page of tweets and add the objects to our current list of tweets
    public void loadMoreData() {
        //Log.d("LoadMore","loading more data");
        client.getNextPageOfTweets(new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                List<Tweet> tweetsToAdd=new ArrayList<>();
                adapter.notifyDataSetChanged();
                scrollListener.resetState();

                for(int i=0;i<response.length();i++){
                    try {
                        JSONObject jsonTweetObject=response.getJSONObject(i);
                        Tweet tweet=Tweet.fromJson(jsonTweetObject);
                        lastTweetSeen=tweet.uid;
                        //Log.d("tweetid","id is "+lastTweetSeen);
                        tweetsToAdd.add(tweet);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    adapter.clear();
                    adapter.addTweets(tweetsToAdd);
                    lastTweetSeen=tweetsToAdd.get(tweetsToAdd.size()-1).uid;
                    //Log.d("tweetid","lastTweetSeen is "+lastTweetSeen);
                    swipeContainer.setRefreshing(false);
                }
                //Log.d("LoadMore","Success to load more data");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.e("LoadMore","Fail to load more data");
            }
        },lastTweetSeen-1);
    }

    private void populateHomeTimeline() {
        client.getHomeTimeline(new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                //Log.d("TwitterClient",response.toString());
                List<Tweet> tweetsToAdd=new ArrayList<>();

                for(int i=0;i<response.length();i++){
                    try {
                        JSONObject jsonTweetObject=response.getJSONObject(i);
                        Tweet tweet=Tweet.fromJson(jsonTweetObject);
                        lastTweetSeen=tweet.uid;
                        Log.d("tweetid","id is "+lastTweetSeen);
                        tweetsToAdd.add(tweet);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    adapter.clear();
                    adapter.addTweets(tweetsToAdd);
                    lastTweetSeen=tweetsToAdd.get(tweetsToAdd.size()-1).uid;
                    //Log.d("tweetid","lastTweetSeen is "+lastTweetSeen);
                    swipeContainer.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.e("TwitterClient",responseString);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.e("TwitterClient",errorResponse.toString());
            }
        });
    }
}
