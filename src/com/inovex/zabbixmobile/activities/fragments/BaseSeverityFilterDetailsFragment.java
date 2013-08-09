package com.inovex.zabbixmobile.activities.fragments;

import android.app.Activity;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.inovex.zabbixmobile.R;
import com.inovex.zabbixmobile.adapters.BaseSeverityPagerAdapter;
import com.inovex.zabbixmobile.listeners.OnListItemSelectedListener;
import com.inovex.zabbixmobile.model.HostGroup;
import com.inovex.zabbixmobile.model.TriggerSeverity;
import com.viewpagerindicator.TitlePageIndicator;

public abstract class BaseSeverityFilterDetailsFragment<T> extends
		BaseServiceConnectedFragment {

	public static final String TAG = BaseSeverityFilterDetailsFragment.class
			.getSimpleName();

	private static final String ARG_ITEM_POSITION = "arg_item_position";
	private static final String ARG_ITEM_ID = "arg_item_id";
	private static final String ARG_SEVERITY = "arg_severity";
	private static final String ARG_SPINNER_VISIBLE = "arg_spinner_visible";
	
	protected ViewPager mDetailsPager;
	protected int mPosition = 0;
	protected long mItemId = 0;
	protected TriggerSeverity mSeverity = TriggerSeverity.ALL;
	protected TitlePageIndicator mDetailsPageIndicator;
	private OnListItemSelectedListener mCallbackMain;
	protected BaseSeverityPagerAdapter<T> mDetailsPagerAdapter;

	private long mHostGroupId = HostGroup.GROUP_ID_ALL;

	private boolean mLoadingSpinnerVisible;

	/**
	 * Selects an event which shall be displayed in the view pager.
	 * 
	 * @param position
	 *            list position
	 * @param severity
	 *            severity (this is used to retrieve the correct pager adapter
	 */
	public void selectItem(int position) {
		Log.d(TAG, "selectItem(" + position + ")");
		if (mDetailsPagerAdapter == null
				|| mDetailsPagerAdapter.getCount() == 0)
			return;
		if (position > mDetailsPagerAdapter.getCount() - 1)
			position = 0;
		setPosition(position);
		setCurrentItemId(mDetailsPagerAdapter.getItemId(position));
	}

	/**
	 * This sets the current position and updates the pager, pager adapter and
	 * page indicator.
	 * 
	 * @param position
	 */
	public void setPosition(int position) {
		this.mPosition = position;
		if (mDetailsPageIndicator != null) {
			mDetailsPageIndicator.setCurrentItem(position);
			mDetailsPager.setCurrentItem(position);
			mDetailsPagerAdapter.setCurrentPosition(position);
		}
	}

	private void setCurrentItemId(long itemId) {
		this.mItemId = itemId;
	}

	/**
	 * Sets the current severity and updates the pager adapter.
	 * 
	 * @param severity
	 *            current severity
	 */
	public void setSeverity(TriggerSeverity severity) {
		// exchange adapter if it's necessary
		// if(severity == this.mSeverity)
		// return;
		this.mSeverity = severity;
		if (mZabbixDataService != null) {
			retrievePagerAdapter();
			// the adapter could be fresh -> set fragment manager
			mDetailsPagerAdapter.setFragmentManager(getChildFragmentManager());
			mDetailsPager.setAdapter(mDetailsPagerAdapter);
		}
	}

	/**
	 * Sets the current host group id and updates the pager adapter.
	 * 
	 * @param hostGroupId
	 *            current severity
	 */
	public void setHostGroupId(long hostGroupId) {
		this.mHostGroupId = hostGroupId;
		// if (mZabbixDataService != null) {
		// retrievePagerAdapter();
		// // the adapter could be fresh -> set fragment manager
		// mDetailsPagerAdapter.setFragmentManager(getChildFragmentManager());
		// mDetailsPager.setAdapter(mDetailsPagerAdapter);
		// }
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		if (savedInstanceState != null) {
			mPosition = savedInstanceState.getInt(ARG_ITEM_POSITION, 0);
			mItemId = savedInstanceState.getLong(ARG_ITEM_ID, 0);
			mSeverity = TriggerSeverity.getSeverityByNumber(savedInstanceState
					.getInt(ARG_SEVERITY, TriggerSeverity.ALL.getNumber()));
			mLoadingSpinnerVisible = savedInstanceState.getBoolean(
					ARG_SPINNER_VISIBLE, false);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_severity_details,
				container, false);
		Log.d(TAG, "onCreateView");
		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (mLoadingSpinnerVisible)
			showLoadingSpinner();
		Log.d(TAG, "onViewCreated");
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		super.onServiceConnected(name, service);
		setupDetailsViewPager();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(ARG_ITEM_POSITION, mPosition);
		outState.putLong(ARG_ITEM_ID, mItemId);
		outState.putInt(ARG_SEVERITY, mSeverity.getNumber());
		outState.putBoolean(ARG_SPINNER_VISIBLE,
				mLoadingSpinnerVisible);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(TAG, "onAttach");

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try {
			mCallbackMain = (OnListItemSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnListItemSelectedListener.");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		// This is a fix for the issue with the child fragment manager;
		// described here:
		// http://stackoverflow.com/questions/15207305/getting-the-error-java-lang-illegalstateexception-activity-has-been-destroyed
		// and here: https://code.google.com/p/android/issues/detail?id=42601
		// If the fragment manager is not set to null, there will be issues when
		// the activity is destroyed and there are pending transactions
		mDetailsPagerAdapter.setFragmentManager(null);
	}

	/**
	 * 
	 */
	protected abstract void retrievePagerAdapter();

	/**
	 * Performs the setup of the view pager used to swipe between details pages.
	 */
	protected void setupDetailsViewPager() {
		Log.d(TAG, "setupViewPager");

		retrievePagerAdapter();
		mDetailsPagerAdapter.setFragmentManager(getChildFragmentManager());

		// initialize the view pager
		mDetailsPager = (ViewPager) getView().findViewById(
				R.id.severity_view_pager);
		mDetailsPager.setAdapter(mDetailsPagerAdapter);

		// Initialize the page indicator
		mDetailsPageIndicator = (TitlePageIndicator) getView().findViewById(
				R.id.severity_page_indicator);
		mDetailsPageIndicator.setViewPager(mDetailsPager);

		Log.d(TAG, "current position: " + mDetailsPagerAdapter.getCurrentPosition());
		mDetailsPager.setCurrentItem(mPosition);
		mDetailsPagerAdapter.setCurrentPosition(mPosition);
		mDetailsPageIndicator.setCurrentItem(mPosition);
		
//		mDetailsPageIndicator.setCurrentItem(mDetailsPagerAdapter.getCurrentPosition());
		
		mDetailsPageIndicator
				.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

					@Override
					public void onPageScrollStateChanged(int arg0) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onPageScrolled(int arg0, float arg1, int arg2) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onPageSelected(int position) {
						Log.d(TAG, "detail page selected: " + position);

						// propagate page change only if there actually was a
						// change -> prevent infinite propagation
						mDetailsPagerAdapter.setCurrentPosition(position);
						if (position != mPosition)
							mCallbackMain.onListItemSelected(position,
									mDetailsPagerAdapter.getItemId(position));
					}
				});
	}

	public void redrawPageIndicator() {
		if(mDetailsPageIndicator != null)
			mDetailsPageIndicator.invalidate();
	}
	
	/**
	 * Shows a loading spinner instead of this page's list view.
	 */
	public void showLoadingSpinner() {
		mLoadingSpinnerVisible = true;
		if (getView() != null) {
			LinearLayout progressLayout = (LinearLayout) getView()
					.findViewById(R.id.severity_details_progress_layout);
			if (progressLayout != null)
				progressLayout.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Dismisses the loading spinner view.
	 * 
	 * If the view has not yet been created, the status is saved and when the
	 * view is created, the spinner will not be shown at all.
	 */
	public void dismissLoadingSpinner() {
		mLoadingSpinnerVisible = false;
		if (getView() != null) {
			LinearLayout progressLayout = (LinearLayout) getView()
					.findViewById(R.id.severity_details_progress_layout);
			if (progressLayout != null) {
				progressLayout.setVisibility(View.GONE);
			}
		}

	}

}
