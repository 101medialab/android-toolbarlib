package com.hkm.advancedtoolbar.materialsearch;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.support.annotation.ColorInt;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.hkm.advancedtoolbar.R;
import com.hkm.advancedtoolbar.Util.AnimationUtil;
import com.hkm.advancedtoolbar.Util.SearchAdapter;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author Miguel Catalan Bañuls
 */
public class MaterialSearchView extends SearchViewBase {
    public static final int REQUEST_VOICE = 9999;

    private MenuItem mMenuItem;
    private boolean mIsSearchOpen = false;

    private boolean mClearingFocus;
    protected int inflate_layout;
    //Views
    protected View mSearchLayout;
    protected View mTintView;
    protected View mUnderLine;
    protected ListView mSuggestionsListView;
    protected EditText mSearchSrcTextView;
    protected ImageButton mBackBtn;
    protected ImageButton mVoiceBtn;
    protected ImageButton mEmptyBtn;
    protected RelativeLayout mSearchTopBar;

    protected CharSequence mOldQueryText;
    protected CharSequence mUserQuery;

    protected OnQueryTextListener mOnQueryChangeListener;
    protected SearchViewListener mSearchViewListener;

    private ListAdapter mAdapter;

    private SavedState mSavedState;

    protected boolean allowVoiceSearch;


    public MaterialSearchView(Context context) {
        this(context, null);
    }

