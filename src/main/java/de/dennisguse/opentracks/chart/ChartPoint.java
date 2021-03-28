package de.dennisguse.opentracks.chart;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.content.UITrackPoint;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;
import de.dennisguse.opentracks.util.UnitConversions;

public class ChartPoint {
    //X-axis
    private float timeOrDistance;

    //Y-axis
    private Float altitude;
    private float speed;
    private float pace;
    private Float heartRate;
    private Float cadence;
    private Float power;

    @VisibleForTesting
    ChartPoint(Float altitude) {
        this.altitude = altitude;
    }

    public ChartPoint(@NonNull TrackStatisticsUpdater trackStatisticsUpdater, @NonNull UITrackPoint trackPoint, boolean chartByDistance, boolean metricUnits) {
        TrackStatistics trackStatistics = trackStatisticsUpdater.getTrackStatistics();

        if (chartByDistance) {
            float distance = (float) (trackStatistics.getTotalDistance() * UnitConversions.M_TO_KM);
            if (!metricUnits) {
                distance *= UnitConversions.KM_TO_MI;
            }
            timeOrDistance = distance;
        } else {
            timeOrDistance = trackStatistics.getTotalTime().toMillis();
        }

        if (trackPoint.hasAltitude()) {
            altitude = (float) trackPoint.getAltitudeEGM2008();

            if (!metricUnits) {
                altitude = (float) (altitude * UnitConversions.M_TO_FT);
            }
        }

        speed = (float) (trackStatisticsUpdater.getSmoothedSpeed() * UnitConversions.MPS_TO_KMH);
        if (!metricUnits) {
            speed *= UnitConversions.KM_TO_MI;
        }
        pace = speed == 0 ? 0.0f : (60.0f / speed);
        if (trackPoint.hasHeartRate()) {
            heartRate = trackPoint.getHeartRate_bpm();
        }
        if (trackPoint.hasCyclingCadence()) {
            cadence = trackPoint.getCyclingCadence_rpm();
        }
        if (trackPoint.hasPower()) {
            power = trackPoint.getPower();
        }
    }

    public double getTimeOrDistance() {
        return timeOrDistance;
    }

    public float getSpeed() {
        return speed;
    }

    public float getPace() {
        return pace;
    }

    public Float getAltitude() {
        return altitude;
    }

    public Float getHeartRate() {
        return heartRate;
    }

    public Float getCadence() {
        return cadence;
    }

    public Float getPower() {
        return power;
    }
}
