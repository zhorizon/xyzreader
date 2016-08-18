package com.example.xyzreader.ui;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.view.menu.ActionMenuItem;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
//    private static final float PARALLAX_FACTOR = 1.25f;

    private Context mContext;
    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private Toolbar mToolbar;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
//    private ObservableScrollView mScrollView;
//    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
//    private ColorDrawable mStatusBarColorDrawable;

//    private int mTopInset;
    private ImageView mPhotoView;
//    private int mScrollY;
//    private boolean mIsCard = false;
//    private int mStatusBarFullOpacityBottom;
    enum State {EXPANDED, COLLAPSED};
    private State mCurrentState = State.EXPANDED;
    private String mTitle = "";

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

//        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
//        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
//                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
//        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout)
//                mRootView.findViewById(R.id.draw_insets_frame_layout);
//        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
//            @Override
//            public void onInsetsChanged(Rect insets) {
//                mTopInset = insets.top;
//            }
//        });

//        mScrollView = (ObservableScrollView) mRootView.findViewById(R.id.scrollview);
//        mScrollView.setCallbacks(new ObservableScrollView.Callbacks() {
//            @Override
//            public void onScrollChanged() {
//                mScrollY = mScrollView.getScrollY();
//                getActivityCast().onUpButtonFloorChanged(mItemId, ArticleDetailFragment.this);
//                mPhotoContainerView.setTranslationY((int) (mScrollY - mScrollY / PARALLAX_FACTOR));
//                updateStatusBar();
//            }
//        });

        mContext = container.getContext();
        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);

        setTransitionName(mRootView.findViewById(R.id.meta_bar), "title_" + mItemId);
//        setTransitionName(mRootView.findViewById(R.id.fragment_root), "card_" + mItemId);

        mToolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
        getActivityCast().setSupportActionBar(mToolbar);
        getActivityCast().getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Not sure this a bug or not
        // When first time open the detail activity, the 'back' toolbar icon does not work, it does not trigger onOptionsItemSelected.
        // It works if swipe to right, or swipe 2 pages back to left...
        // Manually add the listener and delegate the onClick event.
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            final ActionMenuItem mNavItem = new ActionMenuItem(mToolbar.getContext(),
                    0, android.R.id.home, 0, 0, mTitle);
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(mNavItem);
            }
        });

        // clear the toolbar title, otherwise will default the activity name
        mToolbar.setTitle("");

        mCollapsingToolbarLayout = (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsing_toolbar);
//        mCollapsingToolbarLayout.setExpandedTitleColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));

//        mStatusBarColorDrawable = new ColorDrawable(0);
//
//        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
//                        .setType("text/plain")
//                        .setText("Some sample text")
//                        .getIntent(), getString(R.string.action_share)));
//            }
//        });

        // Set toolbar margin top as the status bar height
        int statusBarHeight = getStatusBarHeight();

        // not sure it is a bug that when the collapsingToolbarLayout with enterAlwaysCollapsed, the collapsed
        // height if missing the status bar height, here try to add the top margin to place the collapsed
        // toolbar under the status bar
        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) mToolbar.getLayoutParams();
        p.setMargins(p.leftMargin, statusBarHeight, p.rightMargin, p.bottomMargin);

        mCurrentState = State.EXPANDED;

        // Calculate ActionBar height
//        TypedValue tv = new TypedValue();
//        int actionBarHeight = 0;
//        if (mContext.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
//            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
//        }
        int actionBarHeight = mToolbar.getLayoutParams().height;
        final int finalActionBarHeight = actionBarHeight + statusBarHeight;

        // Change toolbar background color to solid color while the appBar scroll off screen,
        // so that can display the colored toolbar with title when enter back
        // Change toolbar background color to transparent while the appBar expand larger than the
        // toolbar height, animate the color change like fade out effect
        AppBarLayout appBarLayout = (AppBarLayout) mRootView.findViewById(R.id.appbar);
        if (appBarLayout != null) {

            appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                Boolean toolbarIsTransparent = true;

                @Override
                public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

                    if (verticalOffset == 0) {
                        mCurrentState = State.EXPANDED;
                    } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
                        mCurrentState = State.COLLAPSED;
                    }

