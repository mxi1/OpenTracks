package de.dennisguse.opentracks.content;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.time.Instant;

import de.dennisguse.opentracks.content.data.TrackPoint;

public class UITrackPoint {

    private final TrackPoint trackPoint;

    private final Double altitudeEGM2008;

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public UITrackPoint(@NonNull TrackPoint trackPoint, Double altitudeEGM2008) {
        this.trackPoint = trackPoint;
        this.altitudeEGM2008 = altitudeEGM2008 != null ? altitudeEGM2008 : 0;
    }

    public TrackPoint getTrackPoint() {
        return trackPoint;
    }

    public double getAltitudeEGM2008() {
        return altitudeEGM2008;
    }

    //Delegate to TrackPoint
    @NonNull
    public TrackPoint.Type getType() {
        return trackPoint.getType();
    }

    @NonNull
    public TrackPoint.Id getId() {
        //This is loaded from the database must be non-null!
        return trackPoint.getId();
    }

    public boolean hasLocation() {
        return trackPoint.hasLocation();
    }

    public double getLatitude() {
        return trackPoint.getLatitude();
    }

    public double getLongitude() {
        return trackPoint.getLongitude();
    }

    @NonNull
    public Location getLocation() {
        return trackPoint.getLocation();
    }

    public boolean hasAltitudeGain() {
        return trackPoint.hasAltitudeGain();
    }

    public float getAltitudeGain() {
        return trackPoint.getAltitudeGain();
    }

    public boolean hasAltitudeLoss() {
        return trackPoint.hasAltitudeLoss();
    }

    public float getAltitudeLoss() {
        return trackPoint.getAltitudeLoss();
    }

    public Instant getTime() {
        return trackPoint.getTime();
    }

    public boolean isRecent() {
        return trackPoint.isRecent();
    }

    public boolean hasAltitude() {
        return trackPoint.hasAltitude();
    }

    public double getAltitude() {
        return trackPoint.getAltitude();
    }

    public boolean hasSpeed() {
        return trackPoint.hasSpeed();
    }

    public float getSpeed() {
        return trackPoint.getSpeed();
    }

    public boolean isMoving() {
        return trackPoint.isMoving();
    }

    public boolean hasBearing() {
        return trackPoint.hasBearing();
    }

    public float getBearing() {
        return trackPoint.getBearing();
    }

    public boolean hasAccuracy() {
        return trackPoint.hasAccuracy();
    }

    public float getAccuracy() {
        return trackPoint.getAccuracy();
    }

    public boolean hasSensorDistance() {
        return trackPoint.hasSensorDistance();
    }

    public Float getSensorDistance() {
        return trackPoint.getSensorDistance();
    }

    public boolean hasHeartRate() {
        return trackPoint.hasHeartRate();
    }

    public float getHeartRate_bpm() {
        return trackPoint.getHeartRate_bpm();
    }

    public boolean hasCyclingCadence() {
        return trackPoint.hasCyclingCadence();
    }

    public float getCyclingCadence_rpm() {
        return trackPoint.getCyclingCadence_rpm();
    }

    public boolean hasPower() {
        return trackPoint.hasPower();
    }

    public float getPower() {
        return trackPoint.getPower();
    }
}