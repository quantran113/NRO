package nro.models.item;

import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.utils.Util;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DropManager {

    private static DropManager instance;

    public static class DropRule {
        public int id;
        public int mapId; // -1 for all maps
        public int itemId;
        public int quantity;
        public int ratePercent; // e.g. 5 = 5%
        public boolean active = true;

        public DropRule(int id, int mapId, int itemId, int quantity, int ratePercent) {
            this.id = id;
            this.mapId = mapId;
            this.itemId = itemId;
            this.quantity = quantity;
            this.ratePercent = ratePercent;
        }

        public JSONObject toJSONObject() {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("mapId", mapId);
            json.put("itemId", itemId);
            json.put("quantity", quantity);
            json.put("ratePercent", ratePercent);
            json.put("active", active);
            return json;
        }
    }

    public static final List<DropRule> dropRules = new ArrayList<>();
    private static int autoId = 1;

    static {
        // Sample default clean drop rules
        dropRules.add(new DropRule(autoId++, -1, 189, 1000, 10)); // All maps drop Vàng 189 (10%)
        dropRules.add(new DropRule(autoId++, 0, 76, 500, 15));    // Map 0 drop Vàng 76 (15%)
        dropRules.add(new DropRule(autoId++, 5, 77, 1, 1));       // Map 5 drop Ngọc 77 (1%)
        dropRules.add(new DropRule(autoId++, 104, 933, 1, 5));    // Map 104 drop Mảnh Vỡ (5%)
    }

    public static DropManager gI() {
        if (instance == null) {
            instance = new DropManager();
        }
        return instance;
    }

    public List<ItemMap> processMobDrop(Player player, Zone zone, int mapId, int x, int yEnd) {
        List<ItemMap> list = new ArrayList<>();
        if (player == null || zone == null) {
            return list;
        }

        for (DropRule rule : dropRules) {
            if (!rule.active) continue;
            if (rule.mapId == -1 || rule.mapId == mapId) {
                if (Util.isTrue(rule.ratePercent, 100)) {
                    ItemMap itemMap = new ItemMap(zone, rule.itemId, rule.quantity, x, yEnd, player.id);
                    list.add(itemMap);
                }
            }
        }
        return list;
    }

    public synchronized void addRule(int mapId, int itemId, int quantity, int ratePercent) {
        dropRules.add(new DropRule(autoId++, mapId, itemId, quantity, ratePercent));
    }

    public synchronized boolean removeRule(int id) {
        return dropRules.removeIf(r -> r.id == id);
    }
}
