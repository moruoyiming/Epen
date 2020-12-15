package com.tstudy.students;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.util.Log;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public static final String TAG = "ApplicationTest_tag";
    public ApplicationTest() {
        super(Application.class);

   int [] array = new int[2];

        Log.d(TAG, "ApplicationTest: aaa"+array[0]);

    }


}