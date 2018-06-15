package joels.com.projectsunshine;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;

import joels.com.projectsunshine.data.SunshinePreferences;
import joels.com.projectsunshine.utilities.NetworkUtils;
import joels.com.projectsunshine.utilities.OpenWeatherJsonUtils;

public class MainActivity extends AppCompatActivity {

    private TextView mWeatherTextView, mErrorMessageDisplay;
    private ProgressBar mLoadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        mWeatherTextView = findViewById(R.id.tv_weather_data);
        mErrorMessageDisplay = findViewById(R.id.tv_error_message_display);

        /*
        * The ProgressBar that will indicate to the user that we are loading data. It will be
        * hidden when no data is loading.
        *
        * Please note: This so called "ProgressBar" isn't a bar by default. It is more of a circle.
        * We didn't make rules (or the names of Views), we just follow them
         */
        mLoadingIndicator = findViewById(R.id.pb_loading_indicator);

        /* Once all of our views are setup, we can load the weather data. */
        loadWeather();
    }

    private void loadWeather(){
        // Call showWeatherDataView before executing the AsyncTask
        showWeatherDataView();

        String location = SunshinePreferences.getPreferredWeatherLocation(this);
        new FetchWeatherTask().execute(location);

    }

    /*
     * This method will hide the weather data and show the error message
     *
     * Since it is okay to redundantly set the visibility of a view, we don't need to check whether
     * each view is curently visible or invisible.
     */
    private void showErrorMessage() {
        /* First, hide the currently visible data */
        mWeatherTextView.setVisibility(View.INVISIBLE);

        /* Then, show the error */
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }

    /*
    * This method will hide the error message and show the weather data
    */

    private void showWeatherDataView() {
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        mWeatherTextView.setVisibility(View.VISIBLE);
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
                /*
                * Iterate through the array and append the String to the TextView. The reason why we
                * add the "\n\n\n" after the STring is to give visuak separation between each String in the TextView.
                 */
                for (String weatherString : weatherData) {
                    mWeatherTextView.append(weatherString + "\n\n\n");
                }
            } else {
                // If the weather data was null, show the error message
                showErrorMessage();
            }
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
            mWeatherTextView.setText("");
            loadWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
