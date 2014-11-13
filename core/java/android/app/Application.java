/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.app;

import java.util.ArrayList;

import android.content.ComponentCallbacks;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

/**
 * Base class for those who need to maintain global application state. You can
 * provide your own implementation by specifying its name in your
 * AndroidManifest.xml's &lt;application&gt; tag, which will cause that class
 * to be instantiated for you when the process for your application/package is
 * created.
 * 
 * <p class="note">There is normally no need to subclass Application.  In
 * most situation, static singletons can provide the same functionality in a
 * more modular way.  If your singleton needs a global context (for example
 * to register broadcast receivers), the function to retrieve it can be
 * given a {@link android.content.Context} which internally uses
 * {@link android.content.Context#getApplicationContext() Context.getApplicationContext()}
 * when first constructing the singleton.</p>
 */
public class Application extends ContextWrapper implements ComponentCallbacks2 {
    private ArrayList<ComponentCallbacks> mComponentCallbacks =
            new ArrayList<ComponentCallbacks>();
    private ArrayList<ActivityLifecycleCallbacks> mActivityLifecycleCallbacks =
            new ArrayList<ActivityLifecycleCallbacks>();
    private ArrayList<OnProvideAssistDataListener> mAssistCallbacks = null;

    /** @hide */
    public LoadedApk mLoadedApk;

    public interface ActivityLifecycleCallbacks {
        void onActivityCreated(Activity activity, Bundle savedInstanceState);
        void onActivityStarted(Activity activity);
        void onActivityResumed(Activity activity);
        void onActivityPaused(Activity activity);
        void onActivityStopped(Activity activity);
        void onActivitySaveInstanceState(Activity activity, Bundle outState);
        void onActivityDestroyed(Activity activity);
    }

    /**
     * Callback interface for use with {@link Application#registerOnProvideAssistDataListener}
     * and {@link Application#unregisterOnProvideAssistDataListener}.
     */
    public interface OnProvideAssistDataListener {
        /**
         * This is called when the user is requesting an assist, to build a full
         * {@link Intent#ACTION_ASSIST} Intent with all of the context of the current
         * application.  You can override this method to place into the bundle anything
         * you would like to appear in the {@link Intent#EXTRA_ASSIST_CONTEXT} part
         * of the assist Intent.
         */
        public void onProvideAssistData(Activity activity, Bundle data);
    }

    public Application() {
        super(null);
    }

    /**
     * Called when the application is starting, before any activity, service,
     * or receiver objects (excluding content providers) have been created.
     * Implementations should be as quick as possible (for example using 
     * lazy initialization of state) since the time spent in this function
     * directly impacts the performance of starting the first activity,
     * service, or receiver in a process.
     * If you override this method, be sure to call super.onCreate().
     */
    public void onCreate() {
    }

    /**
     * This method is for use in emulated process environments.  It will
     * never be called on a production Android device, where processes are
     * removed by simply killing them; no user code (including this callback)
     * is executed when doing so.
     */
    public void onTerminate() {
    }

