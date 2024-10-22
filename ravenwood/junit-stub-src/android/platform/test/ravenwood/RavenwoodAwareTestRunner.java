/*
 * Copyright (C) 2024 The Android Open Source Project
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
package android.platform.test.ravenwood;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import android.annotation.Nullable;
import android.util.Log;

import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.InvalidOrderingException;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Orderable;
import org.junit.runner.manipulation.Orderer;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.util.function.BiConsumer;

/**
 * A simple pass-through runner that just delegates to the inner runner without doing
 * anything special (no hooks, etc.).
 *
 * This is only used when a real device-side test has Ravenizer enabled.
 */
public class RavenwoodAwareTestRunner extends Runner implements Filterable, Orderable {
    private static final String TAG = "Ravenwood";

    @Inherited
    @Target({TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InnerRunner {
        Class<? extends Runner> value();
    }

    /**
     * An annotation similar to JUnit's BeforeClass, but this gets executed before
     * the inner runner is instantiated, and only on Ravenwood.
     * It can be used to initialize what's needed by the inner runner.
     */
    @Target({METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RavenwoodTestRunnerInitializing {
    }

    private static class NopRule implements TestRule {
        @Override
        public Statement apply(Statement base, Description description) {
            return base;
        }
    }

    public static final TestRule sImplicitClassOuterRule = new NopRule();
    public static final TestRule sImplicitClassInnerRule = new NopRule();
    public static final TestRule sImplicitInstOuterRule = new NopRule();
    public static final TestRule sImplicitInstInnerRule = new NopRule();

    public static final String IMPLICIT_CLASS_OUTER_RULE_NAME = "sImplicitClassOuterRule";
    public static final String IMPLICIT_CLASS_INNER_RULE_NAME = "sImplicitClassInnerRule";
    public static final String IMPLICIT_INST_OUTER_RULE_NAME = "sImplicitInstOuterRule";
    public static final String IMPLICIT_INST_INNER_RULE_NAME = "sImplicitInstInnerRule";

    private final Runner mRealRunner;
    private final TestClass mTestClass;

    public RavenwoodAwareTestRunner(Class<?> testClass) {
        mTestClass = new TestClass(testClass);

        Log.v(TAG, "RavenwoodAwareTestRunner starting for " + testClass.getCanonicalName());

        // Find the real runner.
        final Class<? extends Runner> realRunnerClass;
        final InnerRunner innerRunnerAnnotation = mTestClass.getAnnotation(InnerRunner.class);
        if (innerRunnerAnnotation != null) {
            realRunnerClass = innerRunnerAnnotation.value();
        } else {
            // Default runner.
            realRunnerClass = BlockJUnit4ClassRunner.class;
        }

        try {
            Log.i(TAG, "Initializing the inner runner: " + realRunnerClass);
            mRealRunner = instantiateRealRunner(realRunnerClass, testClass);
        } catch (InstantiationException | IllegalAccessException
                 | InvocationTargetException | NoSuchMethodException e) {
            throw logAndFail("Failed to instantiate " + realRunnerClass, e);
        }
    }

    private Runner instantiateRealRunner(
            Class<? extends Runner> realRunnerClass,
            Class<?> testClass)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {
        try {
            return realRunnerClass.getConstructor(Class.class).newInstance(testClass);
        } catch (NoSuchMethodException e) {
            return realRunnerClass.getConstructor(Class.class, RunnerBuilder.class)
                    .newInstance(testClass, new AllDefaultPossibilitiesBuilder());
        }
    }

    private Error logAndFail(String message, Throwable exception) {
        Log.e(TAG, message, exception);
        throw new AssertionError(message, exception);
    }

    @Override
    public Description getDescription() {
        return mRealRunner.getDescription();
    }

    @Override
    public void run(RunNotifier notifier) {
        mRealRunner.run(notifier);
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        if (mRealRunner instanceof Filterable r) {
            r.filter(filter);
        }
    }

    @Override
    public void order(Orderer orderer) throws InvalidOrderingException {
        if (mRealRunner instanceof Orderable r) {
            r.order(orderer);
        }
    }

    @Override
    public void sort(Sorter sorter) {
        if (mRealRunner instanceof Sortable r) {
            r.sort(sorter);
        }
    }

    static void onRavenwoodRuleEnter(Description description, RavenwoodRule rule) {
    }

    static void onRavenwoodRuleExit(Description description, RavenwoodRule rule) {
    }

    /**
     * Contains Ravenwood private APIs.
     */
    public static class RavenwoodPrivate {
        private RavenwoodPrivate() {
        }

        /**
         * Set a listener for onCriticalError(), for testing. If a listener is set, we won't call
         * System.exit().
         */
        public void setCriticalErrorHandler(@Nullable BiConsumer<String, Throwable> handler) {
        }
    }

    private static final RavenwoodPrivate sRavenwoodPrivate = new RavenwoodPrivate();

    public static RavenwoodPrivate private$ravenwood() {
        return sRavenwoodPrivate;
    }
}
