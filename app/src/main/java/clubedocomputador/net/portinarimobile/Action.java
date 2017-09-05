package clubedocomputador.net.portinarimobile;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;

import org.as.mjpeg.MjpegView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class Action extends AppCompatActivity {

    private SharedPreferences settings;
    private String host;
    private int nav;
    private BottomNavigationView navigation;

    private FrameLayout content;
    private View currentView;
    private LinearLayout cameraContainer;
    private Button gate;
    private Button reboot;
    private Switch light;
    private MjpegView camera;
    private ConfigFragment configFragment;


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            nav = item.getItemId();
            return navigate();
        }

    };

    private void home() {
        currentView = inflateViewIntoContentView(R.layout.main);
        cameraContainer = currentView.findViewById(R.id.camera);
        gate = currentView.findViewById(R.id.openGate);
        setOnClickListener(gate, "gate");
        light = currentView.findViewById(R.id.toggleLight);
        setLightListener();
        initVideoStreaming();
    }

    private void system() {
        currentView = inflateViewIntoContentView(R.layout.system);
        reboot = currentView.findViewById(R.id.reboot);
        setOnClickListener(reboot, "reboot");
    }

    private void config() {
        if (configFragment != null && configFragment.getView() != null) {
            ((ViewGroup) configFragment.getView().getParent()).removeAllViews();
        }
        configFragment = new ConfigFragment();
        getFragmentManager().beginTransaction().replace(R.id.config_fragment, configFragment).commit();
        currentView = inflateViewIntoContentView(R.layout.config);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action);

        settings = PreferenceManager.getDefaultSharedPreferences(Action.this);
        host = settings.getString("host", "192.168.1.101");

        content = (FrameLayout) findViewById(R.id.content);
        navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        if (savedInstanceState != null) {
            nav = savedInstanceState.getInt("nav");
            navigation.setSelectedItemId(nav);
            navigate();
        } else {
            home();
        }
    }

    private boolean navigate() {
        switch (nav) {
            case R.id.navigation_home:
                home();
                return true;
            case R.id.navigation_config:
                config();
                return true;
            case R.id.navigation_system:
                system();
                return true;
        }
        return false;

    }

    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("nav", nav);
    }

    protected View inflateViewIntoContentView(int view) {
        content.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        View inflatedView = inflater.inflate(view, null);
        content.addView(inflatedView);

        return inflatedView;
    }


    private void initVideoStreaming() {
        int portVideoStream = Integer.parseInt(settings.getString("port-video", "8081"));
        String URL = String.format("http://%s:%d", host, portVideoStream);
        Log.i("URL Video", URL);
        camera = new MjpegView(this);
        camera.setSource(URL);
        camera.setDisplayMode(MjpegView.SIZE_BEST_FIT);
        cameraContainer.addView(camera);
    }

    public void setOnClickListener(View view, final String command) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Appliances().execute(command);
            }
        });
    }

    public void setLightListener() {

        light.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    new Appliances().execute("lighton");
                } else {
                    new Appliances().execute("lightoff");
                }
            }
        });


    }

    private class Appliances extends AsyncTask<String, Void, String> {

        private Exception exception;

        protected String doInBackground(String... command) {
            int portAppliances = Integer.parseInt(settings.getString("port-appliances", "5000"));

            try {
                String URL = String.format("http://%s:%d/%s", host, portAppliances, command[0]);
                Log.i(String.format("URL %s", command[0]), URL);
                URL url = new URL(URL);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(1000);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader r = new BufferedReader(new InputStreamReader(in));
                StringBuilder total = new StringBuilder(in.available());
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line).append('\n');
                }
                return total.toString();
            } catch (Exception e) {
                Log.e("Erro", e.getMessage(), e);
                this.exception = e;
                return null;
            }


        }

        protected void onPostExecute(String response) {
            if (response != null)
                Log.e("Appliance response: ", response);
        }
    }

    public static class ConfigFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.config);
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if (!getActivity().isFinishing()) {
                ConfigFragment f = (ConfigFragment) getFragmentManager().findFragmentById(R.id.config_fragment);
                if (f != null)
                    getFragmentManager().beginTransaction().remove(f).commit();
            }
        }


    }


}
