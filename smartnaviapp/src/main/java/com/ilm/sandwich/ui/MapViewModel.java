package com.ilm.sandwich.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ilm.sandwich.data.NavigationRepository;
import com.ilm.sandwich.data.NavigationState;

/**
 * ViewModel for the main map activity.
 * Holds UI state and observes NavigationRepository for position updates.
 */
public class MapViewModel extends AndroidViewModel {

    private final NavigationRepository repository = NavigationRepository.getInstance();

    private final MutableLiveData<Boolean> followMe = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> satelliteView = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> speechOutput = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> vibration = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> autocorrectEnabled = new MutableLiveData<>(true);

    public MapViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<NavigationState> getNavigationState() {
        return repository.getState();
    }

    public NavigationRepository getRepository() {
        return repository;
    }

    // Follow me
    public LiveData<Boolean> getFollowMe() { return followMe; }
    public void setFollowMe(boolean value) { followMe.setValue(value); }

    // Satellite view
    public LiveData<Boolean> getSatelliteView() { return satelliteView; }
    public void setSatelliteView(boolean value) { satelliteView.setValue(value); }

    // Speech output
    public LiveData<Boolean> getSpeechOutput() { return speechOutput; }
    public void setSpeechOutput(boolean value) { speechOutput.setValue(value); }

    // Vibration
    public LiveData<Boolean> getVibration() { return vibration; }
    public void setVibration(boolean value) { vibration.setValue(value); }

    // Autocorrect
    public LiveData<Boolean> getAutocorrectEnabled() { return autocorrectEnabled; }
    public void setAutocorrectEnabled(boolean value) { autocorrectEnabled.setValue(value); }
}