    public void onConfigurationChanged(Configuration newConfig) {
        Object[] callbacks = collectComponentCallbacks();
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                ((ComponentCallbacks)callbacks[i]).onConfigurationChanged(newConfig);
            }
        }
    }

    public void onLowMemory() {
        Object[] callbacks = collectComponentCallbacks();
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                ((ComponentCallbacks)callbacks[i]).onLowMemory();
            }
        }
    }

    public void onTrimMemory(int level) {
        Object[] callbacks = collectComponentCallbacks();
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                Object c = callbacks[i];
                if (c instanceof ComponentCallbacks2) {
                    ((ComponentCallbacks2)c).onTrimMemory(level);
                }
            }
        }
    }

    public void registerComponentCallbacks(ComponentCallbacks callback) {
        synchronized (mComponentCallbacks) {
            mComponentCallbacks.add(callback);
        }
    }

    public void unregisterComponentCallbacks(ComponentCallbacks callback) {
        synchronized (mComponentCallbacks) {
            mComponentCallbacks.remove(callback);
        }
    }

    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        synchronized (mActivityLifecycleCallbacks) {
            mActivityLifecycleCallbacks.add(callback);
        }
    }

    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        synchronized (mActivityLifecycleCallbacks) {
            mActivityLifecycleCallbacks.remove(callback);
        }
    }

    public void registerOnProvideAssistDataListener(OnProvideAssistDataListener callback) {
        synchronized (this) {
            if (mAssistCallbacks == null) {
                mAssistCallbacks = new ArrayList<OnProvideAssistDataListener>();
            }
            mAssistCallbacks.add(callback);
        }
    }

    public void unregisterOnProvideAssistDataListener(OnProvideAssistDataListener callback) {
        synchronized (this) {
            if (mAssistCallbacks != null) {
                mAssistCallbacks.remove(callback);
            }
        }
    }

    // ------------------ Internal API ------------------
    
    /**
     * @hide
     */
    /* package */ final void attach(Context context) {
        attachBaseContext(context);
        mLoadedApk = ContextImpl.getImpl(context).mPackageInfo;
    }

    /* package */ void dispatchActivityCreated(Activity activity, Bundle savedInstanceState) {
        changeState(ActivityState.CREATE, activity, savedInstanceState);
    }

    /* package */ void dispatchActivityStarted(Activity activity) {
        changeState(ActivityState.START, activity, null);
    }

    /* package */ void dispatchActivityResumed(Activity activity) {
        changeState(ActivityState.RESUME, activity, null);
    }

    /* package */ void dispatchActivityPaused(Activity activity) {
        changeState(ActivityState.PAUSE, activity, null);
    }

    /* package */ void dispatchActivityStopped(Activity activity) {
        changeState(ActivityState.STOP, activity, null);
    }

    /* package */ void dispatchActivitySaveInstanceState(Activity activity, Bundle outState) {
        changeState(ActivityState.SAVE_INSTANCE, activity, outState);
    }

    /* package */ void dispatchActivityDestroyed(Activity activity) {
        changeState(ActivityState.DESTROY, activity, null);
    }
    
    private void changeState(ActivityState state, Activity activity, Bundle bundle) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                state.exec((ActivityLifecycleCallbacks)callbacks[i], activity, bundle);
            }
        }
    }

    private Object[] collectComponentCallbacks() {
        Object[] callbacks = null;
        synchronized (mComponentCallbacks) {
            if (mComponentCallbacks.size() > 0) {
                callbacks = mComponentCallbacks.toArray();
            }
        }
        return callbacks;
    }

    private Object[] collectActivityLifecycleCallbacks() {
        Object[] callbacks = null;
        synchronized (mActivityLifecycleCallbacks) {
            if (mActivityLifecycleCallbacks.size() > 0) {
                callbacks = mActivityLifecycleCallbacks.toArray();
            }
        }
        return callbacks;
    }

    /* package */ void dispatchOnProvideAssistData(Activity activity, Bundle data) {
        Object[] callbacks;
        synchronized (this) {
            if (mAssistCallbacks == null) {
                return;
            }
            callbacks = mAssistCallbacks.toArray();
        }
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                ((OnProvideAssistDataListener)callbacks[i]).onProvideAssistData(activity, data);
            }
        }
    }
    
    private enum ActivityState {
    	CREATE {
            @Override
            void exec(ActivityLifecycleCallbacks callback, Activity activity, Bundle bundle) {
                callback.onActivityCreated(activity, bundle);
            }
        },
    	START {
            @Override
            void exec(ActivityLifecycleCallbacks callback, Activity activity, Bundle bundle) {
                callback.onActivityStarted(activity);
            }
    	},
    	RESUME {
            @Override
            void exec(ActivityLifecycleCallbacks callback, Activity activity, Bundle bundle) {
                callback.onActivityResumed(activity);
            }
    	},
    	PAUSE {
            @Override
            void exec(ActivityLifecycleCallbacks callback, Activity activity, Bundle bundle) {
                callback.onActivityPaused(activity);
            }
    	},
    	STOP {
            @Override
            void exec(ActivityLifecycleCallbacks callback, Activity activity, Bundle bundle) {
                callback.onActivityStopped(activity);
            }
    	},
    	SAVE_INSTANCE {
            @Override
            void exec(ActivityLifecycleCallbacks callback, Activity activity, Bundle bundle) {
                callback.onActivitySaveInstanceState(activity, bundle);
            }
    	},
    	DESTROY {
            @Override
            void exec(ActivityLifecycleCallbacks callback, Activity activity, Bundle bundle) {
                callback.onActivityDestroyed(activity);
            }
        };
    	
        abstract void exec(ActivityLifecycleCallbacks callback, Activity activity, Bundle bundle);
    }
}
