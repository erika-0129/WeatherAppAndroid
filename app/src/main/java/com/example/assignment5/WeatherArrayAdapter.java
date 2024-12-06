// Defines a subclass of ArrayAdapter for binding an ArrayList<Weather> to the MainActivity's ListView
package com.example.assignment5;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class WeatherArrayAdapter extends ArrayAdapter<Weather> {
    // Class for reusing views as list items scroll off and onto the screen
    private static class ViewHolder{
        ImageView conditionImageView;
        TextView dayTextView;
        TextView lowTextView;
        TextView hiTextView;
        TextView humidityTextView;
    }

    // Stores already downloaded Bitmaps for reuse
    private Map<String, Bitmap> bitmaps = new HashMap<>();

    // Constructor to initialize superclass inherited numbers
    public WeatherArrayAdapter(Context context, List<Weather> forecast){
        super(context, -1, forecast);
    }

    // Using threads and handler instead of AsyncTask due to its depreciation in API 30 and higher
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    private Handler handler = new Handler(Looper.getMainLooper());
    // Updated to use threads
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Weather day = getItem(position);
        ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item, parent, false);
            viewHolder.conditionImageView = convertView.findViewById(R.id.conditionImageView);
            viewHolder.dayTextView = convertView.findViewById(R.id.dayTextView);
            viewHolder.lowTextView = convertView.findViewById(R.id.lowTextView);
            viewHolder.hiTextView = convertView.findViewById(R.id.hiTextView);
            viewHolder.humidityTextView = convertView.findViewById(R.id.humidityTextView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (bitmaps.containsKey(day.iconURL)) {
            viewHolder.conditionImageView.setImageBitmap(bitmaps.get(day.iconURL));
        } else {
            loadImage(viewHolder.conditionImageView, day.iconURL);
        }

        Context context = getContext();
        viewHolder.dayTextView.setText(context.getString(R.string.daily_description, day.dayOfWeek, day.description));
        viewHolder.lowTextView.setText(context.getString(R.string.low_temp, day.minTemp));
        viewHolder.hiTextView.setText(context.getString(R.string.high_temp, day.maxTemp));
        viewHolder.humidityTextView.setText(context.getString(R.string.humidity, day.humidity));

        return convertView;
    }

    private void loadImage(ImageView imageView, String url) {
        executorService.execute(() -> {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;

            try {
                URL imageUrl = new URL(url);
                connection = (HttpURLConnection) imageUrl.openConnection();

                try (InputStream inputStream = connection.getInputStream()) {
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    bitmaps.put(url, bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            Bitmap finalBitmap = bitmap;
            handler.post(() -> {
                if (finalBitmap != null) {
                    imageView.setImageBitmap(finalBitmap);
                }
            });
        });
    }
}

    /*
    This information is from the book. Due to AsyncTask being depreciated in API 30 and higher,
    the code had to be updated to use Threads instead of AsyncTask

    // Create custom views for ListView's items
    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        Weather day = getItem(position);
        ViewHolder viewHolder;              // object that references list item's views

        // Check for reusable ViewHolder from ListView item that scrolled offscreen, otherwise, create a new ViewHolder
        if(convertView == null){
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView =
                    inflater.inflate(R.layout.list_item, parent, false);
            viewHolder.conditionImageView =
                    (ImageView) convertView.findViewById(R.id.conditionImageView);
            viewHolder.dayTextView =
                    (TextView) convertView.findViewById(R.id.dayTextView);
            viewHolder.lowTextView =
                    (TextView) convertView.findViewById(R.id.lowTextView);
            viewHolder.hiTextView =
                    (TextView) convertView.findViewById(R.id.hiTextView);
            viewHolder.humidityTextView =
                    (TextView) convertView.findViewById(R.id.humidityTextView);

        }
        else{
            viewHolder = (ViewHolder) convertView.getTag();                 // reuse the existing viewholder stored as the list item's tag
        }

        // if the weather condition icon is already downloaded, reuse
        if(bitmaps.containsKey(day.iconURL)){
            viewHolder.conditionImageView.setImageBitmap(bitmaps.get(day.iconURL));
        }
        else {
            // download and display weather condition image
            new LoadImageTask(viewHolder.conditionImageView).execute(day.iconURL);
        }

        // Get other data from Weather object and pace into views
        Context context = getContext();                     // For loading string resources
        viewHolder.dayTextView.setText(context.getString(R.string.daily_description, day.dayOfWeek, day.description));
        viewHolder.lowTextView.setText(context.getString(R.string.low_temp, day.minTemp));
        viewHolder.hiTextView.setText(context.getString(R.string.high_temp, day.maxTemp));
        viewHolder.humidityTextView.setText(context.getString(R.string.humidity, day.humidity));

        return convertView;
    }

    //AsyncTask to load weather condition icons in a separate thread
    private class LoadImageTask extends AsyncTask<String, Void, Bitmap>{
        private ImageView imageView;            // Displays the thumbnail

        // Store ImageView on which to set the downloaded Bitmap
        public LoadImageTask(ImageView imageView){
            this.imageView = imageView;
        }

        // Load image; params[0] is the string URL representing the image
        @Override
        protected Bitmap doInBackground(String... params){
            Bitmap bitmap = null;
            HttpURLConnection connection = null;

            try{
                URL url = new URL(params[0]);

                // open an HttpURLConnection, get its InputStream and download image
                connection = (HttpURLConnection) url.openConnection();

                try(InputStream inputStream = connection.getInputStream()){
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    bitmaps.put(params[0], bitmap);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
            finally{
                connection.disconnect();            // Close the HttpURLConnection
            }
            return bitmap;
        }

        // Set weather condition image in list item
        @Override
        protected void onPostExecute(Bitmap bitmap){
            imageView.setImageBitmap(bitmap);
        }*/

