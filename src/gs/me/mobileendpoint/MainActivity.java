package gs.me.mobileendpoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import android.content.IntentSender;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v4.app.FragmentActivity;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements 
	GooglePlayServicesClient.ConnectionCallbacks,
	GooglePlayServicesClient.OnConnectionFailedListener,
	com.google.android.gms.location.LocationListener {

    // Define an object that holds accuracy and frequency parameters
    LocationRequest mLocationRequest;
   
    LocationClient mLocationClient;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mLocationClient = new LocationClient(this,this,this);
		
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(5000);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(1000);
	}
	
	@Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
    	if (mLocationClient.isConnected()) {
	        // If location client is still connected on stop, remove the location updates.
    		mLocationClient.removeLocationUpdates((com.google.android.gms.location.LocationListener) this);
    	}
    	// Disconnecting the client invalidates it.
	    mLocationClient.disconnect();
        super.onStop();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void sendLocation(View view) {
		mLocationClient.requestLocationUpdates(mLocationRequest, (com.google.android.gms.location.LocationListener) this);
	}
	
	public void stopSendingLocations(View view) {
		System.out.println("Stop sending locations.");
    	if (mLocationClient.isConnected()) {
	        // If location client is still connected on stop, remove the location updates.
    		mLocationClient.removeLocationUpdates((com.google.android.gms.location.LocationListener) this);
    	}
	}
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        9000);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
        	System.out.println("Error");
        }
	}

	@Override
	public void onConnected(Bundle arg0) {
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }

	@Override
	public void onDisconnected() {
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
		
	}
	
	/*
	 * Get unix time for utc
	 */
	public int GetUnixTime()
	{
	    Calendar calendar = Calendar.getInstance();
	    long now = calendar.getTimeInMillis();
	    int utc = (int)(now / 1000);
	    return (utc);

	}
	/*
	 * Converts a Location to a JSONObject
	 */
	private JSONObject convertToJSON(Location location) {
		JSONObject locationJSON = new JSONObject();
		try {
			locationJSON.put("latitude", Double.toString(location.getLatitude()));
			locationJSON.put("longitude", Double.toString(location.getLongitude()));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return locationJSON;
	}
	// Holder for the previous location.
	private Location previousLocation;
	private int prevTimeInMillis; 
	
	@Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
		JSONObject json = new JSONObject();
		try {
			json.put("id", Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID));
			json.put("mobileId", Build.DISPLAY);
			JSONObject locationJSON = convertToJSON(location);
			int currentTime = GetUnixTime();
			if (previousLocation != null){
				JSONObject prevLocationJSON = convertToJSON(previousLocation);
				prevLocationJSON.put("timestamp", prevTimeInMillis);
				locationJSON.put("previousPosition", prevLocationJSON);
				locationJSON.put("timestamp", currentTime);
			} else {
				prevTimeInMillis = currentTime;
				previousLocation = location;
			}
			json.put("position", locationJSON);
		} catch (JSONException e) {
			e.printStackTrace();
		}
        // Send location information.
    	new SendDataTask().execute(json.toString());
        Toast.makeText(this, json.toString(), Toast.LENGTH_SHORT).show();
    }
	
	private class SendDataTask extends AsyncTask<String, Integer, Double>{
		 
		@Override
		protected Double doInBackground(String... params) {
			// TODO Auto-generated method stub
			postData(params[0]);
			return null;
		}
 
		protected void onPostExecute(Double result){
			System.out.println(result);
		}
		
		protected void onProgressUpdate(Integer... progress){
			System.out.println(progress);
		}
 
		public void postData(String value) {
			// Create a new HttpClient and Post Header
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost post = new HttpPost("http://10.0.2.2:3002/mobile");
			post.setHeader("token", "some_value");
 
			try {
                StringEntity se = new StringEntity(value);  
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                post.setEntity(se);
				// Execute HTTP Post Request
				HttpResponse response = httpclient.execute(post);
				StatusLine statusLine = response.getStatusLine();
			    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
			        ByteArrayOutputStream out = new ByteArrayOutputStream();
			        response.getEntity().writeTo(out);
			        out.close();
			        String responseString = out.toString();
			        System.out.println(responseString);
			    } else{
			        //Closes the connection.
			        response.getEntity().getContent().close();
			        throw new IOException(statusLine.getReasonPhrase());
			    }
 
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}
		}
 
	}

}
