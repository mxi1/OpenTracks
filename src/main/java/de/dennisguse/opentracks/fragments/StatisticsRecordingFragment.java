package de.dennisguse.opentracks.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackRecordingActivity;
import de.dennisguse.opentracks.adapters.SensorsAdapter;
import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.TrackDataListener;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.sensor.SensorDataCycling;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.databinding.StatisticsRecordingBinding;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.services.TrackRecordingServiceInterface;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.UnitConversions;
import de.dennisguse.opentracks.viewmodels.SensorDataModel;

/**
 * A fragment to display track statistics to the user for a currently recording {@link Track}.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StatisticsRecordingFragment extends Fragment implements TrackDataListener {

    private static final String TAG = StatisticsRecordingFragment.class.getSimpleName();

    private static final long UI_UPDATE_INTERVAL = UnitConversions.ONE_SECOND_MS;
    public static Fragment newInstance() {
        return new StatisticsRecordingFragment();
    }


    private TrackDataHub trackDataHub;

    private Handler handlerUpdateUI;

    private TrackRecordingServiceConnection trackRecordingServiceConnection = new TrackRecordingServiceConnection();
    private TrackPoint lastTrackPoint;

    //TODO Should not be nullable
    private TrackStatistics lastTrackStatistics;

    private String category = "";

    private StatisticsRecordingBinding viewBinding;
    private SensorsAdapter sensorsAdapter;

    private SharedPreferences sharedPreferences;
    private boolean preferenceMetricUnits;
    private boolean preferenceReportSpeed;
    private boolean preferenceShowAltitude;
    private boolean preferenceShowCoordinate;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        boolean updateUInecessary = false;

        if (PreferencesUtils.isKey(getContext(), R.string.stats_units_key, key)) {
            updateUInecessary = true;
            preferenceMetricUnits = PreferencesUtils.isMetricUnits(sharedPreferences, getContext());
        }

        if (PreferencesUtils.isKey(getContext(), R.string.stats_rate_key, key)) {
            updateUInecessary = true;
            preferenceReportSpeed = PreferencesUtils.isReportSpeed(sharedPreferences, getContext(), category);
        }

        if (PreferencesUtils.isKey(getContext(), R.string.stats_show_grade_altitude_key, key)) {
            updateUInecessary = true;
            preferenceShowAltitude = PreferencesUtils.isShowStatsAltitude(sharedPreferences, getContext());
        }

        if (PreferencesUtils.isKey(getContext(), R.string.stats_show_coordinate_key, key)) {
            updateUInecessary = true;
            preferenceShowCoordinate = PreferencesUtils.isStatsShowCoordinate(sharedPreferences, getContext());
        }

        if (key != null && updateUInecessary && isResumed()) {
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    updateUI();
                }
            });
        }
    };

    private final Runnable updateUIeachSecond = new Runnable() {
        public void run() {
            if (isResumed() && isSelectedTrackRecording()) {
                if (!isSelectedTrackPaused() && lastTrackStatistics != null) {
                    synchronized (this) {
                        if (lastTrackPoint != null && lastTrackPoint.hasLocation() && !lastTrackPoint.isRecent()) {
                            lastTrackPoint = null;
                            setLocationValues();
                        }
                    }
                    updateTotalTime();
                    updateSensorDataUI();
                }

                handlerUpdateUI.postDelayed(this, UI_UPDATE_INTERVAL);
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handlerUpdateUI = new Handler();

        sharedPreferences = PreferencesUtils.getSharedPreferences(getContext());

        sensorsAdapter = new SensorsAdapter(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = StatisticsRecordingBinding.inflate(inflater, container, false);
        viewBinding.statsActivityTypeIcon.setOnClickListener(v -> ((TrackRecordingActivity) getActivity()).chooseActivityType(category));

        RecyclerView sensorsRecyclerView = viewBinding.statsSensorsRecyclerView;
        sensorsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        sensorsRecyclerView.setAdapter(sensorsAdapter);

        return viewBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeTrackDataHub();

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);

        trackRecordingServiceConnection.startConnection(getContext());

        updateUIeachSecond.run();
        handlerUpdateUI.post(updateUIeachSecond);
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseTrackDataHub();

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        handlerUpdateUI.removeCallbacks(updateUIeachSecond);
    }

    @Override
    public void onStop() {
        super.onStop();
        trackRecordingServiceConnection.unbind(getContext());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        trackRecordingServiceConnection = null;
        sharedPreferences = null;
    }

    @Override
    public void onTrackUpdated(final Track track) {
        if (isResumed()) {
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    lastTrackStatistics = track != null ? track.getTrackStatistics() : null;
                    String newCategory = track != null ? track.getCategory() : "";
                    if (!category.equals(newCategory)) {
                        category = newCategory;
                        sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, getString(R.string.stats_rate_key));
                    }
                    updateUI();
                }
            });
        }
    }

    @Override
    public void clearTrackPoints() {
        lastTrackPoint = null;
    }

    @Override
    public void onSampledInTrackPoint(@NonNull TrackPoint trackPoint) {
        // We don't care.
    }

    @Override
    public void onSampledOutTrackPoint(@NonNull TrackPoint trackPoint) {
        // We don't care.
    }

    @Override
    public void onNewTrackPointsDone(@NonNull TrackPoint newLastTrackPoint) {
        if (isResumed()) {
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    synchronized (this) {
                        this.lastTrackPoint = newLastTrackPoint;

                        if (!isSelectedTrackRecording() || isSelectedTrackPaused()) {
                            this.lastTrackPoint = null;
                        }

                        if (this.lastTrackPoint != null && this.lastTrackPoint.hasLocation() && !this.lastTrackPoint.isRecent()) {
                            this.lastTrackPoint = null;
                        }
                        setLocationValues();
                    }
                }
            });
        }
    }

    @Override
    public void clearMarkers() {
        // We don't care.
    }

    @Override
    public void onNewMarker(Marker marker) {
        // We don't care.
    }

    @Override
    public void onNewMarkersDone() {
        // We don't care.
    }

    /**
     * Resumes the trackDataHub.
     * Needs to be synchronized because trackDataHub can be accessed by multiple threads.
     */
    private synchronized void resumeTrackDataHub() {
        trackDataHub = ((TrackRecordingActivity) getActivity()).getTrackDataHub();
        trackDataHub.registerTrackDataListener(this, true, false, true, true);
    }

    /**
     * Pauses the trackDataHub.
     * Needs to be synchronized because trackDataHub can be accessed by multiple threads.
     */
    private synchronized void pauseTrackDataHub() {
        trackDataHub.unregisterTrackDataListener(this);
        trackDataHub = null;
    }

    /**
     * Returns true if the selected track is recording.
     * Needs to be synchronized because trackDataHub can be accessed by multiple threads.
     */
    private synchronized boolean isSelectedTrackRecording() {
        return trackDataHub != null && trackDataHub.isSelectedTrackRecording();
    }

    /**
     * Returns true if the selected track is paused.
     * Needs to be synchronized because trackDataHub can be accessed by multiple threads.
     */
    private synchronized boolean isSelectedTrackPaused() {
        return trackDataHub != null && trackDataHub.isSelectedTrackPaused();
    }

    private void updateUI() {
        updateStats();
        setLocationValues();
        updateSensorDataUI();
    }

    /**
     * Tries to fetch most recent {@link SensorDataSet} from {@link de.dennisguse.opentracks.services.TrackRecordingService}.
     * Also sets altitude gain and loss.
     */
    private void updateSensorDataUI() {
        TrackRecordingServiceInterface trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();

        if (trackRecordingService == null) {
            Log.d(TAG, "Cannot get the track recording service.");
        } else {
            SensorDataSet sensorDataSet = trackRecordingService.getSensorData();
            if (sensorDataSet != null) {
                List<SensorDataModel> sensorDataList = new ArrayList<>();
                if (sensorDataSet.getHeartRate() != null) {
                    sensorDataList.add(new SensorDataModel(sensorDataSet.getHeartRate()));
                }
                if (sensorDataSet.getCyclingCadence() != null) {
                    sensorDataList.add(new SensorDataModel(sensorDataSet.getCyclingCadence()));
                }
                if (sensorDataSet.getCyclingPower() != null) {
                    sensorDataList.add(new SensorDataModel(sensorDataSet.getCyclingPower()));
                }
                sensorsAdapter.swapData(sensorDataList);
                setSpeedSensorData(sensorDataSet);
            }
            //TODO Check if we can get the total altitude gain and loss via trackStatistics instead of doing some computation in the UI layer.
            setTotalAltitudeGain(trackRecordingService.getAltitudeGain_m());
            setTotalAltitudeLoss(trackRecordingService.getAltitudeLoss_m());
        }
    }

    // Set altitude gain
    private void setTotalAltitudeGain(Float altitudeGain_m) {
        Float totalAltitudeGain = altitudeGain_m;

        if (lastTrackStatistics != null && lastTrackStatistics.hasTotalAltitudeGain()) {
            if (altitudeGain_m == null) {
                totalAltitudeGain = lastTrackStatistics.getTotalAltitudeGain();
            } else {
                totalAltitudeGain += lastTrackStatistics.getTotalAltitudeGain();
            }
        }

        Pair<String, String> parts = StringUtils.formatAltitude(getContext(), totalAltitudeGain, preferenceMetricUnits);
        viewBinding.statsAltitudeGainValue.setText(parts.first);
        viewBinding.statsAltitudeGainUnit.setText(parts.second);
    }

    // Set altitude loss
    private void setTotalAltitudeLoss(Float altitudeLoss_m) {
        Float totalAltitudeLoss = altitudeLoss_m;

        if (lastTrackStatistics != null && lastTrackStatistics.hasTotalAltitudeLoss()) {
            if (altitudeLoss_m == null) {
                totalAltitudeLoss = lastTrackStatistics.getTotalAltitudeLoss();
            } else {
                totalAltitudeLoss += lastTrackStatistics.getTotalAltitudeLoss();
            }
        }

        Pair<String, String> parts = StringUtils.formatAltitude(getContext(), totalAltitudeLoss, preferenceMetricUnits);
        viewBinding.statsAltitudeLossValue.setText(parts.first);
        viewBinding.statsAltitudeLossUnit.setText(parts.second);
    }

    private void setSpeedSensorData(SensorDataSet sensorDataSet) {
        if (sensorDataSet != null && sensorDataSet.getCyclingDistanceSpeed() != null) {
            SensorDataCycling.DistanceSpeed data = sensorDataSet.getCyclingDistanceSpeed();

            if (data.hasValue() && data.isRecent()) {
                setTotalDistance(data.getValue().distance_overall_m);
                setSpeed(data.getValue().speed_mps);
            }
            if (data.hasValue() && data.isRecent()) {
                setSpeed(data.getValue().speed_mps);
            }
        }
    }

    private void updateStats() {

        setTotalDistance(0);

        // Set activity type
        {
            String trackIconValue = TrackIconUtils.getIconValue(getContext(), category);
            viewBinding.statsActivityTypeIcon.setEnabled(isSelectedTrackRecording());
            viewBinding.statsActivityTypeIcon.setImageResource(TrackIconUtils.getIconDrawable(trackIconValue));
        }

        // Set time
        if (lastTrackStatistics != null) {
            viewBinding.statsMovingTimeValue.setText(StringUtils.formatElapsedTime(lastTrackStatistics.getMovingTime()));
            updateTotalTime();
        }

        // Set average speed/pace
        {
            double speed = lastTrackStatistics != null ? lastTrackStatistics.getAverageSpeed() : Double.NaN;
            viewBinding.statsAverageSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
            viewBinding.statsAverageSpeedValue.setText(parts.first);
            viewBinding.statsAverageSpeedUnit.setText(parts.second);
        }

        // Set max speed/pace
        {
            double speed = lastTrackStatistics == null ? Double.NaN : lastTrackStatistics.getMaxSpeed();

            viewBinding.statsMaxSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_max_speed : R.string.stats_fastest_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
            viewBinding.statsMaxSpeedValue.setText(parts.first);
            viewBinding.statsMaxSpeedUnit.setText(parts.second);
        }

        // Set moving speed/pace
        {
            double speed = lastTrackStatistics != null ? lastTrackStatistics.getAverageMovingSpeed() : Double.NaN;

            viewBinding.statsMovingSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_average_moving_speed : R.string.stats_average_moving_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
            viewBinding.statsMovingSpeedValue.setText(parts.first);
            viewBinding.statsMovingSpeedUnit.setText(parts.second);
        }

        // Set altitude gain and loss
        {
            viewBinding.statsAltitudeGroup.setVisibility(preferenceShowAltitude ? View.VISIBLE : View.GONE);
        }
    }

    private void updateTotalTime() {
        Duration totalTime = lastTrackStatistics.getTotalTime();
        if (isSelectedTrackRecording()) {
            TrackRecordingServiceInterface trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();
            if (trackRecordingService != null) {
                totalTime = trackRecordingService.getTotalTime();
            }
        }

        viewBinding.statsTotalTimeValue.setText(StringUtils.formatElapsedTime(totalTime));
    }

    private void setLocationValues() {
        // Set speed/pace
        double speed = lastTrackPoint != null && lastTrackPoint.hasSpeed() ? lastTrackPoint.getSpeed() : Double.NaN;
        setSpeed(speed);

        // Set altitude
        viewBinding.statsAltitudeGroup.setVisibility(preferenceShowAltitude ? View.VISIBLE : View.GONE);

        if (preferenceShowAltitude) {
            // Current altitude
            Float altitude = lastTrackPoint != null && lastTrackPoint.hasAltitude() ? (float) lastTrackPoint.getAltitude() : null;
            Pair<String, String> parts = StringUtils.formatAltitude(getContext(), altitude, preferenceMetricUnits);
            viewBinding.statsAltitudeCurrentValue.setText(parts.first);
            viewBinding.statsAltitudeCurrentUnit.setText(parts.second);
        }

        // Set coordinate
        viewBinding.statsCoordinateGroup.setVisibility(preferenceShowCoordinate ? View.VISIBLE : View.GONE);
        if (preferenceShowCoordinate) {
            String latitudeText = getContext().getString(R.string.value_unknown);
            String longitudeText = getContext().getString(R.string.value_unknown);
            if (lastTrackPoint != null && lastTrackPoint.hasLocation()) {
                latitudeText = StringUtils.formatCoordinate(lastTrackPoint.getLatitude());
                longitudeText = StringUtils.formatCoordinate(lastTrackPoint.getLongitude());
            }
            viewBinding.statsLatitudeValue.setText(latitudeText);
            viewBinding.statsLongitudeValue.setText(longitudeText);
        }
    }

    private void setTotalDistance(double sensorDistanceSinceLastTrackpoint) {
        double totalDistance = lastTrackStatistics != null ? (lastTrackStatistics.getTotalDistance() + sensorDistanceSinceLastTrackpoint) : Double.NaN;
        Pair<String, String> parts = StringUtils.getDistanceParts(getContext(), totalDistance, preferenceMetricUnits);

        viewBinding.statsDistanceValue.setText(parts.first);
        viewBinding.statsDistanceUnit.setText(parts.second);
    }

    private void setSpeed(double speed) {
        viewBinding.statsSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_speed : R.string.stats_pace);

        Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
        viewBinding.statsSpeedValue.setText(parts.first);
        viewBinding.statsSpeedUnit.setText(parts.second);
    }
}
