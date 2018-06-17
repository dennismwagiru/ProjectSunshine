package joels.com.projectsunshine;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;

import joels.com.projectsunshine.ForecastAdapter.ForecastAdapterOnClickHandler;
import joels.com.projectsunshine.data.SunshinePreferences;
import joels.com.projectsunshine.utilities.NetworkUtils;
import joels.com.projectsunshine.utilities.OpenWeatherJsonUtils;

public class MainActivity extends AppCompatActivity implements ForecastAdapterOnClickHandler {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private ForecastAdapter forecastAdapter;
    private TextView mErrorMessageDisplay;
    private ProgressBar mLoadingIndicator;

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
        forecastAdapter = new ForecastAdapter(this);

        /* Setting the adapter attaches it to the RecyclerView in our layout. */
        mRecyclerView.setAdapter(forecastAdapter);

        /* Once all of our views are setup, we can load the weather data. */
        loadWeather();
    }

    private void loadWeather(){
        // Call showWeatherDataView before executing the AsyncTask
        showWeatherDataView();

        String location = SunshinePreferences.getPreferredWeatherLocation(this);
        new FetchWeatherTask().execute(location);

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

    /*
    * This method will hide the error message and show the weather data
    */

    private void showWeatherDataView() {
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    // Class that extends AsyncTask to perform network calls
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        // Override onPreExecute and show the loading indicator
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        // Override doInBackgroud method to perform your network requests
        @Override
        protected String[] doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up
            if (params.length == 0){
                return null;
            }

            String location = params[0];
            URL weatherRequestUrl = NetworkUtils.buildUrl(location);

            try {
                String jsonWeatherResponse = NetworkUtils.getResponseFromHttpUrl(weatherRequestUrl);

                String[] simpleJsonWeatherData = OpenWeatherJsonUtils.getSimpleWeatherStringsFromJson(MainActivity.this, jsonWeatherResponse);

                return simpleJsonWeatherData;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        // Override the onPostExecute method to display the results of the network request
        @Override
        protected void onPostExecute(String[] weatherData) {
            // As soon as the data is finished loading, hide the loading indicator
            mLoadingIndicator.setVisibility(View.GONE);
            if (weatherData != null) {
                // If the weather data was not null, make sure that the data view is visible
                showWeatherDataView();
                // Use forecastAdapter.setWeatherData(weatherData) to pass in the weatherData
                forecastAdapter.setWeatherData(weatherData);
            } else {
                // If the weather data was null, show the error message
                showErrorMessage();
            }
        }
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
        String addressString = "1600 Ampitheatre Parkway, CA";
        Uri geoLocation = Uri.parse("geo:0,0?=" + addressString);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed");
        }
    }

    // Override onCreateOptionsMenu to inflate the menu for this activty
    // Return true to display the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Use AppCompacActivity's method getMenuInflater to get a handle on the menu inflater
        MenuInflater inflater = getMenuInflater();

        // Use the inflater's inflate method to infalte our menu layout to this menu
        inflater.inflate(R.menu.forecast, menu);

        // Return true so that the menu is displayed in the Toolbar
        return true;
    }

    // Override onOptionsItemSelected to handle clicks on the refresh button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            forecastAdapter.setWeatherData(null);
            loadWeather();
            return true;
        }

        if (id == R.id.action_map) {
            openLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
