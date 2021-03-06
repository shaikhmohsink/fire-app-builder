/**
 * This file was modified by Amazon:
 * Copyright 2015-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.amazon.android.tv.tenfoot.ui.fragments;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.helper.AuthHelper;
import com.amazon.android.model.Action;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.presenter.CardPresenter;
import com.amazon.android.tv.tenfoot.presenter.CustomListRowPresenter;
import com.amazon.android.tv.tenfoot.presenter.SettingsCardPresenter;
import com.amazon.android.tv.tenfoot.ui.activities.ContentDetailsActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowHeaderPresenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import java.util.List;

/**
 * Main class to show ContentBrowseFragment with header and rows of content.
 */
public class FullContentBrowseFragment extends BrowseFragment {

    private static final String TAG = FullContentBrowseFragment.class.getSimpleName();

    private ArrayObjectAdapter mRowsAdapter;
    private Drawable mDefaultBackground;
    private BackgroundManager mBackgroundManager;
    private ArrayObjectAdapter settingsAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);
        EventBus.getDefault().register(this);
        mRowsAdapter = new ArrayObjectAdapter(new CustomListRowPresenter());
        addSettingsActionsToRowAdapter(mRowsAdapter);
        loadContents();
        setAdapter(mRowsAdapter);

        prepareBackgroundManager();
        setupUIElements();
        setupEventListeners();
    }

    private void prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
    }

    private void setupUIElements() {
        // Set custom badge drawable and title here but note that when badge is set title does
        // not show so we have setTitle commented out.
        setBadgeDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.company_logo));
        //setTitle(getString(R.string.browse_title));

        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // Set headers and rows background color
        setBrandColor(getResources().getColor(R.color.browse_headers_bar));
        setDefaultBackground(R.drawable.default_background);
        mBackgroundManager.setDrawable(ContextCompat.getDrawable(getActivity(), R.drawable
                .browse_background_no_preview));

        // Disables the scaling of rows when Headers bar is in open state.
        enableRowScaling(false);

        // Set search icon color
        setSearchAffordanceColor(getResources().getColor(R.color.search_orb));

        // Here is where a header presenter can be set to customize the look
        // of the headers list.
        setHeaderPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object o) {

                return new RowHeaderPresenter();
            }
        });
    }

    private void loadContents() {

        Log.i(TAG, "Loading contents...");

        ContentContainer rootContentContainer = ContentBrowser.getInstance(getActivity())
                                                              .getRootContentContainer();

        CardPresenter cardPresenter = new CardPresenter();

        for (ContentContainer contentContainer : rootContentContainer.getContentContainers()) {

            HeaderItem header = new HeaderItem(0, contentContainer.getName());
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);

            for (ContentContainer innerContentContainer : contentContainer.getContentContainers()) {
                listRowAdapter.add(innerContentContainer);
            }

            for (Content content : contentContainer.getContents()) {
                listRowAdapter.add(content);
            }

            mRowsAdapter.add(mRowsAdapter.size() - 1, new ListRow(header, listRowAdapter));
        }
    }

    private void addSettingsActionsToRowAdapter(ArrayObjectAdapter arrayObjectAdapter) {

        List<Action> settings = ContentBrowser.getInstance(getActivity())
                                              .getSettingsActions();

        if (settings != null && !settings.isEmpty()) {

            SettingsCardPresenter cardPresenter = new SettingsCardPresenter();
            settingsAdapter = new ArrayObjectAdapter(cardPresenter);

            for (Action item : settings) {
                settingsAdapter.add(item);
            }
        }
        else {
            Log.d(TAG, "No settings were found");
        }

        if (settingsAdapter != null) {
            // Create settings header and row.
            HeaderItem header = new HeaderItem(0, getString(R.string.settings_title));
            arrayObjectAdapter.add(0, new ListRow(header, settingsAdapter));
        }
    }

    /**
     * /**
     * Event bus listener method to listen for authentication updates from AUthHelper and update
     * the login action status in settings.
     *
     * @param authenticationStatusUpdateEvent Broadcast event for update in authentication status.
     */
    @Subscribe
    public void onAuthenticationStatusUpdateEvent(AuthHelper.AuthenticationStatusUpdateEvent
                                                          authenticationStatusUpdateEvent) {

        if (settingsAdapter != null) {
            settingsAdapter.notifyArrayItemRangeChanged(0, settingsAdapter.size());
        }
    }

    private void setupEventListeners() {

        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                ContentBrowser.getInstance(getActivity())
                              .switchToScreen(ContentBrowser.CONTENT_SEARCH_SCREEN);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    protected void setDefaultBackground(Drawable background) {

        mDefaultBackground = background;
    }

    protected void setDefaultBackground(int resourceId) {

        mDefaultBackground = ContextCompat.getDrawable(getActivity(), resourceId);
    }

    protected void updateBackground(Drawable drawable) {

        BackgroundManager.getInstance(getActivity()).setDrawable(drawable);
    }

    protected void clearBackground() {

        BackgroundManager.getInstance(getActivity()).setDrawable(mDefaultBackground);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {

        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Content) {
                Content content = (Content) item;
                Log.d(TAG, "Content clicked: " + item.toString());

                View imageView = ((ImageCardView) itemViewHolder.view).getMainImageView();

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        imageView,
                        ContentDetailsActivity.SHARED_ELEMENT_NAME).toBundle();

                ContentBrowser.getInstance(getActivity())
                              .setLastSelectedContent(content)
                              .switchToScreen(ContentBrowser.CONTENT_DETAILS_SCREEN, bundle);
            }
            else if (item instanceof ContentContainer) {
                ContentContainer contentContainer = (ContentContainer) item;
                Log.d(TAG, "ContentContainer with name " + contentContainer.getName() + " was " +
                        "clicked");

                ContentBrowser.getInstance(getActivity())
                              .setLastSelectedContentContainer(contentContainer)
                              .switchToScreen(ContentBrowser.CONTENT_SUBMENU_SCREEN);
            }
            else if (item instanceof Action) {
                Action settingsItemModel = (Action) item;
                Log.d(TAG, "Settings with title " + settingsItemModel.getAction() + " was clicked");
                ContentBrowser.getInstance(getActivity())
                              .settingsActionTriggered(getActivity(), settingsItemModel);
            }
        }
    }
}
