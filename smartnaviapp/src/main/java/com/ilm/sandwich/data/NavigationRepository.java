package com.ilm.sandwich.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Singleton repository holding the current navigation state.
 * Thread-safe: uses postValue for updates from sensor/background threads.
 */
public class NavigationRepository {

    private static NavigationRepository instance;

    private final MutableLiveData<NavigationState> state = new MutableLiveData<>(NavigationState.DEFAULT);

    private NavigationRepository() {}

    public static synchronized NavigationRepository getInstance() {
        if (instance == null) {
            instance = new NavigationRepository();
        }
        return instance;
    }

    public LiveData<NavigationState> getState() {
        return state;
    }

    public NavigationState getCurrentState() {
        NavigationState s = state.getValue();
        return s != null ? s : NavigationState.DEFAULT;
    }

    public void updatePosition(double lat, double lon) {
        state.postValue(getCurrentState().withPosition(lat, lon));
    }

    public void updateFromStep(double lat, double lon, double azimuth, int stepCount) {
        state.postValue(getCurrentState().withStep(lat, lon, azimuth, stepCount));
    }

    public void updateGpsError(float error) {
        state.postValue(getCurrentState().withGpsError(error));
    }

    public void updateAltitude(int altitude) {
        state.postValue(getCurrentState().withAltitude(altitude));
    }

    public void reset() {
        state.postValue(NavigationState.DEFAULT);
    }
}
