/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.startop.test;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Trace;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.Arrays;

class Benchmark {
    // Time limit to run benchmarks in seconds
    public static final int TIME_LIMIT = 5;

    public Benchmark(ViewGroup parent, CharSequence name, Runnable thunk) {
        Context context = parent.getContext();
        Button button = new Button(context);
        TextView mean = new TextView(context);
        TextView stdev = new TextView(context);

        button.setText(name);
        mean.setText("");
        stdev.setText("");

        button.setOnClickListener((_button) -> {
            mean.setText("Running...");
            stdev.setText("");

            new AsyncTask() {
                double resultMean = 0;
                double resultStdev = 0;

                @Override
                protected Object doInBackground(Object... _args) {
                    long startTime = System.nanoTime();
                    int count = 0;

                    // Run benchmark
                    while (true) {
                        long elapsed = -System.nanoTime();
                        thunk.run();
                        elapsed += System.nanoTime();

                        count++;
                        double elapsedVariance = (double) elapsed - resultMean;
                        resultMean += elapsedVariance / count;
                        resultStdev += elapsedVariance * ((double) elapsed - resultMean);

                        if (System.nanoTime() - startTime > TIME_LIMIT * 1e9) {
                            break;
                        }
                    }
                    resultStdev = Math.sqrt(resultStdev / (count - 1));

                    return null;
                }

                @Override
                protected void onPostExecute(Object _result) {
                    mean.setText(String.format("%.3f", resultMean / 1e6));
                    stdev.setText(String.format("%.3f", resultStdev / 1e6));
                }
            }.execute(new Object());
        });

        parent.addView(button);
        parent.addView(mean);
        parent.addView(stdev);
    }
}

public class InteractiveBenchmarkActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.interactive_benchmark_page);

        GridLayout benchmarkList = findViewById(R.id.benchmark_list);

        new Benchmark(benchmarkList, "Empty", () -> {
        });

        LayoutInflater inflater = LayoutInflater.from(this);
        new Benchmark(benchmarkList, "layout3", () -> {
            inflater.inflate(R.layout.layout3, null);
        });
    }
}
