/*
* Copyright 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package com.example.android.recyclerview

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ViewAnimator

import com.example.android.common.activities.SampleActivityBase
import com.example.android.common.logger.Log
import com.example.android.common.logger.LogFragment
import com.example.android.common.logger.LogWrapper
import com.example.android.common.logger.MessageOnlyLogFilter

/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * [android.support.v4.app.Fragment] which can display a view.
 *
 *
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
class MainActivity : SampleActivityBase() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().run {
                replace(R.id.sample_content_fragment, RecyclerViewFragment())
                commit()
            }
        }
    }

    companion object {
        val TAG = "MainActivity"
    }
}
