package com.dannicholson.climbingfitnessapp;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.test.espresso.idling.CountingIdlingResource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Keeps an in-progress save alive when the phone rotates.
public class SaveEntryViewModel extends ViewModel {
    enum State { READY, SAVING, SAVED, FAILED }
    static final CountingIdlingResource DATABASE_WORK =
            new CountingIdlingResource("Training database work");
    final MutableLiveData<State> state = new MutableLiveData<>(State.READY);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    void save(Runnable insert) {
        if (state.getValue() == State.SAVING || state.getValue() == State.SAVED) return;
        state.setValue(State.SAVING);
        DATABASE_WORK.increment();
        worker.execute(() -> {
            State result;
            try {
                insert.run();
                result = State.SAVED;
            } catch (Exception exception) {
                Log.e("SaveEntry", "Could not save entry", exception);
                result = State.FAILED;
            }
            State completed = result;
            main.post(() -> {
                try { state.setValue(completed); }
                finally { DATABASE_WORK.decrement(); }
            });
        });
    }

    @Override protected void onCleared() { worker.shutdown(); }
}
