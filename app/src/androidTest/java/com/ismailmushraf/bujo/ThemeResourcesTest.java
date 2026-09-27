package com.ismailmushraf.bujo;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ThemeResourcesTest {
    @Test public void controlsKeepThemeAfterRepeatedInflation() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
            for (int mode : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES,
                    Configuration.UI_MODE_NIGHT_YES, Configuration.UI_MODE_NIGHT_NO}) {
                Configuration config = new Configuration(app.getResources().getConfiguration());
                config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | mode;
                Context context = new androidx.appcompat.view.ContextThemeWrapper(
                        app.createConfigurationContext(config), R.style.Theme_BuJo);
                LayoutInflater inflater = LayoutInflater.from(context);
                TextView selected = (TextView) inflater.inflate(R.layout.item_bb10_spinner, null);
                TextView popup = (TextView) inflater.inflate(R.layout.item_bb10_spinner_dropdown, null);
                int textColor = context.getResources().getColor(R.color.bujo_text);
                assertEquals(textColor, selected.getCurrentTextColor());
                assertEquals(textColor, popup.getCurrentTextColor());
                View plan = inflater.inflate(R.layout.fragment_daily_log, null);
                assertEquals(textColor, ((TextView) plan.findViewById(R.id.btn_plan_tomorrow)).getCurrentTextColor());
                View workout = inflater.inflate(R.layout.header_workout, null);
                assertEquals(textColor, ((TextView) workout.findViewById(R.id.btn_add_set)).getCurrentTextColor());
                assertNotEquals(textColor, context.getResources().getColor(R.color.bb10_button_background));
                inflater.inflate(R.layout.fragment_settings, null);
                View coach = inflater.inflate(R.layout.dialog_coach_wizard, null);
                assertEquals(textColor, ((TextView)coach.findViewById(R.id.coach_wizard_message)).getCurrentTextColor());
                inflater.inflate(R.layout.settings_coach_section, null);
                inflater.inflate(R.layout.dialog_plan_day, null);
                View profile = inflater.inflate(R.layout.fragment_profile, null);
                assertEquals(textColor, ((TextView) profile.findViewById(R.id.tv_rank_title)).getCurrentTextColor());
                androidx.appcompat.widget.AppCompatImageView rank = profile.findViewById(R.id.tv_rank_icon);
                for (int icon : new int[]{R.drawable.ic_bb10_profile_wanderer,
                        R.drawable.ic_bb10_profile_book, R.drawable.ic_bb10_profile_scholar,
                        R.drawable.ic_bb10_profile_mountain, R.drawable.ic_bb10_profile_temple,
                        R.drawable.ic_bb10_profile_flame, R.drawable.ic_bb10_profile_battery,
                        R.drawable.ic_bb10_profile_trophy, R.drawable.ic_bb10_profile_star}) {
                    rank.setImageResource(icon);
                    assertNotNull(rank.getDrawable());
                }
                assertEquals(context.getResources().getColor(R.color.bb10_blue),
                        androidx.core.widget.ImageViewCompat.getImageTintList(rank).getDefaultColor());
                assertEquals(View.GONE, profile.findViewById(R.id.layout_points_breakdown).getVisibility());
                View goal = inflater.inflate(R.layout.item_goal_efficiency, null);
                assertEquals(textColor, ((TextView) goal.findViewById(R.id.tv_project_name)).getCurrentTextColor());
            }
        });
    }
}
