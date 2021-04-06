/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import de.dennisguse.opentracks.util.TTSUtils;

import static de.dennisguse.opentracks.util.SystemUtils.getNavBarHeight;
import static de.dennisguse.opentracks.util.SystemUtils.isVendorXiaomi;

/**
 * @author Jimmy Shih
 */
public abstract class AbstractActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        int height = dm.heightPixels;
        Log.d("AbstractActivity", Integer.toString(height));
        if (isVendorXiaomi(this)) {
            height += getNavBarHeight(this) / 2 - 16;
            Log.d("AbstractActivity", Integer.toString(height));
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.height = height;

            this.getWindow().setAttributes(layoutParams);
        }

        // Set volume control stream for text to speech
        setVolumeControlStream(TTSUtils.getTTSStream());

        setContentView(getRootView());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupActionBarBack(toolbar);

        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //window.setStatusBarColor(Color.rgb(0xf5, 0xf5, 0xf5));
            // change the toolbar color to match the primary color
            window.setStatusBarColor(this.getResources().getColor(R.color.colorAccent, this.getTheme()));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    protected abstract View getRootView();

    protected void setupActionBarBack(@Nullable Toolbar toolbar) {
        setSupportActionBar(toolbar);
        if (toolbar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
}
