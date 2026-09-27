package com.ismailmushraf.bujo;

import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ismailmushraf.bujo.fragments.ProfileFragment;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ProfileScreenTest {
    @Test public void breakdownTogglesAndProfileCanBeReopened() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                for (int visit = 0; visit < 2; visit++) {
                    ProfileFragment fragment = new ProfileFragment();
                    activity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment).commitNow();
                    View root = fragment.requireView();
                    assertFalse(((TextView) root.findViewById(R.id.tv_rank_title)).getText().toString().isEmpty());
                    View breakdown = root.findViewById(R.id.layout_points_breakdown);
                    assertEquals(View.GONE, breakdown.getVisibility());
                    root.findViewById(R.id.layout_rank_box).performClick();
                    assertEquals(View.VISIBLE, breakdown.getVisibility());
                    assertNotNull(root.findViewById(R.id.iv_expand_chevron));
                    root.findViewById(R.id.layout_rank_box).performClick();
                    assertEquals(View.GONE, breakdown.getVisibility());
                }
            });
        }
    }
}
