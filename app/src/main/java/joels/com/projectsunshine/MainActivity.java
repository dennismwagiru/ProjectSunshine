package joels.com.projectsunshine;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.net.URL;

import joels.com.projectsunshine.ForecastAdapter.ForecastAdapterOnClickHandler;
import joels.com.projectsunshine.data.SunshinePreferences;
import joels.com.projectsunshine.utilities.NetworkUtils;
import joels.com.projectsunshine.utilities.OpenWeatherJsonUtils;

public class MainActivity extends AppCompatActivity implements ForecastAdapterOnClickHandler,
        LoaderCallbacks<String[]>, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private ForecastAdapter forecastAdapter;
    private TextView mErrorMessageDisplay;
    private ProgressBar mLoadingIndicator;

    private static final int FORECAST_LOADER_ID = 0;

    private static boolean PREFERENCES_HAVE_BEEN_UPDATED = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        mRecyclerView = findViewById(R.id.recyclerview_forecast);

        mErrorMessageDisplay = findViewById(R.id.tv_error_message_display);

        /*
        * The ProgressBar that will indicate to the user that we are loading data. It will be
        * hidden when no data is loading.
        *
        * Please note: This so called "ProgressBar" isn't a bar by default. It is more of a circle.
        * We didn't make rules (or the names of Views), we just follow them
         */
        mLoadingIndicator = findViewById(R.id.pb_loading_indicator);

        /*
         * LinearLayoutManager can support HORIZONTAL or VERTICAL orientations. The reverse layout
         * parameter is useful mostly for HORIZONTAL layouts that should reverse for right to left
         * languages.
         */
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        /*
         * Use this setting to improve performance if you know that changes in content do not
         * change the child layout size in the RecyclerView
         */
        mRecyclerView.setHasFixedSize(true);

        /*
         * The ForecastAdapter is responsible for linking our weather data with the Views that
         * will end up displaying our weather data.
         */
        forecastAdapter = new ForecastAdapter(this, this);

        /* Setting the adapter attaches it to the RecyclerView in our layout. */
        mRecyclerView.setAdapter(forecastAdapter);

        /**
         * This ID will uniquely identify the Loader. We can use it, for example, to get a handle
         * on our Loader at a later point in time through the support LoadManager.
         */
        int loaderId = FORECAST_LOADER_ID;

        /**
         * From MainActivity, we have implemented the LoaderCallbacks interface with the type of
         * String array. (implements LoaderCallbacks<String[]>) The variable callback is passed
         * something to notify us of, it will do so through this callback.
         */
        LoaderCallbacks<String[]> callback = MainActivity.this;

        /**
         * The second parameter of the initLoader method below is a Bundle. Optionally, you can
         * pass a Bundle to initLoader that you can then access from within the onCreateLoader
         * callback. In our case, we don't actually use the Bundle, but it's here in case we wanted
         * to.
         */
        Bundle bundleForLoader = null;

        /**
         * Ensures a loader is initialized and active. If the loader doesn't already exist, one is
         * created and (if the activity/fragment is currently started) starts with the loader. Otherwise
         * the last created loader is re-used.
         */
        getSupportLoaderManager().initLoader(loaderId, bundleForLoader, callback);

        Log.d(TAG, "onCreate: registering preference changed listener");

        /*
         * Register MainActivity as an OnPreferenceChangedListener to receive a callback when a
         * SharedPreference has changed. Please note that we must unregister MainActivity as an
         * OnSharedPreferenceChanged listener in onDestroy to avoid any memory leaks.
         */

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * This method is overridden by our MainActivity class in order to handle RecyclerView item
     * clicks.
     *
     * @param weatherForDay The weather for the day that was clicked
     */
    @Override
    public void onClick(String weatherForDay) {
        Context context = this;
        Class destinationClass = DetailActivity.class;
        Intent intentToStartDetailClass = new Intent(context, destinationClass);
        intentToStartDetailClass.putExtra(Intent.EXTRA_TEXT, weatherForDay);
        startActivity(intentToStartDetailClass);
    }

    /*
     * This method will hide the weather data and show the error message
     *
     * Since it is okay to redundantly set the visibility of a view, we don't need to check whether
     * each view is curently visible or invisible.
     */
    private void showErrorMessage() {
        /* First, hide the currently visible data */
        mRecyclerView.setVisibility(View.INVISIBLE);

        /* Then, show the error */
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }

    /**
     * OnStart is called when the Activity is coming into view. This happens when the Activity is
     * first created, but also happens when the Activity is returned to from another Activity. We
     * are going to use the fact that onStart is called when the user returns to this Activity to
     * check if the location setting or the preferred units setting has changed. If it has changed,
     * we are going to perform a new query.
     */
    @Override
    protected void onStart() {
        super.onStart();
        /*
         * If the preferences for location or units have changed since the user was last in
         * MainActivity, perform another query and set the flag to false.
         *
         * This isn't the ideal solution because there really isn't a need to perform another
         * GET request just to change the units, but this is the simplest solution that gets the
         * job done for now. Later in this course, we are going to show you more elegant ways to
         * handle converting the units from celsius to fahrenheit and back without hitting the
         * network again by keeping a copy of the data in a manageable format.
         */
        if (PREFERENCES_HAVE_BEEN_UPDATED) {
            Log.d(TAG, "onStart: preferences were updated");
            getSupportLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
            PREFERENCES_HAVE_BEEN_UPDATED = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    /*
    * This method will hide the error message and show the weather data
    */
    private void showWeatherDataView() {
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id The ID whose loader is to be created.
     * @param loaderArgs Any arguments supplied by the caller.
     *
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<String[]> onCreateLoader(int id, final Bundle loaderArgs) {
        return new AsyncTaskLoader<String[]>(this) {

            /* This String array will hold and help cache our weather data */
            String[] mWeatherData = null;

            /**
             * Subclasses of AsyncTaskLoader must implement this to tale care of loading their data.
             * */

            @Override
            protected void onStartLoading() {
                if (mWeatherData != null){
                    deliverResult(mWeatherData);
                } else {
                    mLoadingIndicator.setVisibility(View.VISIBLE);
                    forceLoad();
                }
            }

            /**
             * This is the method of the AsncTaskLoader that will load and parse the JSON data
             * from OpenWeatherMap in the background.
             *
             * @retun Weather data from OpenWeatherMap as an array of Strings.
             *
             */
            @Override
            public String[] loadInBackground() {
                String locationQuery = SunshinePreferences.getPreferredWeatherLocation(MainActivity.this);
                URL weatherRequestUrl = NetworkUtils.buildUrl(locationQuery);
                try {
                    String jsonWeatherResponse = NetworkUtils.getResponseFromHttpUrl(weatherRequestUrl);
                    String[] simplejSonWetherData = OpenWeatherJsonUtils.getSimpleWeatherStringsFromJson(MainActivity.this, jsonWeatherResponse);

                    return simplejSonWetherData;
                } catch (Exception e){
                    e.printStackTrace();
                    return null;
                }
            }

            /**
             * Sends the result of the load to the rgistered listener.
             *
             * @param data The result of the load
             */
            public void deliverResult(String[] data) {
                mWeatherData = data;
                super.deliverResult(data);
            }
        };
    }

    /**
     * Called when a previously created loader has finished its load.
     *
     * @param loader The loader that has finished
     * @param data The data generated by the Loader
     */

    @Override
    public void onLoadFinished(Loader<String[]> loader, String[] data) {
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        forecastAdapter.setWeatherData(data);
        if (null == data){
            showErrorMessage();
        } else {
            showWeatherDataView();
        }
    }

    /**
     * Called when a previously created loader is being reset, and thus making its data unavailable.
     * The application should at this point remove any references it has to the Loader's data.
     *
     * @param loader The loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<String[]> loader) {
        /**
         * We aren't using this method in our example application, but we are required to Override
         * it to implement the LoaderCallbacs<String> interface.
         */
    }

    /**
     * This method is used when we are resetting data, so that at one point in time duing a refresh
     * of our data you can see that there is no data showing.
     */
    private void invalidateData() {
        forecastAdapter.setWeatherData(null);
    }

    /**
     * This method uses the URI scheme for showing a location found on a map.
     * This super-handy intent is detailed in the "Common Intents" page of
     * Android's developer site:
     *
     * @see <a"http://developer.android.com/guide/components/intents-common.html#Maps">
     *
     */

    private void openLocationInMap() {
        String addressString = SunshinePreferences.getPreferredWeatherLocation(this);

        Uri geoLocation = Uri.parse("geo:0,0?=" + addressString);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed");
        }
    }

    // Override onCreateOptionsMenu to inflate the menu for this activity
    // Return true to display the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Use AppCompactActivity's method getMenuInflater to get a handle on the menu inflater
        MenuInflater inflater = getMenuInflater();

        // Use the inflater's inflate method to inflate our menu layout to this menu
        inflater.inflate(R.menu.forecast, menu);

        // Return true so that the menu is displayed in the Toolbar
        return true;
    }

    // Override onOptionsItemSelected to handle clicks on the refresh button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            invalidateData();
            getSupportLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
            return true;
        }

        if (id == R.id.action_map) {
            openLocationInMap();
            return true;
        }

        if (id == R.id.action_settings) {
            Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
            startActivity(startSettingsActivity);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        /*
         * Set this flag to true so that when control returns to MainActivity, it can refresh the
         * data.
         *
         * This isn't the ideal solution because there really isn't a need to perform another
         * GET request just to change the units, but this is the simplest solution that gets the
         * job done for now. Later in this course, we are going to show you more elegant ways to
         * handle converting the units from celsius to fahrenheit and back without hitting the
         * network again by keeping a copy of the data in a manageable format.
         */
        PREFERENCES_HAVE_BEEN_UPDATED = true;
    }
}
