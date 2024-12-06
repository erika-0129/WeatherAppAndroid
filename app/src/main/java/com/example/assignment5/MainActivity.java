package com.example.assignment5;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // NOTE: AsyncTask - This class was deprecated in API level 30. Using threads in this project.
    // List of Weather objects representing the forecast
    private static final String TAG = "MainActivity";
    private List<Weather> weatherList = new ArrayList<>();
    // ArrayAdapter for binding Weather objects to a ListView
    private WeatherArrayAdapter weatherArrayAdapter;
    private ListView weatherListView; // displays weather info
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinatorLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // create ArrayAdapter to bind weatherList to the weatherListView
        weatherListView = findViewById(R.id.weatherListView);
        weatherArrayAdapter = new WeatherArrayAdapter(this, weatherList);
        weatherListView.setAdapter(weatherArrayAdapter);

        // configure FAB to hide keyboard and initiate web service request
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            EditText locationEditText = findViewById(R.id.locationEditText);
            String city = locationEditText.getText().toString();
            URL url = getWeatherURL(city);

            if (url != null) {
                dismissKeyboard(locationEditText);
                getWeatherTask(url);
            } else {
                showSnackbar(R.string.invalid_url);
            }
        });
    }

    // programmatically dismiss keyboard when user touches FAB
    private void dismissKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private URL getWeatherURL(String city) {
        String apiKey = getString(R.string.API_KEY);
        String baseUrl = getString(R.string.web_service_url);

        try {
            String urlString = String.format(baseUrl, apiKey, URLEncoder.encode(city, "UTF-8"));
            Log.d(TAG, "Weather URL: " + urlString);
            return new URL(urlString);
        } catch (Exception e) {
            Log.e(TAG, "Error creating weather URL", e);
        }
        return null;
    }

    private void getWeatherTask(URL url) {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
                int response = connection.getResponseCode();
                Log.d(TAG, "URL response code: " + response);

                if (response == HttpURLConnection.HTTP_OK) {
                    StringBuilder builder = new StringBuilder();

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                    } catch (IOException e) {
                        showSnackbar(R.string.read_error);
                        Log.e(TAG, "Error reading data", e);
                    }

                    JSONObject weatherData = new JSONObject(builder.toString());
                    handler.post(() -> updateWeatherList(weatherData));
                } else {
                    showSnackbar(R.string.connect_error);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching coordinates", e);
                showSnackbar(R.string.connect_error);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    // Cleaner to use when a Snackbar is needed
    private void showSnackbar(int messageResId) {
        handler.post(() -> Snackbar.make(findViewById(R.id.coordinatorLayout), messageResId, Snackbar.LENGTH_LONG).show());
    }

    private void updateWeatherList(JSONObject weather) {
        convertJSONtoArrayList(weather);
        weatherArrayAdapter.notifyDataSetChanged();
        weatherListView.smoothScrollToPosition(0);
    }

    // create Weather objects from JSONObject containing the forecast
    private void convertJSONtoArrayList(JSONObject forecast) {
        weatherList.clear();

        try {
            JSONObject forecastObject = forecast.getJSONObject("forecast");
            JSONArray dailyArray = forecastObject.getJSONArray("forecastday");

            for (int i = 0; i < dailyArray.length(); ++i) {
                JSONObject dayObject = dailyArray.getJSONObject(i);
                String date = dayObject.getString("date");
                JSONObject dayDetails = dayObject.getJSONObject("day");
                double maxTemp = dayDetails.getDouble("maxtemp_f");
                double minTemp = dayDetails.getDouble("mintemp_f");
                double avgHumidity = dayDetails.getDouble("avghumidity");
                JSONObject condition = dayDetails.getJSONObject("condition");
                String description = condition.getString("text");
                String iconURL = condition.getString("icon");

                weatherList.add(new Weather(date, minTemp, maxTemp, avgHumidity, description, iconURL));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data", e);
            showSnackbar(R.string.no_data_error);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}

    /*
    // makes the REST web service call to get weather data and
    // saves the data to a local HTML file
    private class GetWeatherTask
            extends AsyncTask<URL, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(URL... params) {
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection) params[0].openConnection();
                int response = connection.getResponseCode();

                if (response == HttpURLConnection.HTTP_OK) {
                    StringBuilder builder = new StringBuilder();

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()))) {

                        String line;

                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                    }
                    catch (IOException e) {
                        Snackbar.make(findViewById(R.id.coordinatorLayout),
                                R.string.read_error, Snackbar.LENGTH_LONG).show();
                        e.printStackTrace();
                    }

                    return new JSONObject(builder.toString());
                }
                else {
                    Snackbar.make(findViewById(R.id.coordinatorLayout),
                            R.string.connect_error, Snackbar.LENGTH_LONG).show();
                }
            }
            catch (Exception e) {
                Snackbar.make(findViewById(R.id.coordinatorLayout),
                        R.string.connect_error, Snackbar.LENGTH_LONG).show();
                e.printStackTrace();
            }
            finally {
                connection.disconnect(); // close the HttpURLConnection
            }

            return null;
        }

        // process JSON response and update ListView
        @Override
        protected void onPostExecute(JSONObject weather) {
            convertJSONtoArrayList(weather); // repopulate weatherList
            weatherArrayAdapter.notifyDataSetChanged(); // rebind to ListView
            weatherListView.smoothScrollToPosition(0); // scroll to top
        }
    }*/