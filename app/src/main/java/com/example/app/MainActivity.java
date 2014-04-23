package com.example.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import retrofit.MockRestAdapter;
import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends Activity {

    static final String TAG = MainActivity.class.getSimpleName();

    TextView resultView;

    interface ExampleService {
        @GET("/example") Observable<String> fetch(@Query("result") String result);
    }

    static class MockExampleService implements ExampleService {
        @Override public Observable<String> fetch(String result) {
            Log.e(MockExampleService.class.getSimpleName(), "called mock service for result: " + result);

            return Observable.just(result);
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultView = (TextView) findViewById(R.id.result);

        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("mock:///").build();
        MockRestAdapter mockRestAdapter = MockRestAdapter.from(restAdapter);

        ExampleService service = mockRestAdapter.create(ExampleService.class, new MockExampleService());

        // doesn't work when observed on main thread
        service.fetch("Main thread")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override public void onCompleted() {
                        Log.e(TAG, "Completed in main.");
                        Toast.makeText(MainActivity.this, "completed", LENGTH_SHORT).show();
                    }

                    @Override public void onError(Throwable e) {
                        Log.e(TAG, "Error in main.", e);
                        Toast.makeText(MainActivity.this, e.getMessage(), LENGTH_SHORT).show();
                    }

                    @Override public void onNext(String result) {
                        Log.e(TAG, "Next in main: " + result);
                        resultView.setText(result);
                    }
                });

        // but on background thread, it does
        service.fetch("Background thread")
                .subscribe(new Observer<String>() {
                    @Override public void onCompleted() {
                        Log.e(TAG, "Completed in background.");
                    }

                    @Override public void onError(Throwable e) {
                        Log.e(TAG, "Error in background.", e);
                    }

                    @Override public void onNext(String result) {
                        Log.e(TAG, "Next in background: " + result);
                    }
                });
    }
}