//Log.d("test", "mCollapsingToolbarLayout.getHeight()=" + mCollapsingToolbarLayout.getHeight() + ", verticalOffset=" + verticalOffset + ", finalActionBarHeight=" + finalActionBarHeight + ", mToolbar.getY=" + mToolbar.getY());
//Log.d("test", "mCollapsingToolbarLayout.getBottom()=" + mCollapsingToolbarLayout.getBottom() + ", mToolbar.getBottom()=" + mToolbar.getBottom());
                    if ((mCollapsingToolbarLayout.getHeight() + verticalOffset <= finalActionBarHeight) && mCurrentState.equals(State.COLLAPSED)) {
                        if (toolbarIsTransparent) {
//Log.d("test", "sold color");
                            mToolbar.setBackgroundColor(ContextCompat.getColor(mContext, R.color.theme_primary));
                            mToolbar.setTitle(mTitle);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                mToolbar.setElevation(4);
                            }
                            toolbarIsTransparent = false;
//                        } else {
//                            mToolbar.setTranslationY(mCollapsingToolbarLayout.getHeight());
//Log.d("test", "toolbar height=" + mToolbar.getLayoutParams().height);
//                            if (mCollapsingToolbarLayout.getHeight() + verticalOffset > initialToolbarHeight) {
//                                int padding = mCollapsingToolbarLayout.getHeight() + verticalOffset - initialToolbarHeight;
//                                mToolbar.setPadding(0, padding, 0, 0);
//                                mToolbar.getLayoutParams().height = initialToolbarHeight + padding;
////                            }
                        }
                    } else if (!toolbarIsTransparent) {
                        mCurrentState = State.EXPANDED;

                        toolbarIsTransparent = true;
                        mToolbar.setTitle("");
                        mToolbar.setTranslationY(0);

                        // animate the toolbar background color to transparent
                        ArgbEvaluator evaluator = new ArgbEvaluator();
                        final ValueAnimator animator = new ValueAnimator();
                        animator.setIntValues(ContextCompat.getColor(mContext, R.color.theme_primary),
                                ContextCompat.getColor(mContext, android.R.color.transparent));
                        animator.setEvaluator(evaluator);
                        animator.setDuration(300);
                        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                int color = (int) animator.getAnimatedValue();
                                mToolbar.setBackgroundColor(color);
                            }
                        });
                        animator.start();
                    }
                    // Fix collapsingToolbar and toolbar reveal problem, sometimes it is found that the toolbar not able
                    // to cover the bottom of behind collapsingToolbar
//                    if (!toolbarIsTransparent && mCollapsingToolbarLayout.getBottom() != mToolbar.getBottom()) {
                    if (!toolbarIsTransparent) {
                        mToolbar.setTranslationY(mCollapsingToolbarLayout.getBottom() - mToolbar.getBottom());
                    }
//                    } else {
//                        mToolbar.setTranslationY(0);
//                    }
                }
            });
        }

        bindViews();
//        updateStatusBar();
        return mRootView;
    }

    private int getStatusBarHeight() {
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //    private void updateStatusBar() {
//        int color = 0;
//        if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {
//            float f = progress(mScrollY,
//                    mStatusBarFullOpacityBottom - mTopInset * 3,
//                    mStatusBarFullOpacityBottom - mTopInset);
//            color = Color.argb((int) (255 * f),
//                    (int) (Color.red(mMutedColor) * 0.9),
//                    (int) (Color.green(mMutedColor) * 0.9),
//                    (int) (Color.blue(mMutedColor) * 0.9));
//        }
//        mStatusBarColorDrawable.setColor(color);
//        mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);
//    }
//
//    static float progress(float v, float min, float max) {
//        return constrain((v - min) / (max - min), 0, 1);
//    }
//
//    static float constrain(float val, float min, float max) {
//        if (val < min) {
//            return min;
//        } else if (val > max) {
//            return max;
//        } else {
//            return val;
//        }
//    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            Log.d(TAG, "bindViews mCursor is not null");
//            mRootView.setAlpha(0);
//            mRootView.setVisibility(View.VISIBLE);
//            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.generate(bitmap, 12);
                                mMutedColor = p.getDarkMutedColor(0xFF333333);
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                ObjectAnimator.ofFloat(mPhotoView, "alpha", 0, 1).setDuration(300).start();
                                mRootView.findViewById(R.id.meta_bar).setBackgroundColor(mMutedColor);
//                                updateStatusBar();
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });

            // Set toolbar title
            mTitle = titleView.getText().toString();
//        } else {
//            mRootView.setVisibility(View.GONE);
//            titleView.setText("N/A");
//            bylineView.setText("N/A" );
//            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(TAG, "onCreateLoader");
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG, "onLoadFinished, cursor.count()=" + cursor.getCount() + ", cursor.getPosition()=" + cursor.getPosition());
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Log.d(TAG, "onLoaderReset");
        mCursor = null;
//        bindViews();
    }

//    public int getUpButtonFloor() {
//        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
//            return Integer.MAX_VALUE;
//        }
//
//        // account for parallax
//        return mIsCard
//                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
//                : mPhotoView.getHeight() - mScrollY;
//    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setTransitionName(View view, String name) {
        view.setTransitionName(name);
    }

}
