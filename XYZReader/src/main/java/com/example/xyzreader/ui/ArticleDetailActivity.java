package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.transition.AutoTransition;
import android.transition.Explode;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.animation.Interpolator;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    final private String TAG = "ArticleDetailActivity";
    private Cursor mCursor;
    private long mStartId;

//    private long mSelectedItemId;
//    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
//    private int mTopInset;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
//    private View mUpButtonContainer;
//    private View mUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            getWindow().getDecorView().setSystemUiVisibility(
//                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
//                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//        }

        // Postpone the shared element enter transition
        ActivityCompat.postponeEnterTransition(this);

        setContentView(R.layout.activity_article_detail);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

//        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
//            @Override
//            public void onPageScrollStateChanged(int state) {
//                super.onPageScrollStateChanged(state);
//                mUpButton.animate()
//                        .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
//                        .setDuration(300);
//            }
//
//            @Override
//            public void onPageSelected(int position) {
//                if (mCursor != null) {
//                    mCursor.moveToPosition(position);
//                }
//                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
//                updateUpButtonPosition();
//            }
//        });

//        mUpButtonContainer = findViewById(R.id.up_container);

//        mUpButton = findViewById(R.id.action_up);
//        mUpButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                onSupportNavigateUp();
//            }
//        });

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            mUpButtonContainer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
//                @Override
//                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
//                    view.onApplyWindowInsets(windowInsets);
//                    mTopInset = windowInsets.getSystemWindowInsetTop();
//                    mUpButtonContainer.setTranslationY(mTopInset);
//                    updateUpButtonPosition();
//                    return windowInsets;
//                }
//            });
//        }

        // CoordinatorLayout status bar padding disappears from ViewPager 2nd page?!
        ViewCompat.setOnApplyWindowInsetsListener(mPager, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                insets = ViewCompat.onApplyWindowInsets(v, insets);
                if (insets.isConsumed()) {
                    return insets;
                }

                boolean consumed = false;
                for (int i = 0, count = mPager.getChildCount(); i < count; i++) {
                    ViewCompat.dispatchApplyWindowInsets(mPager.getChildAt(i), insets);
                    if (insets.isConsumed()) {
                        consumed = true;
                    }
                }
                return consumed ? insets.consumeSystemWindowInsets() : insets;
            }
        });

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
//                mSelectedItemId = mStartId;
            }
        }

        // Set activity transition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setAllowEnterTransitionOverlap(false);

            Transition enterTransition = new AutoTransition();
            getWindow().setEnterTransition(enterTransition);
            enterTransition.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {
                    // hide the share FAB
                    ArticleDetailFragment f = (ArticleDetailFragment) mPagerAdapter.getCurrentFragment();
                    View fab = f.getView().findViewById(R.id.share_fab);
                    fab.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    // show share FAB
                    ArticleDetailFragment f = (ArticleDetailFragment) mPagerAdapter.getCurrentFragment();
                    FloatingActionButton fab = (FloatingActionButton) f.getView().findViewById(R.id.share_fab);
                    fab.show();
                }

                @Override
                public void onTransitionCancel(Transition transition) { }

                @Override
                public void onTransitionPause(Transition transition) { }

                @Override
                public void onTransitionResume(Transition transition) { }
            });

            Transition returnTransition = TransitionInflater.from(this).inflateTransition(R.transition.activity_detail_return);
            getWindow().setReturnTransition(returnTransition);

            getWindow().setSharedElementReturnTransition(null);
            getWindow().setSharedElementsUseOverlay(false);
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    mPager.setCurrentItem(mCursor.getPosition(), false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }

        scheduleStartPostponedTransition(mPager);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

//    public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
//        if (itemId == mSelectedItemId) {
//            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
//            updateUpButtonPosition();
//        }
//    }

//    private void updateUpButtonPosition() {
//        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
//        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
//    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        private Fragment mCurrentFragment;

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (getCurrentFragment() != object) {
                mCurrentFragment = (Fragment) object;
            }
            super.setPrimaryItem(container, position, object);
        }

        public Fragment getCurrentFragment() {
            return mCurrentFragment;
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(TAG, "getItem position: " + position);
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }

    private void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                ActivityCompat.startPostponedEnterTransition(ArticleDetailActivity.this);
                return true;
            }
        });
    }
}
