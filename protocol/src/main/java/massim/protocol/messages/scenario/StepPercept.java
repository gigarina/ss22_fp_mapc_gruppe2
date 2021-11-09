package massim.protocol.messages.scenario;

import massim.protocol.data.Position;
import massim.protocol.messages.RequestActionMessage;
import massim.protocol.data.TaskInfo;
import massim.protocol.data.Thing;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class StepPercept extends RequestActionMessage {

    public Set<Thing> things = new HashSet<>();
    public Set<TaskInfo> taskInfo = new HashSet<>();
    public Map<String, Set<Position>> terrain = new HashMap<>();
    public long score;
    public String lastAction;
    public String lastActionResult;
    public List<String> lastActionParams = new ArrayList<>();
    public Set<Position> attachedThings = new HashSet<>();
    public int energy;
    public boolean deactivated;
    public String role;
    public JSONArray stepEvents;

    public StepPercept(JSONObject content) {
        super(content);
        parsePercept(content.getJSONObject("percept"));
    }

    public StepPercept(int step, long score, Set<Thing> things, Map<String, Set<Position>> terrain,
                       Set<TaskInfo> taskInfo, String action, List<String> lastActionParams, String result,
                       Set<Position> attachedThings, JSONArray stepEvents, String role) {
        super(System.currentTimeMillis(), -1, -1, step); // id and deadline are updated later
        this.score = score;
        this.things.addAll(things);
        this.taskInfo.addAll(taskInfo);
        this.lastAction = action;
        this.lastActionResult = result;
        this.terrain = terrain;
        this.lastActionParams.addAll(lastActionParams);
        this.attachedThings = attachedThings;
        this.stepEvents = stepEvents;
        this.role = role;
    }

    @Override
    public JSONObject makePercept() {
        var percept = new JSONObject();
        var jsonThings = new JSONArray();
        var jsonTasks = new JSONArray();
        var jsonTerrain = new JSONObject();
        percept.put("score", score);
        percept.put("things", jsonThings);
        percept.put("tasks", jsonTasks);
        percept.put("terrain", jsonTerrain);
        percept.put("energy", energy);
        percept.put("deactivated", deactivated);
        things.forEach(t -> jsonThings.put(t.toJSON()));
        taskInfo.forEach(t -> jsonTasks.put(t.toJSON()));
        terrain.forEach((t, positions) -> {
            JSONArray jsonPositions = new JSONArray();
            positions.forEach(p -> jsonPositions.put(p.toJSON()));
            jsonTerrain.put(t, jsonPositions);
        });
        percept.put("lastAction", lastAction);
        percept.put("lastActionResult", lastActionResult);
        var params = new JSONArray();
        lastActionParams.forEach(params::put);
        percept.put("lastActionParams", params);
        JSONArray attached = new JSONArray();
        attachedThings.forEach(a -> {
            JSONArray pos = new JSONArray();
            pos.put(a.x);
            pos.put(a.y);
            attached.put(pos);
        });
        percept.put("attached", attached);

        percept.put("events", stepEvents != null? stepEvents : new JSONArray());

        percept.put("role", this.role);

        return percept;
    }

    private void parsePercept(JSONObject percept) {
        score = percept.getLong("score");
        JSONArray jsonThings = percept.getJSONArray("things");
        JSONArray jsonTasks = percept.getJSONArray("tasks");
        for (int i = 0; i < jsonThings.length(); i++) {
            JSONObject jsonThing = jsonThings.getJSONObject(i);
            things.add(Thing.fromJson(jsonThing));
        }
        for (int i = 0; i < jsonTasks.length(); i++) {
            JSONObject jsonTask = jsonTasks.getJSONObject(i);
            taskInfo.add(TaskInfo.fromJson(jsonTask));
        }
        lastAction = percept.getString("lastAction");
        lastActionResult = percept.getString("lastActionResult");
        JSONObject jsonTerrain = percept.getJSONObject("terrain");
        jsonTerrain.keys().forEachRemaining(t -> {
            Set<Position> positions = new HashSet<>();
            JSONArray jsonPositions = jsonTerrain.getJSONArray(t);
            for (int i = 0; i < jsonPositions.length(); i++) {
                positions.add(Position.fromJSON(jsonPositions.getJSONArray(i)));
            }
            terrain.put(t, positions);
        });
        var params = percept.getJSONArray("lastActionParams");
        for (int i = 0; i < params.length(); i++) lastActionParams.add(params.getString(i));
        JSONArray jsonAttached = percept.getJSONArray("attached");
        for (int i = 0; i < jsonAttached.length(); i++) {
            JSONArray pos = jsonAttached.getJSONArray(i);
            attachedThings.add(Position.of(pos.getInt(0), pos.getInt(1)));
        }

        energy = percept.getInt("energy");
        deactivated = percept.getBoolean("deactivated");

        var stepEvents = percept.optJSONArray("events");
        this.stepEvents = stepEvents != null? stepEvents : new JSONArray();

        this.role = percept.getString("role");
    }
}
