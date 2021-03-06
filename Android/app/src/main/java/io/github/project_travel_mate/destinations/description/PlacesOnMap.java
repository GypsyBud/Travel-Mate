package io.github.project_travel_mate.destinations.description;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lucasr.twowayview.TwoWayView;

import java.io.IOException;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.project_travel_mate.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import utils.GPSTracker;

import static utils.Constants.API_LINK;
import static utils.Constants.EXTRA_MESSAGE_ID;
import static utils.Constants.EXTRA_MESSAGE_LATITUDE;
import static utils.Constants.EXTRA_MESSAGE_LONGITUDE;
import static utils.Constants.EXTRA_MESSAGE_NAME;
import static utils.Constants.EXTRA_MESSAGE_TYPE;

public class PlacesOnMap extends AppCompatActivity implements OnMapReadyCallback {

    @BindView(R.id.lv)
    TwoWayView lv;

    private String mDestinationLongitude;
    private String mDesinationLatitude;

    private ProgressDialog mProgressDialog;
    private int mMode;
    private int mIcon;
    private GoogleMap mGoogleMap;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places_on_map);

        ButterKnife.bind(this);

        Intent intent   = getIntent();
        String name     = intent.getStringExtra(EXTRA_MESSAGE_NAME);
        String id       = intent.getStringExtra(EXTRA_MESSAGE_ID);
        String type     = intent.getStringExtra(EXTRA_MESSAGE_TYPE);
        mHandler        = new Handler(Looper.getMainLooper());

        setTitle(name);

        switch (type) {
            case "restaurant":
                mMode = 0;
                mIcon = R.drawable.restaurant;
                break;
            case "hangout":
                mMode = 1;
                mIcon = R.drawable.hangout;
                break;
            case "monument":
                mMode = 2;
                mIcon = R.drawable.monuments;
                break;
            default:
                mMode = 4;
                mIcon = R.drawable.shopping;
                break;
        }

        mDesinationLatitude = intent.getStringExtra(EXTRA_MESSAGE_LATITUDE);
        mDestinationLongitude = intent.getStringExtra(EXTRA_MESSAGE_LONGITUDE);

        getPlaces();

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setTitle("Places");
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }

    private void showMarker(Double locationLat, Double locationLong, String locationName) {
        LatLng coord = new LatLng(locationLat, locationLong);
        if (ContextCompat.checkSelfPermission(PlacesOnMap.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mGoogleMap != null) {
                mGoogleMap.setMyLocationEnabled(true);
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coord, 14));

                MarkerOptions temp = new MarkerOptions();
                MarkerOptions markerOptions = temp
                        .title(locationName)
                        .position(coord)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_drop_black_24dp));
                mGoogleMap.addMarker(markerOptions);
            }
        }
    }

    private void getPlaces() {

        mProgressDialog = new ProgressDialog(PlacesOnMap.this);
        mProgressDialog.setMessage("Fetching data, Please wait...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.show();

        // to fetch city names
        String uri = API_LINK + "places-api.php?lat=" + mDesinationLatitude +
                "&lng=" + mDestinationLongitude + "&mMode=" + mMode;
        Log.v("executing", "URI : " + uri );

        //Set up client
        OkHttpClient client = new OkHttpClient();
        //Execute request
        Request request = new Request.Builder()
                .url(uri)
                .build();
        //Setup callback
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.v("Request Failed", "Message : " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {

                final String res = Objects.requireNonNull(response.body()).string();

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject feed = new JSONObject(res);

                            JSONArray feedItems = feed.getJSONArray("results");
                            Log.v("response", feedItems.toString());


                            lv.setAdapter(new CityInfoAdapter(PlacesOnMap.this, feedItems, mIcon));

                            mProgressDialog.dismiss();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e("ERROR : ", "Message : " + e.getMessage());
                        }
                    }
                });

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {

        mGoogleMap = map;

        GPSTracker tracker = new GPSTracker(this);
        if (!tracker.canGetLocation()) {
            tracker.showSettingsAlert();
        } else {
            String curlat = Double.toString(tracker.getLatitude());
            String curlon = Double.toString(tracker.getLongitude());
            if (curlat.equals("0.0")) {
                curlat = "28.5952242";
                curlon = "77.1656782";
            }
            LatLng coordinate = new LatLng(Double.parseDouble(curlat), Double.parseDouble(curlon));
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 14);
            map.animateCamera(yourLocation);
        }
    }

    class CityInfoAdapter extends BaseAdapter {

        final Context mContext;
        final JSONArray mFeedItems;
        final int mRd;
        LinearLayout mLinearLayout;
        private final LayoutInflater mInflater;

        CityInfoAdapter(Context context, JSONArray feedItems, int r) {
            this.mContext = context;
            this.mFeedItems = feedItems;
            mRd = r;
            mInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mFeedItems.length();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            try {
                return mFeedItems.getJSONObject(position);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View vi = convertView;
            if (vi == null)
                vi = mInflater.inflate(R.layout.city_infoitem, parent, false);

            TextView title = vi.findViewById(R.id.item_name);
            TextView description = vi.findViewById(R.id.item_address);
            LinearLayout onmap = vi.findViewById(R.id.map);
            mLinearLayout = vi.findViewById(R.id.b2);


            try {
                title.setText(mFeedItems.getJSONObject(position).getString("name"));
                description.setText(mFeedItems.getJSONObject(position).getString("address"));
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("ERROR", "Message : " + e.getMessage());
            }

            ImageView iv = vi.findViewById(R.id.image);
            iv.setImageResource(mRd);

            onmap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Intent browserIntent;
                    try {
                        browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps?q=" +
                                mFeedItems.getJSONObject(position).getString("name") +
                                "+(name)+@" +
                                mFeedItems.getJSONObject(position).getString("lat") +
                                "," +
                                mFeedItems.getJSONObject(position).getString("lng")
                        ));
                        mContext.startActivity(browserIntent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });

            mLinearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent browserIntent;
                    browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.co.in/"));
                    mContext.startActivity(browserIntent);
                }
            });

            vi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mGoogleMap.clear();
                    try {
                        showMarker(Double.parseDouble(mFeedItems.getJSONObject(position).getString("lat")),
                                Double.parseDouble(mFeedItems.getJSONObject(position).getString("lng")),
                                mFeedItems.getJSONObject(position).getString("name"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            return vi;
        }
    }
}
