package joels.com.projectsunshine;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;

import joels.com.projectsunshine.data.SunshinePreferences;
import joels.com.projectsunshine.utilities.NetworkUtils;
import joels.com.projectsunshine.utilities.OpenWeatherJsonUtils;

public class MainActivity extends AppCompatActivity {

    private TextView mWeatherTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        mWeatherTextView = findViewById(R.id.tv_weather_data);

        loadWeather();
    }

    private void loadWeather(){
        String location = SunshinePreferences.getPreferredWeatherLocation(this);
        new FetchWeatherTask().execute(location);

    }

    // Class that extends AsyncTask to perform network calls

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

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
            if (weatherData != null) {
                /*
                * Iterate through the array and append the String to the TextView. The reason why we
                * add the "\n\n\n" after the STring is to give visuak separation between each String in the TextView.
                 */
                for (String weatherString : weatherData) {
                    mWeatherTextView.append(weatherString + "\n\n\n");
                }
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