    public MaterialSearchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        initiateView();
        initStyle(attrs, defStyleAttr);
    }

    @Override
    protected void initStyle(AttributeSet attrs, int defStyleAttr) {
        super.initStyle(attrs, defStyleAttr);
        TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.MaterialSearchView, defStyleAttr, 0);
        if (a != null) {

            if (a.hasValue(R.styleable.MaterialSearchView_material_layout)) {
       /*         inflate_layout = setLayout(a.getInt(R.styleable.MaterialSearchView_material_layout, 0));
            } else {*/
                inflate_layout = setLayout(0);
            }

            if (a.hasValue(R.styleable.SearchViewBase_searchOverlayColor)) {
                setOverLay(a.getColor(R.styleable.SearchViewBase_searchOverlayColor, 0));
            }

            if (a.hasValue(R.styleable.SearchViewBase_searchSuggestionBackground)) {
                setSuggestionBackground(a.getDrawable(R.styleable.SearchViewBase_searchSuggestionBackground));
            }

            a.recycle();
        }
    }


    protected int setLayout(int enum_layout) {
        switch (enum_layout) {
            case 0:
                return R.layout.search_view;
            case 1:
                return R.layout.material_search_ios;
            case 2:
                return R.layout.material_search_ios_classic;
            case 3:
                return R.layout.material_search_ios_simple;
            default:
                return R.layout.search_view;
        }
    }

    @Override
    protected void initiateView() {
        LayoutInflater.from(mContext).inflate(R.layout.search_view, this, true);
        mSearchLayout = findViewById(R.id.search_layout);

        mSearchTopBar = (RelativeLayout) mSearchLayout.findViewById(R.id.search_top_bar);
        mSuggestionsListView = (ListView) mSearchLayout.findViewById(R.id.suggestion_list);
        mSearchSrcTextView = (EditText) mSearchLayout.findViewById(R.id.searchTextView);
        mBackBtn = (ImageButton) mSearchLayout.findViewById(R.id.action_up_btn);
        mVoiceBtn = (ImageButton) mSearchLayout.findViewById(R.id.action_voice_btn);
        mEmptyBtn = (ImageButton) mSearchLayout.findViewById(R.id.action_empty_btn);
        mUnderLine = mSearchLayout.findViewById(R.id.search_view_underline);
        mTintView = mSearchLayout.findViewById(R.id.transparent_view);

        mSearchSrcTextView.setOnClickListener(mOnClickListener);
        mBackBtn.setOnClickListener(mOnClickListener);
        mVoiceBtn.setOnClickListener(mOnClickListener);
        mEmptyBtn.setOnClickListener(mOnClickListener);
        mTintView.setOnClickListener(mOnClickListener);

        allowVoiceSearch = false;

        showVoice(true);

        initSearchView();

        mSuggestionsListView.setVisibility(GONE);
    }

    protected void initSearchView() {
        mSearchSrcTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                onSubmitQuery();
                return true;
            }
        });

        mSearchSrcTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mUserQuery = s;
                startFilter(s);
                MaterialSearchView.this.onTextChanged(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mSearchSrcTextView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showKeyboard(mSearchSrcTextView);
                    showSuggestions();
                }
            }
        });
    }

    private void startFilter(CharSequence s) {
        if (mAdapter != null && mAdapter instanceof Filterable) {
            ((Filterable) mAdapter).getFilter().filter(s, MaterialSearchView.this);
        }
    }

    public final OnClickListener mOnClickListener = new OnClickListener() {

        public void onClick(View v) {
            if (v == mBackBtn) {
                backBtnClick();
            } else if (v == mVoiceBtn) {
                onVoiceClicked();
            } else if (v == mEmptyBtn) {
                searchClearClick();
            } else if (v == mSearchSrcTextView) {
                showSuggestions();
            } else if (v == mTintView) {
                tintViewClick();
            }
        }
    };

    protected void searchClearClick() {
        mSearchSrcTextView.setText(null);
    }

    protected void tintViewClick() {
        closeSearch();
    }

    protected void backBtnClick() {
        closeSearch();
    }

    protected void triggerQuery() {
        closeSearch();
        mSearchSrcTextView.setText(null);
    }

    public void setSearchOpen(boolean b) {
        mIsSearchOpen = b;
    }

    private void onVoiceClicked() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        //intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak an item name or number");    // user hint
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);    // setting recognition model, optimized for short phrases – search queries
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);    // quantity of results we want to receive
        if (mContext instanceof Activity) {
            ((Activity) mContext).startActivityForResult(intent, REQUEST_VOICE);
        }
    }

    private void onTextChanged(CharSequence newText) {
        CharSequence text = mSearchSrcTextView.getText();
        mUserQuery = text;
        boolean hasText = !TextUtils.isEmpty(text);
        if (hasText) {
            mEmptyBtn.setVisibility(VISIBLE);
            showVoice(false);
        } else {
            mEmptyBtn.setVisibility(GONE);
            showVoice(true);
        }

        if (mOnQueryChangeListener != null && !TextUtils.equals(newText, mOldQueryText)) {
            mOnQueryChangeListener.onQueryTextChange(newText.toString());
        }
        mOldQueryText = newText.toString();
    }


    private void onSubmitQuery() {
        CharSequence query = mSearchSrcTextView.getText();
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryChangeListener == null || !mOnQueryChangeListener.onQueryTextSubmit(query.toString())) {
                triggerQuery();
            }
        }
    }

    private boolean isVoiceAvailable() {
        if (isInEditMode()) {
            return false;
        }

        PackageManager pm = getContext().getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {
            return false;
        } else {
            return true;
        }
    }

    protected void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    protected void showKeyboard(View view) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1 && view.hasFocus()) {
            view.clearFocus();
        }
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, 0);
    }

    //Public Attributes

    @Override
    public void setBackground(Drawable background) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mSearchTopBar.setBackground(background);
        } else {
            mSearchTopBar.setBackgroundDrawable(background);
        }
    }

    @Override
    public void setBackgroundColor(@ColorInt int color) {
        mSearchTopBar.setBackgroundColor(color);
    }

    @Override
    public void setOverLay(@ColorInt int color) {
        mTintView.setBackgroundColor(color);
    }

    @Override
    public void setTextColor(@ColorInt int color) {
        mSearchSrcTextView.setTextColor(color);
    }

    @Override
    public void setHintTextColor(int color) {
        mSearchSrcTextView.setHintTextColor(color);
    }


    @Override
    public void setHint(CharSequence hint) {
        mSearchSrcTextView.setHint(hint);
    }

    @Override
    public void setVoiceIcon(Drawable drawable) {
        mVoiceBtn.setImageDrawable(drawable);
    }

    @Override
    public void setCloseIcon(Drawable drawable) {
        mEmptyBtn.setImageDrawable(drawable);
    }

    @Override
    public void setBackIcon(Drawable drawable) {
        mBackBtn.setImageDrawable(drawable);
    }

    @Override
    public void setSuggestionBackground(Drawable background) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mSuggestionsListView.setBackground(background);
        } else {
            mSuggestionsListView.setBackgroundDrawable(background);
        }
    }

    public void setUnderLineVisibility(int visibility) {
        mUnderLine.setVisibility(visibility);
    }

    public void setCursorDrawable(int drawable) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            mSearchSrcTextView.setTextCursorDrawable(drawable);
        } else {
            try {
                // https://github.com/android/platform_frameworks_base/blob/kitkat-release/core/java/android/widget/TextView.java#L562-564
                Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
                f.setAccessible(true);
                f.set(mSearchSrcTextView, drawable);
            } catch (Exception ignored) {
                Log.e("MaterialSearchView", ignored.toString());
            }
        }
    }

    public void setVoiceSearch(boolean voiceSearch) {
        allowVoiceSearch = voiceSearch;
    }

    //Public Methods

    /**
     * Call this method to show suggestions list. This shows up when adapter is set. Call {@link #setAdapter(ListAdapter)} before calling this.
     */
    public void showSuggestions() {
        if (mAdapter != null && mAdapter.getCount() > 0 && mSuggestionsListView.getVisibility() == GONE) {
            mSuggestionsListView.setVisibility(VISIBLE);
        }
    }

    /**
     * Set Suggest List OnItemClickListener
     *
     * @param listener OnItemClickListener listener
     */
    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        mSuggestionsListView.setOnItemClickListener(listener);
    }

    /**
     * Set Adapter for suggestions list. Should implement Filterable.
     *
     * @param adapter adapter
     */
    public void setAdapter(ListAdapter adapter) {
        mAdapter = adapter;
        mSuggestionsListView.setAdapter(adapter);
        startFilter(mSearchSrcTextView.getText());
    }

    /**
     * Set Adapter for suggestions list with the given suggestion array
     *
     * @param suggestions array of suggestions
     */
    public void setSuggestions(String[] suggestions) {
        if (suggestions != null && suggestions.length > 0) {
            final SearchAdapter adapter = new SearchAdapter(mContext, suggestions);
            setAdapter(adapter);

            setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    setQuery((String) adapter.getItem(position), false);
                }
            });
        }
    }

    /**
     * Dismiss the suggestions list.
     */
    public void dismissSuggestions() {
        if (mSuggestionsListView.getVisibility() == VISIBLE) {
            mSuggestionsListView.setVisibility(GONE);
        }
    }


    /**
     * Calling this will set the query to search text box. if submit is true, it'll submit the query.
     *
     * @param query  the input for search query
     * @param submit bool with auto submission
     */
    public void setQuery(CharSequence query, boolean submit) {
        mSearchSrcTextView.setText(query);
        if (query != null) {
            mSearchSrcTextView.setSelection(mSearchSrcTextView.length());
            mUserQuery = query;
        }
        if (submit && !TextUtils.isEmpty(query)) {
            onSubmitQuery();
        }
    }

    /**
     * if show is true, this will enable voice search. If voice is not available on the device, this method call has not effect.
     *
     * @param show bool for show
     */
    public void showVoice(boolean show) {
        if (show && isVoiceAvailable() && allowVoiceSearch) {
            mVoiceBtn.setVisibility(VISIBLE);
        } else {
            mVoiceBtn.setVisibility(GONE);
        }
    }

    /**
     * Call this method and pass the menu item so this class can handle click events for the Menu Item.
     *
     * @param menuItem item menu
     */
    public void setMenuItem(MenuItem menuItem) {
        this.mMenuItem = menuItem;
        mMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showSearch();
                return true;
            }
        });
    }

    /**
     * Return true if search is open
     *
     * @return bool
     */
    public boolean isSearchOpen() {
        return mIsSearchOpen;
    }

    /**
     * Open Search View. This will animate the showing of the view.
     */
    public void showSearch() {
        showSearch(true);
    }

    /**
     * Open Search View. if animate is true, Animate the showing of the view.
     *
     * @param animate bool for animation
     */
    public void showSearch(boolean animate) {
        if (isSearchOpen()) {
            return;
        }

        //Request Focus
        mSearchSrcTextView.setText(null);
        mSearchSrcTextView.requestFocus();

        if (animate) {
            AnimationUtil.fadeInView(mSearchLayout, AnimationUtil.ANIMATION_DURATION_MEDIUM, new AnimationUtil.AnimationListener() {
                @Override
                public boolean onAnimationStart(View view) {
                    return false;
                }

                @Override
                public boolean onAnimationEnd(View view) {
                    if (mSearchViewListener != null) {
                        mSearchViewListener.onSearchViewShown();
                    }
                    return false;
                }

                @Override
                public boolean onAnimationCancel(View view) {
                    return false;
                }
            });
        } else {
            mSearchLayout.setVisibility(VISIBLE);
            if (mSearchViewListener != null) {
                mSearchViewListener.onSearchViewShown();
            }
        }
        mIsSearchOpen = true;
    }

    /**
     * Close search view.
     */
    public void closeSearch() {
        if (!isSearchOpen()) {
            return;
        }

        mSearchSrcTextView.setText(null);
        dismissSuggestions();
        clearFocus();

        mSearchLayout.setVisibility(GONE);
        if (mSearchViewListener != null) {
            mSearchViewListener.onSearchViewClosed();
        }
        mIsSearchOpen = false;

    }

    /**
     * Set this listener to listen to Query Change events.
     *
     * @param listener the listener
     */
    public void setOnQueryTextListener(OnQueryTextListener listener) {
        mOnQueryChangeListener = listener;
    }

    /**
     * Set this listener to listen to Search View open and close events
     *
     * @param listener the listener
     */
    public void setOnSearchViewListener(SearchViewListener listener) {
        mSearchViewListener = listener;
    }

    @Override
    public void onFilterComplete(int count) {
        if (count > 0) {
            showSuggestions();
        } else {
            dismissSuggestions();
        }
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        // Don't accept focus if in the middle of clearing focus
        if (mClearingFocus) return false;
        // Check if SearchView is focusable.
        if (!isFocusable()) return false;
        return mSearchSrcTextView.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    public void clearFocus() {
        mClearingFocus = true;
        hideKeyboard(this);
        super.clearFocus();
        mSearchSrcTextView.clearFocus();
        mClearingFocus = false;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        //begin boilerplate code that allows parent classes to save state
        Parcelable superState = super.onSaveInstanceState();
        mSavedState = new SavedState(superState);
        //end
        mSavedState.query = mUserQuery != null ? mUserQuery.toString() : null;
        mSavedState.isSearchOpen = this.mIsSearchOpen;
        return mSavedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        //begin boilerplate code so parent classes can restore state
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        mSavedState = (SavedState) state;

        if (mSavedState.isSearchOpen) {
            showSearch(false);
            setQuery(mSavedState.query, false);
        }

        super.onRestoreInstanceState(mSavedState.getSuperState());
    }

    public void fromActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MaterialSearchView.REQUEST_VOICE) {
            Log.d("check", data.toString());
            String query = data.getStringExtra(SearchManager.QUERY);
            //  searchView.setS
            if (query == null) return;
            if (query.isEmpty()) return;
            mSearchSrcTextView.setText(query);
        }
    }

}