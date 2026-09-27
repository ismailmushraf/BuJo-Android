package com.ismailmushraf.bujo;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ismailmushraf.bujo.coach.CoachProtocol;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CoachProtocolTest {
    private JSONObject snapshot() throws Exception { return new JSONObject().put("projects",new JSONArray().put(new JSONObject().put("id",7))); }
    private JSONObject answer() throws Exception { return new JSONObject().put("message","Start small.").put("suggestions",new JSONArray().put(new JSONObject().put("title","Outline the next step").put("project_id",7).put("reason","Highest priority."))); }
    @Test public void acceptsValidatedProjectSuggestion() throws Exception { CoachProtocol.validate(answer(),snapshot()); assertEquals("application/json",CoachProtocol.request(snapshot()).getJSONObject("generationConfig").getString("responseMimeType")); }
    @Test(expected=JSONException.class) public void rejectsUnknownProject() throws Exception { JSONObject invalid=answer(); invalid.getJSONArray("suggestions").getJSONObject(0).put("project_id",99); CoachProtocol.validate(invalid,snapshot()); }
}
