package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private final String LOG_TAG = "LIST_ACTIVITY";
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Adapter mAdapter;
    private boolean mOnStartRefreshing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Hide toolbar title
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Add onRefresh handling on SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(LOG_TAG, "onRefresh called from SwipeRefreshLayout");
                refresh();
            }
        });

        // Add recycler adapter
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        mAdapter = new Adapter();
        mAdapter.setHasStableIds(true);
        recyclerView.setAdapter(mAdapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(sglm);

        // Set transition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setReenterTransition(null);
        }

        // Here we want to show the UI first, then display a refreshing icon to indicate we are
        // refreshing data when firstly open the activity
        // However, I found that if calling initLoader here, it will call the loader callback
        // onLoadFinished, but I to refresh the data first and not to show anything.
        // I cannot find any good place to do it, so delay to initLoader after Service boardcast
        // receiver is called for data update.
        if (savedInstanceState == null) {
            // Will refresh data later
            mOnStartRefreshing = true;
        } else {
            // Load data from database
            getLoaderManager().initLoader(0, null, this);
        }
    }

    @Override
    protected void onStart() {
        Log.d(LOG_TAG, "onStart, register receiver");
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));

        if (mOnStartRefreshing) {
            mOnStartRefreshing = false;
            // Delay the call here, or it will not show the refreshing indicator
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                    refresh();
                }
            });
        }
    }

    @Override
    protected void onStop() {
        Log.d(LOG_TAG, "onStop, unregister receiver");
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private void refresh() {
        Log.d(LOG_TAG, "refresh");
        startService(new Intent(this, UpdaterService.class));
    }

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        private boolean mLastRefreshingStatus = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                Log.d(LOG_TAG, "onReceive, receive service result");
                // I not not sure this method will be called after registerReceiver, but definitely it
                // is being called twice when the first time create the activity.
                // I want to skip the first time call, so do something to detect this method is called
                // really from the service boardcast.
                // Under this condition, I assume it is called from service boardcast:
                // The service will boardcast two times, one when start refreshing, one after refresh done
                if (mLastRefreshingStatus && !intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false)) {
                    if (getLoaderManager().getLoader(0) == null) {
                    Log.d(LOG_TAG, "onReceive, initLoader");
                        getLoaderManager().initLoader(0, null, ArticleListActivity.this);
                    }
                }
                mLastRefreshingStatus = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
            }
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(LOG_TAG, "onCreateLoader, Create loader");
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(LOG_TAG, "onLoadFinished, Cursor loader finished");
        mAdapter.changeCursor(cursor);
        mAdapter.notifyDataSetChanged();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter() {};

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityOptionsCompat options = ActivityOptionsCompat.
                            makeSceneTransitionAnimation(ArticleListActivity.this,
                                    Pair.create(vh.titleContainer, getTransitionName(vh.titleContainer))
//                                    Pair.create(vh.cardContainer, getTransitionName(vh.cardContainer))
                            );
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))),
                            options.toBundle());
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

            // Assign a unique transition name for each card
//            setTransitionName(holder.cardContainer, "card_" + getItemId(position));
            setTransitionName(holder.titleContainer, "title_" + getItemId(position));
        }

        @Override
        public int getItemCount() {
            return mCursor == null ? 0 : mCursor.getCount();
        }

        public void changeCursor(Cursor cursor) {
            mCursor = cursor;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;
        public View titleContainer;
        public View cardContainer;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
            titleContainer = view.findViewById(R.id.title_container);
            cardContainer = view.findViewById(R.id.card);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private String getTransitionName(View view) { return view.getTransitionName(); }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setTransitionName(View view, String name) {
        view.setTransitionName(name);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                Log.d(LOG_TAG, "Refresh menu item selected");
                mSwipeRefreshLayout.setRefreshing(true);
                refresh();
                return true;
        }

        // User didn't trigger a refresh, let the superclass handle this action
        return super.onOptionsItemSelected(item);
    }
}
