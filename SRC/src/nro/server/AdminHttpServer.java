package nro.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import nro.models.data.LocalManager;

import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_system.Template.ItemOptionTemplate;
import nro.models.player_system.Template.ItemTemplate;
import nro.models.server.Client;
import nro.models.event.EventManager;
import nro.models.server.Manager;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.managers.GiftCodeManager;
import nro.models.player_system.GiftCode;
import nro.models.services.GiftCodeService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class AdminHttpServer {

    private static AdminHttpServer instance;
    private HttpServer server;

    public static AdminHttpServer gI() {
        if (instance == null) {
            instance = new AdminHttpServer();
        }
        return instance;
    }

    public void start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // Handlers
            server.createContext("/api/players", new PlayersHandler());
            server.createContext("/api/item-templates", new ItemTemplatesHandler());
            server.createContext("/api/option-templates", new OptionTemplatesHandler());
            server.createContext("/api/grant-item", new GrantItemHandler());
            server.createContext("/api/grant-item-batch", new GrantItemBatchHandler());
            server.createContext("/api/grant-pet", new GrantPetHandler());
            server.createContext("/api/server-events", new ServerEventsHandler());
            server.createContext("/api/drop-rules", new DropRulesHandler());
            server.createContext("/api/map-templates", new MapTemplatesHandler());
            server.createContext("/api/npc-shops", new NpcShopsHandler());
            server.createContext("/api/next-task", new NextTaskHandler());
            server.createContext("/api/rename-player", new RenamePlayerHandler());
            server.createContext("/api/reload-giftcode", new ReloadGiftCodeHandler());
            server.createContext("/api/use-giftcode", new UseGiftCodeHandler());
            server.createContext("/api/adjust-player-power", new AdjustPlayerPowerHandler());
            server.createContext("/api/adjust-player-event-point", new AdjustPlayerEventPointHandler());
            server.createContext("/api/manage-bots", new ManageBotsHandler());
            server.createContext("/api/bosses", new BossesHandler());
            server.createContext("/api/bosses/spawn", new SpawnBossHandler());
            server.createContext("/api/bosses/action", new BossActionHandler());
            server.createContext("/api/player-inventory", new PlayerInventoryHandler());
            server.createContext("/api/badges-tasks", new BadgesTasksHandler());
            server.createContext("/api/complete-badges-task", new CompleteBadgesTaskHandler());

            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            System.out.println(">>> [Web Admin API Server] Running on http://127.0.0.1:" + port);
        } catch (Exception e) {
            System.err.println("Lỗi khi khởi chạy AdminHttpServer: " + e.getMessage());
        }
    }

    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) {
        try {
            setCorsHeaders(exchange);
            byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- GET /api/players ---
    private static class PlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                JSONArray arr = new JSONArray();
                // Online players
                List<Player> onlineList = Client.gI().getPlayers();
                for (Player p : onlineList) {
                    if (p != null && p.name != null) {
                        JSONObject obj = new JSONObject();
                        obj.put("id", p.id);
                        obj.put("name", p.name);
                        obj.put("gender", p.gender);
                        obj.put("online", true);
                        arr.add(obj);
                    }
                }

                // Fetch offline players from DB (top 100)
                try (Connection conn = LocalManager.getConnection();
                        PreparedStatement ps = conn.prepareStatement("SELECT id, name, gender FROM player LIMIT 100")) {
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        long pid = rs.getLong("id");
                        String name = rs.getString("name");
                        int gender = rs.getInt("gender");

                        boolean isOnline = false;
                        for (Player p : onlineList) {
                            if (p != null && p.id == pid) {
                                isOnline = true;
                                break;
                            }
                        }
                        if (!isOnline) {
                            JSONObject obj = new JSONObject();
                            obj.put("id", pid);
                            obj.put("name", name);
                            obj.put("gender", gender);
                            obj.put("online", false);
                            arr.add(obj);
                        }
                    }
                }

                sendJsonResponse(exchange, 200, arr.toJSONString());
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- GET /api/item-templates ---
    private static class ItemTemplatesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                JSONArray arr = new JSONArray();
                for (ItemTemplate temp : Manager.ITEM_TEMPLATES) {
                    if (temp != null) {
                        JSONObject obj = new JSONObject();
                        obj.put("id", temp.id);
                        obj.put("name", temp.name);
                        obj.put("type", temp.type);
                        obj.put("gender", temp.gender);
                        obj.put("description", temp.description != null ? temp.description : "");
                        obj.put("iconID", temp.iconID);

                        Item tempItem = ItemService.gI().createNewItem(temp.id, 1);
                        JSONArray defaultOptsArr = new JSONArray();
                        if (tempItem != null && tempItem.itemOptions != null) {
                            for (Item.ItemOption io : tempItem.itemOptions) {
                                JSONObject optObj = new JSONObject();
                                optObj.put("id", io.optionTemplate.id);
                                optObj.put("param", io.param);
                                defaultOptsArr.add(optObj);
                            }
                        }
                        obj.put("defaultOptions", defaultOptsArr);

                        arr.add(obj);
                    }
                }
                sendJsonResponse(exchange, 200, arr.toJSONString());
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- GET /api/option-templates ---
    private static class OptionTemplatesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                JSONArray arr = new JSONArray();
                for (ItemOptionTemplate temp : Manager.ITEM_OPTION_TEMPLATES) {
                    if (temp != null) {
                        JSONObject obj = new JSONObject();
                        obj.put("id", temp.id);
                        obj.put("name", temp.name);
                        arr.add(obj);
                    }
                }
                sendJsonResponse(exchange, 200, arr.toJSONString());
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- POST /api/grant-item ---
    private static class GrantItemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                JSONObject body = (JSONObject) JSONValue.parse(reader);

                if (body == null) {
                    sendJsonResponse(exchange, 400,
                            "{\"status\": \"error\", \"message\": \"Dữ liệu yêu cầu không hợp lệ\"}");
                    return;
                }

                String playerName = (String) body.get("playerName");
                long itemId = ((Number) body.get("itemId")).longValue();
                int quantity = body.containsKey("quantity") ? ((Number) body.get("quantity")).intValue() : 1;
                int starCount = body.containsKey("stars") ? ((Number) body.get("stars")).intValue() : 0;
                JSONArray optionsReq = (JSONArray) body.get("options");

                if (playerName == null || playerName.trim().isEmpty()) {
                    sendJsonResponse(exchange, 400,
                            "{\"status\": \"error\", \"message\": \"Tên nhân vật không được để trống\"}");
                    return;
                }

                // Check if player is ONLINE
                Player targetPlayer = null;
                for (Player p : Client.gI().getPlayers()) {
                    if (p != null && p.name != null && p.name.equalsIgnoreCase(playerName.trim())) {
                        targetPlayer = p;
                        break;
                    }
                }

                if (targetPlayer != null) {
                    // --- PLAYER IS ONLINE ---
                    Item item = ItemService.gI().createNewItem((short) itemId, quantity);
                    if (item == null) {
                        sendJsonResponse(exchange, 400,
                                "{\"status\": \"error\", \"message\": \"Vật phẩm không tồn tại (ID: " + itemId
                                        + ")\"}");
                        return;
                    }

                    // Custom options -> clear defaults (đồ trắng) and use only custom
                    // No custom options -> keep defaults from createNewItem (đồ gốc game)
                    if (optionsReq != null) {
                        item.itemOptions.clear();
                        for (Object o : optionsReq) {
                            JSONObject optObj = (JSONObject) o;
                            int optId = ((Number) optObj.get("id")).intValue();
                            int param = ((Number) optObj.get("param")).intValue();
                            item.itemOptions.add(new Item.ItemOption(optId, param));
                        }
                    }

                    // Add star slots option (107)
                    if (starCount > 0) {
                        item.itemOptions.add(new Item.ItemOption(107, starCount));
                    }

                    boolean added = InventoryService.gI().addItemBag(targetPlayer, item);
                    if (added) {
                        InventoryService.gI().sendItemBags(targetPlayer);
                        Service.gI().sendThongBao(targetPlayer,
                                "Admin vừa cấp cho bạn: " + item.template.name + " (x" + quantity + ")");

                        JSONObject res = new JSONObject();
                        res.put("status", "success");
                        res.put("message", "Đã cấp thành công [" + item.template.name + "] cho nhân vật "
                                + targetPlayer.name + " (Đang ONLINE)");
                        sendJsonResponse(exchange, 200, res.toJSONString());
                    } else {
                        sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Hành trang nhân vật "
                                + targetPlayer.name + " đã đầy!\"}");
                    }
                } else {
                    // --- PLAYER IS OFFLINE ---
                    try (Connection conn = LocalManager.getConnection();
                            PreparedStatement psSelect = conn
                                    .prepareStatement("SELECT id, items_bag FROM player WHERE name = ?")) {
                        psSelect.setString(1, playerName.trim());
                        ResultSet rs = psSelect.executeQuery();
                        if (rs.next()) {
                            long pid = rs.getLong("id");
                            String itemsBagStr = rs.getString("items_bag");

                            JSONArray bagArray;
                            if (itemsBagStr != null && !itemsBagStr.trim().isEmpty()) {
                                bagArray = (JSONArray) JSONValue.parse(itemsBagStr);
                            } else {
                                bagArray = new JSONArray();
                            }

                            // Create JSON item structure for offline bag according to PlayerDAO format
                            JSONArray dataItem = new JSONArray();
                            dataItem.add((int) itemId);
                            dataItem.add(quantity);

                            JSONArray optionsArr = new JSONArray();
                            if (optionsReq != null && !optionsReq.isEmpty()) {
                                // Custom options -> đồ trắng + only custom
                                for (Object o : optionsReq) {
                                    JSONObject optObj = (JSONObject) o;
                                    int optId = ((Number) optObj.get("id")).intValue();
                                    int param = ((Number) optObj.get("param")).intValue();
                                    JSONArray customOpt = new JSONArray();
                                    customOpt.add(optId);
                                    customOpt.add(param);
                                    optionsArr.add(customOpt.toJSONString());
                                }
                            } else {
                                // No custom -> đồ gốc game (get default options)
                                Item tempItem = ItemService.gI().createNewItem((short) itemId, 1);
                                ItemService.gI().initDefaultItemOptions(tempItem);
                                if (tempItem != null && tempItem.itemOptions != null) {
                                    for (Item.ItemOption io : tempItem.itemOptions) {
                                        JSONArray baseOpt = new JSONArray();
                                        baseOpt.add(io.optionTemplate.id);
                                        baseOpt.add(io.param);
                                        optionsArr.add(baseOpt.toJSONString());
                                    }
                                }
                            }
                            if (starCount > 0) {
                                JSONArray starOpt = new JSONArray();
                                starOpt.add(107);
                                starOpt.add(starCount);
                                optionsArr.add(starOpt.toJSONString());
                            }
                            dataItem.add(optionsArr.toJSONString());
                            dataItem.add(System.currentTimeMillis());

                            // Find first empty slot (where item tempId is -1)
                            boolean replaced = false;
                            for (int i = 0; i < bagArray.size(); i++) {
                                String slotStr = String.valueOf(bagArray.get(i));
                                JSONArray slotArr = (JSONArray) JSONValue.parse(slotStr);
                                if (slotArr != null && !slotArr.isEmpty()) {
                                    int tempId = Integer.parseInt(String.valueOf(slotArr.get(0)));
                                    if (tempId == -1) {
                                        bagArray.set(i, dataItem.toJSONString());
                                        replaced = true;
                                        break;
                                    }
                                }
                            }

                            if (!replaced) {
                                sendJsonResponse(exchange, 400,
                                        "{\"status\": \"error\", \"message\": \"Hành trang nhân vật " + playerName
                                                + " đã đầy! (30/30 ô)\"}");
                                return;
                            }

                            // Save back to DB
                            try (PreparedStatement psUpdate = conn
                                    .prepareStatement("UPDATE player SET items_bag = ? WHERE id = ?")) {
                                psUpdate.setString(1, bagArray.toJSONString());
                                psUpdate.setLong(2, pid);
                                psUpdate.executeUpdate();
                            }

                            JSONObject res = new JSONObject();
                            res.put("status", "success");
                            res.put("message",
                                    "Đã cấp thành công vật phẩm cho nhân vật " + playerName + " (Đang OFFLINE)");
                            sendJsonResponse(exchange, 200, res.toJSONString());
                        } else {
                            sendJsonResponse(exchange, 404,
                                    "{\"status\": \"error\", \"message\": \"Không tìm thấy nhân vật [" + playerName
                                            + "] trong dữ liệu\"}");
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- POST /api/grant-item-batch ---
    private static class GrantItemBatchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                JSONObject body = (JSONObject) JSONValue.parse(reader);

                if (body == null) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Dữ liệu không hợp lệ\"}");
                    return;
                }

                String playerName = (String) body.get("playerName");
                JSONArray itemsReq = (JSONArray) body.get("items");

                if (playerName == null || playerName.trim().isEmpty()) {
                    sendJsonResponse(exchange, 400,
                            "{\"status\": \"error\", \"message\": \"Tên nhân vật không được để trống\"}");
                    return;
                }

                if (itemsReq == null || itemsReq.isEmpty()) {
                    sendJsonResponse(exchange, 400,
                            "{\"status\": \"error\", \"message\": \"Danh sách vật phẩm cấp trống\"}");
                    return;
                }

                Player targetPlayer = null;
                for (Player p : Client.gI().getPlayers()) {
                    if (p != null && p.name != null && p.name.equalsIgnoreCase(playerName.trim())) {
                        targetPlayer = p;
                        break;
                    }
                }

                int successCount = 0;

                if (targetPlayer != null) {
                    // --- ONLINE PLAYER ---
                    for (Object itemObj : itemsReq) {
                        JSONObject itemJson = (JSONObject) itemObj;
                        long itemId = ((Number) itemJson.get("itemId")).longValue();
                        int quantity = itemJson.containsKey("quantity") ? ((Number) itemJson.get("quantity")).intValue()
                                : 1;
                        int starCount = itemJson.containsKey("stars") ? ((Number) itemJson.get("stars")).intValue() : 0;
                        JSONArray optionsReq = (JSONArray) itemJson.get("options");

                        Item item = ItemService.gI().createNewItem((short) itemId, quantity);
                        if (item != null) {
                            // Custom options -> đồ trắng; Không custom -> đồ gốc game (already from
                            // createNewItem)
                            if (optionsReq != null && !optionsReq.isEmpty()) {
                                item.itemOptions.clear(); // Đồ trắng
                                for (Object o : optionsReq) {
                                    JSONObject optObj = (JSONObject) o;
                                    int optId = ((Number) optObj.get("id")).intValue();
                                    int param = ((Number) optObj.get("param")).intValue();
                                    item.itemOptions.add(new Item.ItemOption(optId, param));
                                }
                            }
                            if (starCount > 0) {
                                item.itemOptions.add(new Item.ItemOption(107, starCount));
                            }
                            boolean added = InventoryService.gI().addItemBag(targetPlayer, item);
                            if (added)
                                successCount++;
                        }
                    }

                    if (successCount > 0) {
                        InventoryService.gI().sendItemBags(targetPlayer);
                        Service.gI().sendThongBao(targetPlayer, "Admin vừa cấp cho bạn " + successCount + " vật phẩm!");

                        JSONObject res = new JSONObject();
                        res.put("status", "success");
                        res.put("message", "Đã cấp thành công " + successCount + " vật phẩm cho nhân vật ["
                                + targetPlayer.name + "] (Đang ONLINE)");
                        sendJsonResponse(exchange, 200, res.toJSONString());
                    } else {
                        sendJsonResponse(exchange, 400,
                                "{\"status\": \"error\", \"message\": \"Hành trang nhân vật đã đầy!\"}");
                    }
                } else {
                    // --- OFFLINE PLAYER ---
                    try (Connection conn = LocalManager.getConnection();
                            PreparedStatement psSelect = conn
                                    .prepareStatement("SELECT id, items_bag FROM player WHERE name = ?")) {
                        psSelect.setString(1, playerName.trim());
                        ResultSet rs = psSelect.executeQuery();
                        if (rs.next()) {
                            long pid = rs.getLong("id");
                            String itemsBagStr = rs.getString("items_bag");

                            JSONArray bagArray;
                            if (itemsBagStr != null && !itemsBagStr.trim().isEmpty()) {
                                bagArray = (JSONArray) JSONValue.parse(itemsBagStr);
                            } else {
                                bagArray = new JSONArray();
                            }

                            for (Object itemObj : itemsReq) {
                                JSONObject itemJson = (JSONObject) itemObj;
                                long itemId = ((Number) itemJson.get("itemId")).longValue();
                                int quantity = itemJson.containsKey("quantity")
                                        ? ((Number) itemJson.get("quantity")).intValue()
                                        : 1;
                                int starCount = itemJson.containsKey("stars")
                                        ? ((Number) itemJson.get("stars")).intValue()
                                        : 0;
                                JSONArray optionsReq = (JSONArray) itemJson.get("options");

                                JSONArray dataItem = new JSONArray();
                                dataItem.add((int) itemId);
                                dataItem.add(quantity);

                                JSONArray optionsArr = new JSONArray();
                                if (starCount > 0) {
                                    JSONArray starOpt = new JSONArray();
                                    starOpt.add(107);
                                    starOpt.add(starCount);
                                    optionsArr.add(starOpt.toJSONString());
                                }
                                if (optionsReq != null) {
                                    for (Object o : optionsReq) {
                                        JSONObject optObj = (JSONObject) o;
                                        int optId = ((Number) optObj.get("id")).intValue();
                                        int param = ((Number) optObj.get("param")).intValue();
                                        JSONArray customOpt = new JSONArray();
                                        customOpt.add(optId);
                                        customOpt.add(param);
                                        optionsArr.add(customOpt.toJSONString());
                                    }
                                } else {
                                    Item tempItem = ItemService.gI().createNewItem((short) itemId, 1);
                                    ItemService.gI().initDefaultItemOptions(tempItem);
                                    if (tempItem != null && tempItem.itemOptions != null) {
                                        for (Item.ItemOption io : tempItem.itemOptions) {
                                            JSONArray baseOpt = new JSONArray();
                                            baseOpt.add(io.optionTemplate.id);
                                            baseOpt.add(io.param);
                                            optionsArr.add(baseOpt.toJSONString());
                                        }
                                    }
                                }
                                dataItem.add(optionsArr.toJSONString());
                                dataItem.add(System.currentTimeMillis());

                                boolean replaced = false;
                                for (int i = 0; i < bagArray.size(); i++) {
                                    String slotStr = String.valueOf(bagArray.get(i));
                                    JSONArray slotArr = (JSONArray) JSONValue.parse(slotStr);
                                    if (slotArr != null && !slotArr.isEmpty()) {
                                        int tempId = Integer.parseInt(String.valueOf(slotArr.get(0)));
                                        if (tempId == -1) {
                                            bagArray.set(i, dataItem.toJSONString());
                                            replaced = true;
                                            successCount++;
                                            break;
                                        }
                                    }
                                }
                                if (!replaced) {
                                    bagArray.add(dataItem.toJSONString());
                                    successCount++;
                                }
                            }

                            try (PreparedStatement psUpdate = conn
                                    .prepareStatement("UPDATE player SET items_bag = ? WHERE id = ?")) {
                                psUpdate.setString(1, bagArray.toJSONString());
                                psUpdate.setLong(2, pid);
                                psUpdate.executeUpdate();
                            }

                            JSONObject res = new JSONObject();
                            res.put("status", "success");
                            res.put("message", "Đã cấp thành công " + successCount + " vật phẩm cho nhân vật ["
                                    + playerName + "] (Đang OFFLINE)");
                            sendJsonResponse(exchange, 200, res.toJSONString());
                        } else {
                            sendJsonResponse(exchange, 404,
                                    "{\"status\": \"error\", \"message\": \"Không tìm thấy nhân vật [" + playerName
                                            + "]\"}");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- POST /api/grant-pet ---
    private static class GrantPetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                JSONObject body = (JSONObject) JSONValue.parse(reader);

                if (body == null) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Dữ liệu không hợp lệ\"}");
                    return;
                }

                String playerName = (String) body.get("playerName");
                int petType = body.containsKey("petType") ? ((Number) body.get("petType")).intValue() : 0;
                int petGender = body.containsKey("petGender") ? ((Number) body.get("petGender")).intValue() : 0;
                long power = body.containsKey("power") ? ((Number) body.get("power")).longValue() : 2000L;
                long tiemNang = body.containsKey("tiemNang") ? ((Number) body.get("tiemNang")).longValue() : power;

                long minPower = switch (petType) {
                    case 1 -> 1500000L; // Mabư: Min 1.5 Tr
                    case 2 -> 40000000000L; // Úp: Min 40 Tỷ
                    case 3 -> 40000000000L; // Kid Beer: Min 40 Tỷ
                    case 4 -> 40000000000L; // Jiren: Min 40 Tỷ
                    default -> 2000L; // Đệ Thường: Min 2,000
                };

                if (power < minPower) {
                    power = minPower;
                }
                if (tiemNang < power) {
                    tiemNang = power;
                }

                if (playerName == null || playerName.trim().isEmpty()) {
                    sendJsonResponse(exchange, 400,
                            "{\"status\": \"error\", \"message\": \"Tên nhân vật không được để trống\"}");
                    return;
                }

                Player targetPlayer = null;
                for (Player p : Client.gI().getPlayers()) {
                    if (p != null && p.name != null && p.name.equalsIgnoreCase(playerName.trim())) {
                        targetPlayer = p;
                        break;
                    }
                }

                if (targetPlayer != null) {
                    // ONLINE Player
                    if (targetPlayer.pet != null) {
                        switch (petType) {
                            case 1:
                                nro.models.services.PetService.gI().changeMabuPet(targetPlayer, petGender);
                                break;
                            case 2:
                                nro.models.services.PetService.gI().changeUubPet(targetPlayer, petGender);
                                break;
                            case 3:
                                nro.models.services.PetService.gI().changeKidBeerPet(targetPlayer, petGender);
                                break;
                            case 4:
                                nro.models.services.PetService.gI().changeJirenPet(targetPlayer, petGender);
                                break;
                            default:
                                nro.models.services.PetService.gI().changeNormalPet(targetPlayer, petGender);
                                break;
                        }
                    } else {
                        switch (petType) {
                            case 1:
                                nro.models.services.PetService.gI().createMabuPet(targetPlayer, petGender);
                                break;
                            case 2:
                                nro.models.services.PetService.gI().createUubPet(targetPlayer);
                                break;
                            case 3:
                                nro.models.services.PetService.gI().createKidBeerPet(targetPlayer);
                                break;
                            case 4:
                                nro.models.services.PetService.gI().createJirenPet(targetPlayer);
                                break;
                            default:
                                nro.models.services.PetService.gI().createNormalPet(targetPlayer, petGender);
                                break;
                        }
                    }

                    // Set custom power, tiemNang, and limitPower if provided
                    if (power >= 0) {
                        final Player plTarget = targetPlayer;
                        final long pVal = power;
                        final long tVal = tiemNang;
                        final int pType = petType;
                        new Thread(() -> {
                            try {
                                Thread.sleep(1200);
                                if (plTarget.pet != null) {
                                    byte calcLimit = 0;
                                    if (pVal >= 80000000000L)
                                        calcLimit = 9;
                                    else if (pVal >= 70000000000L)
                                        calcLimit = 8;
                                    else if (pVal >= 60000000000L)
                                        calcLimit = 7;
                                    else if (pVal >= 50000000000L)
                                        calcLimit = 6;
                                    else if (pVal >= 39000000000L)
                                        calcLimit = 5;
                                    else if (pVal >= 29000000000L)
                                        calcLimit = 4;
                                    else if (pVal >= 24000000000L)
                                        calcLimit = 3;
                                    else if (pVal >= 19000000000L)
                                        calcLimit = 2;
                                    else if (pVal >= 17000000000L)
                                        calcLimit = 1;

                                    plTarget.pet.nPoint.limitPower = calcLimit;
                                    plTarget.pet.nPoint.power = pVal;
                                    plTarget.pet.nPoint.tiemNang = tVal;

                                    // Automatically open skill 2, 3, 4, 5 based on power level!
                                    if (plTarget.pet.playerSkill != null && plTarget.pet.playerSkill.skills != null) {
                                        if (plTarget.pet.playerSkill.skills.size() > 1 && pVal >= 150000000L) {
                                            plTarget.pet.openSkill2();
                                        }
                                        if (plTarget.pet.playerSkill.skills.size() > 2 && pVal >= 1500000000L) {
                                            plTarget.pet.openSkill3();
                                        }
                                        if (plTarget.pet.playerSkill.skills.size() > 3 && pVal >= 20000000000L) {
                                            plTarget.pet.openSkill4();
                                        }
                                        if (plTarget.pet.playerSkill.skills.size() > 4 && pVal >= 40000000000L
                                                && pType >= 2) {
                                            plTarget.pet.openSkill5();
                                        }
                                    }

                                    plTarget.pet.joinMapMaster();
                                    nro.models.services.Service.gI().point(plTarget);
                                    nro.models.services.Service.gI().sendThongBao(plTarget,
                                            "Admin vừa cấp/đổi Đệ Tử mới thành công cho bạn!");
                                    nro.models.database.PlayerDAO.updatePlayer(plTarget);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }

                    JSONObject res = new JSONObject();
                    res.put("status", "success");
                    res.put("message",
                            "Đã cấp/đổi Đệ tử thành công cho nhân vật [" + targetPlayer.name + "] (Đang ONLINE)");
                    sendJsonResponse(exchange, 200, res.toJSONString());
                } else {
                    sendJsonResponse(exchange, 404,
                            "{\"status\": \"error\", \"message\": \"Nhân vật [" + playerName + "] đang OFFLINE\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- GET & POST /api/server-events ---
    private static class ServerEventsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    JSONObject res = new JSONObject();
                    res.put("expRate", Manager.RATE_EXP_SERVER);
                    res.put("lunarNewYear", EventManager.LUNNAR_NEW_YEAR);
                    res.put("womensDay", EventManager.INTERNATIONAL_WOMANS_DAY);
                    res.put("halloween", EventManager.HALLOWEEN);
                    res.put("christmas", EventManager.CHRISTMAS);
                    res.put("hungVuong", EventManager.HUNG_VUONG);
                    res.put("trungThu", EventManager.TRUNG_THU);
                    res.put("topUp", EventManager.TOP_UP);
                    sendJsonResponse(exchange, 200, res.toJSONString());
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    JSONObject body = (JSONObject) JSONValue.parse(reader);

                    if (body != null) {
                        if (body.containsKey("expRate")) {
                            byte rate = ((Number) body.get("expRate")).byteValue();
                            if (rate > 0) {
                                Manager.RATE_EXP_SERVER = rate;
                            }
                        }
                        if (body.containsKey("lunarNewYear"))
                            EventManager.LUNNAR_NEW_YEAR = (Boolean) body.get("lunarNewYear");
                        if (body.containsKey("womensDay"))
                            EventManager.INTERNATIONAL_WOMANS_DAY = (Boolean) body.get("womensDay");
                        if (body.containsKey("halloween"))
                            EventManager.HALLOWEEN = (Boolean) body.get("halloween");
                        if (body.containsKey("christmas"))
                            EventManager.CHRISTMAS = (Boolean) body.get("christmas");
                        if (body.containsKey("hungVuong"))
                            EventManager.HUNG_VUONG = (Boolean) body.get("hungVuong");
                        if (body.containsKey("trungThu"))
                            EventManager.TRUNG_THU = (Boolean) body.get("trungThu");
                        if (body.containsKey("topUp"))
                            EventManager.TOP_UP = (Boolean) body.get("topUp");

                        try {
                            EventManager.gI().init();
                        } catch (Exception ex) {
                        }

                        try {
                            nro.models.services.Service.gI().sendThongBaoAllPlayer("🎉 Máy chủ vừa kích hoạt X"
                                    + Manager.RATE_EXP_SERVER + " Tiềm Năng Sức Mạnh và cập nhật Sự Kiện Server!");
                        } catch (Exception ex) {
                        }

                        JSONObject res = new JSONObject();
                        res.put("status", "success");
                        res.put("message", "Đã cập nhật Hệ số X" + Manager.RATE_EXP_SERVER
                                + " TNSM và Sự Kiện Server thành công!");
                        sendJsonResponse(exchange, 200, res.toJSONString());
                    } else {
                        sendJsonResponse(exchange, 400,
                                "{\"status\": \"error\", \"message\": \"Dữ liệu không hợp lệ\"}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- GET & POST /api/drop-rules ---
    private static class DropRulesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    JSONObject res = new JSONObject();
                    JSONArray rulesArr = new JSONArray();
                    for (nro.models.item.DropManager.DropRule r : nro.models.item.DropManager.dropRules) {
                        rulesArr.add(r.toJSONObject());
                    }
                    res.put("rules", rulesArr);
                    sendJsonResponse(exchange, 200, res.toJSONString());
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    JSONObject body = (JSONObject) JSONValue.parse(reader);

                    if (body != null) {
                        String action = (String) body.get("action");
                        if ("add".equals(action)) {
                            int mapId = ((Number) body.get("mapId")).intValue();
                            int itemId = ((Number) body.get("itemId")).intValue();
                            int quantity = ((Number) body.get("quantity")).intValue();
                            int ratePercent = ((Number) body.get("ratePercent")).intValue();
                            nro.models.item.DropManager.gI().addRule(mapId, itemId, quantity, ratePercent);
                            sendJsonResponse(exchange, 200,
                                    "{\"status\": \"success\", \"message\": \"Đã thêm quy tắc rơi đồ theo map thành công!\"}");
                        } else if ("delete".equals(action)) {
                            int id = ((Number) body.get("id")).intValue();
                            nro.models.item.DropManager.gI().removeRule(id);
                            sendJsonResponse(exchange, 200,
                                    "{\"status\": \"success\", \"message\": \"Đã xóa quy tắc rơi đồ!\"}");
                        } else if ("toggle".equals(action)) {
                            int id = ((Number) body.get("id")).intValue();
                            for (nro.models.item.DropManager.DropRule r : nro.models.item.DropManager.dropRules) {
                                if (r.id == id) {
                                    r.active = !r.active;
                                    break;
                                }
                            }
                            sendJsonResponse(exchange, 200,
                                    "{\"status\": \"success\", \"message\": \"Đã thay đổi trạng thái quy tắc rơi đồ!\"}");
                        } else {
                            sendJsonResponse(exchange, 400,
                                    "{\"status\": \"error\", \"message\": \"Hành động không hợp lệ\"}");
                        }
                    } else {
                        sendJsonResponse(exchange, 400,
                                "{\"status\": \"error\", \"message\": \"Dữ liệu không hợp lệ\"}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- GET /api/map-templates ---
    private static class MapTemplatesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "[]");
                return;
            }
            try {
                JSONArray list = new JSONArray();
                if (Manager.MAP_TEMPLATES != null && Manager.MAP_TEMPLATES.length > 0) {
                    for (nro.models.player_system.Template.MapTemplate mt : Manager.MAP_TEMPLATES) {
                        if (mt != null) {
                            JSONObject obj = new JSONObject();
                            obj.put("id", mt.id);
                            obj.put("name", mt.name);
                            list.add(obj);
                        }
                    }
                } else {
                    try (Connection conn = LocalManager.getConnection();
                            PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM map_template");
                            ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject obj = new JSONObject();
                            obj.put("id", rs.getInt("id"));
                            obj.put("name", rs.getString("name"));
                            list.add(obj);
                        }
                    }
                }
                sendJsonResponse(exchange, 200, list.toJSONString());
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- GET & POST /api/npc-shops ---
    private static class NpcShopsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    JSONArray shopsArr = new JSONArray();
                    try (Connection conn = LocalManager.getConnection()) {
                        String sqlShop = "SELECT s.id as shop_id, s.npc_id, s.tag_name, s.type_shop, n.name as npc_name "
                                + "FROM shop s LEFT JOIN npc_template n ON s.npc_id = n.id ORDER BY s.npc_id ASC";
                        try (PreparedStatement ps = conn.prepareStatement(sqlShop);
                                ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                JSONObject shopObj = new JSONObject();
                                int shopId = rs.getInt("shop_id");
                                shopObj.put("shopId", shopId);
                                shopObj.put("npcId", rs.getInt("npc_id"));
                                shopObj.put("npcName", rs.getString("npc_name") != null ? rs.getString("npc_name")
                                        : ("NPC #" + rs.getInt("npc_id")));
                                shopObj.put("tagName", rs.getString("tag_name"));
                                shopObj.put("typeShop", rs.getInt("type_shop"));

                                JSONArray tabsArr = new JSONArray();
                                String sqlTab = "SELECT id, name FROM tab_shop WHERE shop_id = ? ORDER BY id ASC";
                                try (PreparedStatement psTab = conn.prepareStatement(sqlTab)) {
                                    psTab.setInt(1, shopId);
                                    try (ResultSet rsTab = psTab.executeQuery()) {
                                        while (rsTab.next()) {
                                            JSONObject tabObj = new JSONObject();
                                            int tabId = rsTab.getInt("id");
                                            tabObj.put("tabId", tabId);
                                            tabObj.put("name", rsTab.getString("name"));

                                            JSONArray itemsArr = new JSONArray();
                                            String sqlItem = "SELECT i.id, i.temp_id, i.cost, i.icon_spec, i.type_sell, i.is_new, t.name as item_name, t.icon_id "
                                                    + "FROM item_shop i LEFT JOIN item_template t ON i.temp_id = t.id "
                                                    + "WHERE i.is_sell = 1 AND i.tab_id = ? ORDER BY i.id DESC";
                                            try (PreparedStatement psItem = conn.prepareStatement(sqlItem)) {
                                                psItem.setInt(1, tabId);
                                                try (ResultSet rsItem = psItem.executeQuery()) {
                                                    while (rsItem.next()) {
                                                        JSONObject itemObj = new JSONObject();
                                                        int itemShopId = rsItem.getInt("id");
                                                        itemObj.put("id", itemShopId);
                                                        itemObj.put("tempId", rsItem.getInt("temp_id"));
                                                        itemObj.put("cost", rsItem.getInt("cost"));
                                                        itemObj.put("iconSpec", rsItem.getInt("icon_spec"));
                                                        itemObj.put("typeSell", rsItem.getInt("type_sell"));
                                                        itemObj.put("isNew", rsItem.getInt("is_new"));
                                                        itemObj.put("name",
                                                                rsItem.getString("item_name") != null
                                                                        ? rsItem.getString("item_name")
                                                                        : ("Item #" + rsItem.getInt("temp_id")));
                                                        itemObj.put("iconId", rsItem.getInt("icon_id"));

                                                        // Load options
                                                        JSONArray optsArr = new JSONArray();
                                                        String sqlOpt = "SELECT option_id, param FROM item_shop_option WHERE item_shop_id = ?";
                                                        try (PreparedStatement psOpt = conn.prepareStatement(sqlOpt)) {
                                                            psOpt.setInt(1, itemShopId);
                                                            try (ResultSet rsOpt = psOpt.executeQuery()) {
                                                                while (rsOpt.next()) {
                                                                    JSONObject optObj = new JSONObject();
                                                                    optObj.put("id", rsOpt.getInt("option_id"));
                                                                    optObj.put("param", rsOpt.getInt("param"));
                                                                    optsArr.add(optObj);
                                                                }
                                                            }
                                                        }
                                                        itemObj.put("options", optsArr);
                                                        itemsArr.add(itemObj);
                                                    }
                                                }
                                            }
                                            tabObj.put("items", itemsArr);
                                            tabsArr.add(tabObj);
                                        }
                                    }
                                }
                                shopObj.put("tabs", tabsArr);
                                shopsArr.add(shopObj);
                            }
                        }
                    }
                    sendJsonResponse(exchange, 200, shopsArr.toJSONString());
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    JSONObject body = (JSONObject) JSONValue.parse(reader);

                    if (body != null) {
                        String action = (String) body.get("action");
                        if ("add_item".equals(action)) {
                            int tabId = ((Number) body.get("tabId")).intValue();
                            int tempId = ((Number) body.get("tempId")).intValue();
                            int cost = ((Number) body.get("cost")).intValue();
                            int typeSell = ((Number) body.get("typeSell")).intValue();
                            int iconSpec = body.containsKey("iconSpec") ? ((Number) body.get("iconSpec")).intValue()
                                    : -1;
                            int isNew = body.containsKey("isNew") ? ((Number) body.get("isNew")).intValue() : 0;
                            JSONArray options = (JSONArray) body.get("options");

                            try (Connection conn = LocalManager.getConnection()) {
                                String sqlInsert = "INSERT INTO item_shop (tab_id, temp_id, is_new, cost, icon_spec, type_sell, is_sell, create_time) VALUES (?, ?, ?, ?, ?, ?, 1, NOW())";
                                try (PreparedStatement ps = conn.prepareStatement(sqlInsert,
                                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
                                    ps.setInt(1, tabId);
                                    ps.setInt(2, tempId);
                                    ps.setInt(3, isNew);
                                    ps.setInt(4, cost);
                                    ps.setInt(5, iconSpec);
                                    ps.setInt(6, typeSell);
                                    ps.executeUpdate();

                                    int insertedId = -1;
                                    try (ResultSet rs = ps.getGeneratedKeys()) {
                                        if (rs.next()) {
                                            insertedId = rs.getInt(1);
                                        }
                                    }

                                    if (insertedId > 0 && options != null) {
                                        String sqlOpt = "INSERT INTO item_shop_option (item_shop_id, option_id, param) VALUES (?, ?, ?)";
                                        try (PreparedStatement psOpt = conn.prepareStatement(sqlOpt)) {
                                            for (Object o : options) {
                                                JSONObject optObj = (JSONObject) o;
                                                psOpt.setInt(1, insertedId);
                                                psOpt.setInt(2, ((Number) optObj.get("id")).intValue());
                                                psOpt.setInt(3, ((Number) optObj.get("param")).intValue());
                                                psOpt.addBatch();
                                            }
                                            psOpt.executeBatch();
                                        }
                                    }

                                    // Reload in-memory shops if server is running
                                    try {
                                        nro.models.server.Manager.SHOPS = nro.models.database.ShopDAO.getShops(conn);
                                    } catch (Exception ex) {
                                    }
                                }
                            }
                            sendJsonResponse(exchange, 200,
                                    "{\"status\": \"success\", \"message\": \"Đã thêm vật phẩm vào Shop NPC thành công!\"}");
                        } else if ("delete_item".equals(action)) {
                            int itemShopId = ((Number) body.get("itemShopId")).intValue();
                            try (Connection conn = LocalManager.getConnection()) {
                                try (PreparedStatement psOpt = conn
                                        .prepareStatement("DELETE FROM item_shop_option WHERE item_shop_id = ?")) {
                                    psOpt.setInt(1, itemShopId);
                                    psOpt.executeUpdate();
                                }
                                try (PreparedStatement psItem = conn
                                        .prepareStatement("DELETE FROM item_shop WHERE id = ?")) {
                                    psItem.setInt(1, itemShopId);
                                    psItem.executeUpdate();
                                }
                                // Reload in-memory shops if server is running
                                try {
                                    nro.models.server.Manager.SHOPS = nro.models.database.ShopDAO.getShops(conn);
                                } catch (Exception ex) {
                                }
                            }
                            sendJsonResponse(exchange, 200,
                                    "{\"status\": \"success\", \"message\": \"Đã xóa vật phẩm khỏi Shop NPC!\"}");
                        } else {
                            sendJsonResponse(exchange, 400,
                                    "{\"status\": \"error\", \"message\": \"Hành động không hợp lệ\"}");
                        }
                    } else {
                        sendJsonResponse(exchange, 400,
                                "{\"status\": \"error\", \"message\": \"Dữ liệu không hợp lệ\"}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- POST /api/next-task ---
    private static class NextTaskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                InputStream is = exchange.getRequestBody();
                String bodyStr = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject body = (JSONObject) JSONValue.parse(bodyStr);

                String playerName = body != null && body.get("playerName") != null
                        ? body.get("playerName").toString().trim()
                        : "";
                int taskId = body != null && body.get("taskId") != null
                        ? Integer.parseInt(body.get("taskId").toString())
                        : -1;

                if (playerName.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Thiếu tên nhân vật\"}");
                    return;
                }

                Player player = null;
                for (Player p : Client.gI().getPlayers()) {
                    if (p != null) {
                        if ((p.name != null && p.name.equalsIgnoreCase(playerName)) ||
                                (p.getSession() != null && p.getSession().uu != null
                                        && p.getSession().uu.equalsIgnoreCase(playerName))) {
                            player = p;
                            break;
                        }
                    }
                }

                if (player != null && player.playerTask != null) {
                    int targetTaskId;
                    if (taskId >= 0) {
                        targetTaskId = taskId;
                    } else {
                        targetTaskId = player.playerTask.taskMain.id + 1;
                    }
                    player.playerTask.taskMain = TaskService.gI().getTaskMainById(player, targetTaskId);
                    player.playerTask.taskMain.index = 0;
                    if (player.playerTask.taskMain.subTasks != null && !player.playerTask.taskMain.subTasks.isEmpty()) {
                        player.playerTask.taskMain.subTasks.get(0).count = 0;
                    }

                    TaskService.gI().sendTaskMain(player);
                    TaskService.gI().sendUpdateCountSubTask(player);
                    Service.gI().sendThongBao(player,
                            "Admin đã chuyển nhiệm vụ cho bạn: " + player.playerTask.taskMain.name);

                    try (Connection conn = LocalManager.getConnection();
                            PreparedStatement psUpdate = conn
                                    .prepareStatement("UPDATE player SET data_task = ? WHERE id = ?")) {
                        JSONArray taskArr = new JSONArray();
                        taskArr.add(player.playerTask.taskMain.id);
                        taskArr.add(player.playerTask.taskMain.index);
                        if (player.playerTask.taskMain.subTasks != null
                                && !player.playerTask.taskMain.subTasks.isEmpty()) {
                            taskArr.add(
                                    player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count);
                        } else {
                            taskArr.add(0);
                        }
                        taskArr.add(System.currentTimeMillis());
                        psUpdate.setString(1, taskArr.toJSONString());
                        psUpdate.setLong(2, player.id);
                        psUpdate.executeUpdate();
                    } catch (Exception ex) {
                    }

                    sendJsonResponse(exchange, 200,
                            "{\"status\": \"success\", \"message\": \"Đã chuyển qua nhiệm vụ LIVE (Online) cho ["
                                    + player.name + "] sang nhiệm vụ #" + player.playerTask.taskMain.id + "!\"}");
                    return;
                }

                // If offline or player not in memory, update MySQL directly
                try (Connection conn = LocalManager.getConnection();
                        PreparedStatement psSelect = conn
                                .prepareStatement("SELECT id, gender, data_task FROM player WHERE name = ?")) {
                    psSelect.setString(1, playerName);
                    ResultSet rs = psSelect.executeQuery();
                    if (rs.next()) {
                        int pId = rs.getInt("id");
                        String dataTaskStr = rs.getString("data_task");
                        JSONArray taskArr;
                        try {
                            taskArr = (JSONArray) JSONValue.parse(dataTaskStr);
                        } catch (Exception e) {
                            taskArr = new JSONArray();
                            taskArr.add(0);
                            taskArr.add(0);
                            taskArr.add(0);
                        }
                        int currentTaskId = Integer.parseInt(taskArr.get(0).toString());
                        int nextTaskId = taskId >= 0 ? taskId : (currentTaskId + 1);

                        taskArr.set(0, nextTaskId);
                        taskArr.set(1, 0);
                        taskArr.set(2, 0);

                        try (PreparedStatement psUpdate = conn
                                .prepareStatement("UPDATE player SET data_task = ? WHERE id = ?")) {
                            psUpdate.setString(1, taskArr.toJSONString());
                            psUpdate.setInt(2, pId);
                            psUpdate.executeUpdate();
                        }

                        sendJsonResponse(exchange, 200,
                                "{\"status\": \"success\", \"message\": \"Đã chuyển nhiệm vụ (offline) cho ["
                                        + playerName + "] sang nhiệm vụ #" + nextTaskId + "!\"}");
                        return;
                    }
                }

                sendJsonResponse(exchange, 404,
                        "{\"status\": \"error\", \"message\": \"Không tìm thấy nhân vật [" + playerName + "]\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- POST /api/rename-player ---
    private static class RenamePlayerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                InputStream is = exchange.getRequestBody();
                String bodyStr = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject body = (JSONObject) JSONValue.parse(bodyStr);

                String oldName = body != null && body.get("oldName") != null ? body.get("oldName").toString().trim() : "";
                String newName = body != null && body.get("newName") != null ? body.get("newName").toString().trim() : "";

                if (oldName.isEmpty() || newName.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Vui lòng nhập đầy đủ tên nhân vật!\"}");
                    return;
                }

                if (oldName.equalsIgnoreCase(newName)) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Tên nhân vật mới trùng với tên cũ!\"}");
                    return;
                }

                // Check if newName already exists in DB
                try (Connection conn = LocalManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT id FROM player WHERE name = ?")) {
                    ps.setString(1, newName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Tên nhân vật '" + newName + "' đã được sử dụng!\"}");
                            return;
                        }
                    }
                }

                // Update DB
                int rows = 0;
                try (Connection conn = LocalManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("UPDATE player SET name = ? WHERE name = ?")) {
                    ps.setString(1, newName);
                    ps.setString(2, oldName);
                    rows = ps.executeUpdate();
                }

                if (rows == 0) {
                    sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Không tìm thấy nhân vật '" + oldName + "'!\"}");
                    return;
                }

                // Update in-memory player if online
                Player player = null;
                for (Player p : Client.gI().getPlayers()) {
                    if (p != null && p.name != null && p.name.equalsIgnoreCase(oldName)) {
                        player = p;
                        break;
                    }
                }

                if (player != null) {
                    player.name = newName;
                    Service.gI().sendThongBao(player, "Tên nhân vật của bạn đã được đổi thành: " + newName);
                    Service.gI().Send_Info_NV(player);
                }

                sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã đổi tên nhân vật '" + oldName + "' thành '" + newName + "' thành công!\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"Lỗi máy chủ: " + e.getMessage() + "\"}");
            }
        }
    }

    // --- POST /api/reload-giftcode ---
    private static class ReloadGiftCodeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                GiftCodeManager.gI().listGiftCode.clear();
                try (Connection conn = LocalManager.getConnection();
                        PreparedStatement ps = conn.prepareStatement("SELECT * FROM giftcode");
                        ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        GiftCode giftcode = new GiftCode();
                        giftcode.code = rs.getString("code");
                        giftcode.id = rs.getInt("id");
                        giftcode.countLeft = rs.getInt("count_left");
                        if (giftcode.countLeft == -1) {
                            giftcode.countLeft = 999999999;
                        }
                        giftcode.datecreate = rs.getTimestamp("datecreate");
                        giftcode.dateexpired = rs.getTimestamp("expired");
                        JSONArray jar = (JSONArray) JSONValue.parse(rs.getString("detail"));
                        if (jar != null) {
                            for (int i = 0; i < jar.size(); ++i) {
                                JSONObject jsonObj = (JSONObject) jar.get(i);
                                int id = Integer.parseInt(jsonObj.get("id").toString());
                                int quantity = Integer.parseInt(jsonObj.get("quantity").toString());

                                JSONArray option = (JSONArray) jsonObj.get("options");
                                ArrayList<Item.ItemOption> optionList = new ArrayList<>();
                                if (option != null) {
                                    for (int u = 0; u < option.size(); u++) {
                                        JSONObject jsonobject = (JSONObject) option.get(u);
                                        int optionId = Integer.parseInt(jsonobject.get("id").toString());
                                        int param = Integer.parseInt(jsonobject.get("param").toString());
                                        optionList.add(new Item.ItemOption(optionId, param));
                                    }
                                }
                                giftcode.option.put(id, optionList);
                                giftcode.detail.put(id, quantity);
                            }
                        }
                        GiftCodeManager.gI().listGiftCode.add(giftcode);
                    }
                }
                sendJsonResponse(exchange, 200,
                        "{\"status\": \"success\", \"message\": \"Đã tải lại danh sách GiftCode ("
                                + GiftCodeManager.gI().listGiftCode.size() + " mã)!\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- POST /api/use-giftcode ---
    private static class UseGiftCodeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                InputStream is = exchange.getRequestBody();
                String bodyStr = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject body = (JSONObject) JSONValue.parse(bodyStr);

                String playerName = body != null && body.get("playerName") != null
                        ? body.get("playerName").toString().trim()
                        : "";
                String code = body != null && body.get("code") != null ? body.get("code").toString().trim() : "";

                if (playerName.isEmpty() || code.isEmpty()) {
                    sendJsonResponse(exchange, 400,
                            "{\"status\": \"error\", \"message\": \"Thiếu tên nhân vật hoặc mã giftcode\"}");
                    return;
                }

                Player player = null;
                for (Player p : Client.gI().getPlayers()) {
                    if (p != null && p.name != null && p.name.equalsIgnoreCase(playerName)) {
                        player = p;
                        break;
                    }
                }

                if (player == null) {
                    sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Nhân vật [" + playerName
                            + "] phải ONLINE trong game để nhập giftcode!\"}");
                    return;
                }

                GiftCodeService.gI().giftCode(player, code);
                sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã xử lý nhập GiftCode cho ["
                        + player.name + "]!\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- POST /api/adjust-player-power ---
    private static class AdjustPlayerPowerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                InputStream is = exchange.getRequestBody();
                String bodyStr = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject body = (JSONObject) JSONValue.parse(bodyStr);

                if (body == null) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Dữ liệu không hợp lệ\"}");
                    return;
                }

                String playerName = (String) body.get("playerName");
                String action = body.containsKey("action") ? (String) body.get("action") : "add";
                long power = body.containsKey("power") ? ((Number) body.get("power")).longValue() : 0L;
                long tiemNang = body.containsKey("tiemNang") ? ((Number) body.get("tiemNang")).longValue() : 0L;

                if (playerName == null || playerName.trim().isEmpty()) {
                    sendJsonResponse(exchange, 400,
                            "{\"status\": \"error\", \"message\": \"Tên nhân vật không được để trống\"}");
                    return;
                }

                Player player = null;
                for (Player p : Client.gI().getPlayers()) {
                    if (p != null && p.name != null && p.name.equalsIgnoreCase(playerName.trim())) {
                        player = p;
                        break;
                    }
                }

                if (player != null) {
                    if ("set".equalsIgnoreCase(action)) {
                        player.nPoint.power = power;
                        player.nPoint.tiemNang = tiemNang;
                    } else if ("sub".equalsIgnoreCase(action)) {
                        player.nPoint.power = Math.max(0, player.nPoint.power - power);
                        player.nPoint.tiemNang = Math.max(0, player.nPoint.tiemNang - tiemNang);
                    } else { // "add"
                        player.nPoint.power += power;
                        player.nPoint.tiemNang += tiemNang;
                    }

                    nro.models.services.Service.gI().point(player);
                    nro.models.services.Service.gI().sendThongBao(player,
                            "Admin vừa điều chỉnh Sức mạnh & Tiềm năng cho bạn!");
                    nro.models.database.PlayerDAO.updatePlayer(player);

                    sendJsonResponse(exchange, 200,
                            "{\"status\": \"success\", \"message\": \"Đã điều chỉnh SM & Tiềm năng cho nhân vật ["
                                    + player.name + "] (ONLINE)!\"}");
                } else {
                    sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Nhân vật [" + playerName
                            + "] phải ONLINE trong game để điều chỉnh Sức Mạnh!\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- /api/adjust-player-event-point ---
    private static class AdjustPlayerEventPointHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJsonResponse(exchange, 405, "{\"status\": \"error\", \"message\": \"Method Not Allowed\"}");
                    return;
                }
                InputStream is = exchange.getRequestBody();
                JSONObject body = (JSONObject) JSONValue.parse(new InputStreamReader(is, StandardCharsets.UTF_8));
                if (body == null) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Dữ liệu không hợp lệ\"}");
                    return;
                }

                String playerName = (String) body.get("playerName");
                String action = body.containsKey("action") ? (String) body.get("action") : "set";
                int eventPoint = body.containsKey("eventPoint") ? ((Number) body.get("eventPoint")).intValue() : 0;

                if (playerName == null || playerName.trim().isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Tên nhân vật không được để trống\"}");
                    return;
                }

                Player player = null;
                for (Player p : Client.gI().getPlayers()) {
                    if (p != null && p.name != null && p.name.equalsIgnoreCase(playerName.trim())) {
                        player = p;
                        break;
                    }
                }

                int newPt = 0;
                boolean isOnline = false;
                if (player != null) {
                    isOnline = true;
                    if (player.event == null) {
                        player.event = new nro.models.player.PlayerEvent(player);
                    }
                    int currentPt = player.event.getEventPoint();
                    if ("set".equalsIgnoreCase(action)) {
                        newPt = eventPoint;
                    } else if ("sub".equalsIgnoreCase(action)) {
                        newPt = Math.max(0, currentPt - eventPoint);
                    } else { // "add"
                        newPt = currentPt + eventPoint;
                    }

                    player.event.setEventPoint(newPt);
                    nro.models.services.Service.gI().sendThongBao(player, "Admin vừa cập nhật Điểm Sự Kiện cho bạn thành: " + newPt + " điểm!");
                    nro.models.database.PlayerDAO.updatePlayer(player);
                }

                int updated = 0;
                try (Connection conn = LocalManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("UPDATE player SET event_point = CASE WHEN ? = 'set' THEN ? WHEN ? = 'sub' THEN GREATEST(0, event_point - ?) ELSE event_point + ? END WHERE name = ?")) {
                    ps.setString(1, action);
                    ps.setInt(2, eventPoint);
                    ps.setString(3, action);
                    ps.setInt(4, eventPoint);
                    ps.setInt(5, eventPoint);
                    ps.setString(6, playerName.trim());
                    updated = ps.executeUpdate();
                }

                if (isOnline || updated > 0) {
                    sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã cập nhật Điểm Sự Kiện nhân vật [" + playerName.trim() + "] thành công (" + (isOnline ? "ONLINE" : "OFFLINE") + ")!\"}");
                } else {
                    sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Không tìm thấy nhân vật [" + playerName.trim() + "] trong CSDL!\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- /api/manage-bots ---
    private static class ManageBotsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    List<nro.models.Bot.Bot> botList = new ArrayList<>(nro.models.Bot.BotManager.gI().bot);
                    JSONArray arr = new JSONArray();
                    for (nro.models.Bot.Bot b : botList) {
                        if (b != null) {
                            JSONObject obj = new JSONObject();
                            obj.put("id", b.id);
                            obj.put("name", b.name);
                            obj.put("gender", b.gender);
                            obj.put("power", b.nPoint != null ? b.nPoint.power : 0);
                            obj.put("mapName",
                                    b.zone != null && b.zone.map != null ? b.zone.map.mapName : "Đang di chuyển");
                            obj.put("zoneId", b.zone != null ? b.zone.zoneId : 0);
                            arr.add(obj);
                        }
                    }
                    JSONObject res = new JSONObject();
                    res.put("status", "success");
                    res.put("total", botList.size());
                    res.put("bots", arr);
                    sendJsonResponse(exchange, 200, res.toJSONString());
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStream is = exchange.getRequestBody();
                    JSONObject requestObj = (JSONObject) JSONValue
                            .parse(new InputStreamReader(is, StandardCharsets.UTF_8));
                    if (requestObj == null) {
                        sendJsonResponse(exchange, 400,
                                "{\"status\": \"error\", \"message\": \"Dữ liệu yêu cầu không hợp lệ\"}");
                        return;
                    }
                    String action = (String) requestObj.get("action");
                    if ("spawn".equalsIgnoreCase(action)) {
                        int count = 5;
                        if (requestObj.get("count") != null) {
                            count = Integer.parseInt(String.valueOf(requestObj.get("count")));
                        }
                        int type = 0; // 0 = Pem quái/Đi dạo, 1 = Giao dịch, 2 = Săn Boss
                        if (requestObj.get("type") != null) {
                            type = Integer.parseInt(String.valueOf(requestObj.get("type")));
                        }
                        nro.models.Bot.NewBot.gI().runBot(type, null, count);
                        sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã tạo thêm " + count
                                + " Bot giả thành công!\"}");
                    } else if ("clear".equalsIgnoreCase(action)) {
                        List<nro.models.Bot.Bot> botList = new ArrayList<>(nro.models.Bot.BotManager.gI().bot);
                        for (nro.models.Bot.Bot b : botList) {
                            if (b != null) {
                                try {
                                    nro.models.map.service.ChangeMapService.gI().exitMap(b);
                                } catch (Exception ex) {
                                }
                            }
                        }
                        nro.models.Bot.BotManager.gI().bot.clear();
                        sendJsonResponse(exchange, 200,
                                "{\"status\": \"success\", \"message\": \"Đã dọn dẹp và xóa toàn bộ Bot giả khỏi server!\"}");
                    } else if ("kick_one".equalsIgnoreCase(action)) {
                        long botId = requestObj.get("botId") != null
                                ? Long.parseLong(String.valueOf(requestObj.get("botId")))
                                : -1;
                        List<nro.models.Bot.Bot> botList = new ArrayList<>(nro.models.Bot.BotManager.gI().bot);
                        boolean found = false;
                        for (nro.models.Bot.Bot b : botList) {
                            if (b != null && b.id == botId) {
                                try {
                                    nro.models.map.service.ChangeMapService.gI().exitMap(b);
                                } catch (Exception ex) {
                                }
                                nro.models.Bot.BotManager.gI().bot.remove(b);
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            sendJsonResponse(exchange, 200,
                                    "{\"status\": \"success\", \"message\": \"Đã kích thành công Bot ID: " + botId
                                            + "!\"}");
                        } else {
                            sendJsonResponse(exchange, 404,
                                    "{\"status\": \"error\", \"message\": \"Không tìm thấy Bot với ID chỉ định!\"}");
                        }
                    } else {
                        sendJsonResponse(exchange, 400,
                                "{\"status\": \"error\", \"message\": \"Hành động không hợp lệ\"}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- GET /api/bosses ---
    private static class BossesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                JSONArray arr = new JSONArray();
                List<nro.models.boss.Boss> allBosses = new ArrayList<>();

                try {
                    if (nro.models.boss.Boss_Manager.BossManager.gI() != null && nro.models.boss.Boss_Manager.BossManager.gI().getBosses() != null) {
                        allBosses.addAll(nro.models.boss.Boss_Manager.BossManager.gI().getBosses());
                    }
                } catch (Exception e) {}

                try {
                    if (nro.models.boss.Boss_Manager.FinalBossManager.gI() != null && nro.models.boss.Boss_Manager.FinalBossManager.gI().getBosses() != null) {
                        for (nro.models.boss.Boss b : nro.models.boss.Boss_Manager.FinalBossManager.gI().getBosses()) {
                            if (!allBosses.contains(b)) allBosses.add(b);
                        }
                    }
                } catch (Exception e) {}

                try {
                    if (nro.models.boss.Boss_Manager.OtherBossManager.gI() != null && nro.models.boss.Boss_Manager.OtherBossManager.gI().getBosses() != null) {
                        for (nro.models.boss.Boss b : nro.models.boss.Boss_Manager.OtherBossManager.gI().getBosses()) {
                            if (!allBosses.contains(b)) allBosses.add(b);
                        }
                    }
                } catch (Exception e) {}

                for (nro.models.boss.Boss boss : allBosses) {
                    if (boss == null) continue;
                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("id", boss.id);

                        String name = "Boss";
                        if (boss.name != null && !boss.name.isEmpty()) {
                            name = boss.name;
                        } else if (boss.data != null && boss.data.length > 0 && boss.data[0] != null && boss.data[0].getName() != null) {
                            name = boss.data[0].getName();
                        }
                        obj.put("name", name);

                        short head = 0;
                        try {
                            head = boss.getHead();
                        } catch (Exception ex) {
                            if (boss.data != null && boss.data.length > 0 && boss.data[0] != null && boss.data[0].getOutfit() != null) {
                                head = boss.data[0].getOutfit()[0];
                            }
                        }
                        obj.put("head", head);
                        short iconId = Manager.getIconHead(head);
                        obj.put("iconId", iconId);
                        obj.put("avatarUrl", "/icons/" + iconId + ".png");

                        if (boss.nPoint != null) {
                            obj.put("hp", boss.nPoint.hp);
                            obj.put("maxHp", boss.nPoint.hpMax);
                            obj.put("dame", boss.nPoint.dame);
                            obj.put("def", boss.nPoint.defg);
                        } else {
                            obj.put("hp", 0);
                            obj.put("maxHp", 0);
                            obj.put("dame", 0);
                            obj.put("def", 0);
                        }

                        obj.put("status", boss.bossStatus != null ? boss.bossStatus.name() : "REST");

                        if (boss.zone != null && boss.zone.map != null) {
                            obj.put("mapId", boss.zone.map.mapId);
                            obj.put("mapName", boss.zone.map.mapName);
                            obj.put("zoneId", boss.zone.zoneId);
                            obj.put("x", boss.location != null ? boss.location.x : 0);
                            obj.put("y", boss.location != null ? boss.location.y : 0);
                        } else {
                            obj.put("mapId", -1);
                            obj.put("mapName", "Chưa xuất hiện");
                            obj.put("zoneId", -1);
                            obj.put("x", 0);
                            obj.put("y", 0);
                        }

                        arr.add(obj);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                sendJsonResponse(exchange, 200, arr.toJSONString());
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- POST /api/bosses/spawn ---
    private static class SpawnBossHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                JSONObject body = (JSONObject) JSONValue.parse(reader);

                if (body == null || !body.containsKey("bossId")) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Vui lòng nhập Boss ID hợp lệ!\"}");
                    return;
                }

                int bossId = ((Number) body.get("bossId")).intValue();
                int mapId = body.containsKey("mapId") ? ((Number) body.get("mapId")).intValue() : -1;
                long rawHp = body.containsKey("hp") ? ((Number) body.get("hp")).longValue() : 0;
                long rawDame = body.containsKey("dame") ? ((Number) body.get("dame")).longValue() : 0;
                long rawDef = body.containsKey("def") ? ((Number) body.get("def")).longValue() : 0;

                int customHp = (rawHp > 2_000_000_000L) ? 2_000_000_000 : (int) rawHp;
                int customDame = (rawDame > 2_000_000_000L) ? 2_000_000_000 : (int) rawDame;
                int customDef = (rawDef > 2_000_000_000L) ? 2_000_000_000 : (int) rawDef;

                nro.models.boss.Boss boss = nro.models.boss.Boss_Manager.BossManager.gI().createBoss(bossId);

                if (boss != null) {
                    if (boss.currentLevel < 0) {
                        boss.currentLevel = 0;
                    }
                    boss.initBase();
                    boss.changeToTypeNonPK();

                    if (customHp > 0) {
                        boss.nPoint.hpg = customHp;
                    }
                    if (customDame > 0) {
                        boss.nPoint.dameg = customDame;
                    }
                    if (customDef > 0) {
                        boss.nPoint.defg = customDef;
                    }
                    boss.nPoint.calPoint();
                    boss.nPoint.hp = boss.nPoint.hpMax;
                    if (customDame > 0) {
                        boss.nPoint.dame = customDame;
                    }
                    if (customDef > 0) {
                        boss.nPoint.def = customDef;
                    }

                    nro.models.map.Zone targetZone = null;
                    if (mapId >= 0) {
                        try {
                            nro.models.map.Map targetMap = nro.models.map.service.MapService.gI().getMapById(mapId);
                            if (targetMap != null && targetMap.zones != null && !targetMap.zones.isEmpty()) {
                                targetZone = targetMap.zones.get(nro.models.utils.Util.nextInt(0, targetMap.zones.size() - 1));
                            }
                        } catch (Exception ex) {}
                    } else if (mapId == -2) {
                        int[] normalMaps = new int[]{0, 1, 2, 5, 7, 8, 13, 14, 20, 27, 28, 29, 30, 42, 43, 44};
                        int randomMapId = normalMaps[nro.models.utils.Util.nextInt(0, normalMaps.length - 1)];
                        try {
                            nro.models.map.Map targetMap = nro.models.map.service.MapService.gI().getMapById(randomMapId);
                            if (targetMap != null && targetMap.zones != null && !targetMap.zones.isEmpty()) {
                                targetZone = targetMap.zones.get(nro.models.utils.Util.nextInt(0, targetMap.zones.size() - 1));
                            }
                        } catch (Exception ex) {}
                    }

                    if (targetZone == null) {
                        try {
                            targetZone = boss.getMapJoin();
                        } catch (Exception ex) {}
                    }

                    if (targetZone != null) {
                        boss.zone = targetZone;
                        int x = targetZone.map.mapWidth > 100 ? nro.models.utils.Util.nextInt(100, targetZone.map.mapWidth - 100) : nro.models.utils.Util.nextInt(100);
                        int y = targetZone.map.yPhysicInTop(x, 100);
                        boss.location.x = x;
                        boss.location.y = y;
                        try {
                            nro.models.map.service.ChangeMapService.gI().changeMap(boss, targetZone, x, y);
                        } catch (Exception ex) {}
                    }

                    boss.changeStatus(nro.models.consts.BossStatus.ACTIVE);

                    String mapLocationInfo = (boss.zone != null && boss.zone.map != null) ? boss.zone.map.mapName + " (Khu " + boss.zone.zoneId + ")" : "Bản đồ ngẫu nhiên";
                    JSONObject res = new JSONObject();
                    res.put("status", "success");
                    res.put("message", "Đã triệu hồi Boss [" + (boss.name != null ? boss.name : "ID: " + bossId) + "] xuất hiện ngay tại " + mapLocationInfo + "! (HP: " + boss.nPoint.hp + ")");
                    sendJsonResponse(exchange, 200, res.toJSONString());
                } else {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Không thể khởi tạo Boss ID: " + bossId + "! Vui lòng kiểm tra lại ID.\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- POST /api/bosses/action ---
    private static class BossActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                JSONObject body = (JSONObject) JSONValue.parse(reader);

                if (body == null) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Dữ liệu yêu cầu không hợp lệ\"}");
                    return;
                }

                String action = (String) body.get("action");
                int bossId = body.containsKey("bossId") ? ((Number) body.get("bossId")).intValue() : 0;

                List<nro.models.boss.Boss> allBosses = new ArrayList<>();
                if (nro.models.boss.Boss_Manager.BossManager.gI() != null && nro.models.boss.Boss_Manager.BossManager.gI().getBosses() != null) {
                    allBosses.addAll(nro.models.boss.Boss_Manager.BossManager.gI().getBosses());
                }

                nro.models.boss.Boss targetBoss = null;
                for (nro.models.boss.Boss b : allBosses) {
                    if (b != null && b.id == bossId) {
                        targetBoss = b;
                        break;
                    }
                }

                if ("kill".equalsIgnoreCase(action) || "despawn".equalsIgnoreCase(action)) {
                    if (targetBoss != null) {
                        targetBoss.changeStatus(nro.models.consts.BossStatus.DIE);
                        sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã tiêu diệt Boss thành công!\"}");
                    } else {
                        sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Không tìm thấy Boss đang xuất hiện!\"}");
                    }
                } else if ("delete".equalsIgnoreCase(action) || "remove".equalsIgnoreCase(action)) {
                    if (targetBoss != null) {
                        try {
                            targetBoss.changeStatus(nro.models.consts.BossStatus.LEAVE_MAP);
                            if (nro.models.boss.Boss_Manager.BossManager.gI() != null && nro.models.boss.Boss_Manager.BossManager.gI().getBosses() != null) {
                                nro.models.boss.Boss_Manager.BossManager.gI().getBosses().remove(targetBoss);
                            }
                        } catch (Exception ex) {}
                        sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã xóa Boss hoàn toàn khỏi danh sách!\"}");
                    } else {
                        sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Không tìm thấy Boss cần xóa!\"}");
                    }
                } else if ("respawn".equalsIgnoreCase(action)) {
                    if (targetBoss != null) {
                        int savedHpg = targetBoss.nPoint.hpg;
                        int savedDameg = targetBoss.nPoint.dameg;
                        int savedDefg = targetBoss.nPoint.defg;

                        if (targetBoss.currentLevel < 0) {
                            targetBoss.currentLevel = 0;
                        }
                        targetBoss.initBase();
                        targetBoss.changeToTypeNonPK();

                        if (savedHpg > 0) {
                            targetBoss.nPoint.hpg = savedHpg;
                        }
                        if (savedDameg > 0) {
                            targetBoss.nPoint.dameg = savedDameg;
                        }
                        if (savedDefg > 0) {
                            targetBoss.nPoint.defg = savedDefg;
                        }
                        targetBoss.nPoint.calPoint();
                        targetBoss.nPoint.hp = targetBoss.nPoint.hpMax;
                        if (savedDameg > 0) {
                            targetBoss.nPoint.dame = savedDameg;
                        }
                        if (savedDefg > 0) {
                            targetBoss.nPoint.def = savedDefg;
                        }

                        if (targetBoss.zone == null) {
                            try {
                                targetBoss.zone = targetBoss.getMapJoin();
                            } catch (Exception ex) {}
                        }
                        if (targetBoss.zone != null) {
                            try {
                                int x = targetBoss.zone.map.mapWidth > 100 ? nro.models.utils.Util.nextInt(100, targetBoss.zone.map.mapWidth - 100) : nro.models.utils.Util.nextInt(100);
                                int y = targetBoss.zone.map.yPhysicInTop(x, 100);
                                nro.models.map.service.ChangeMapService.gI().changeMap(targetBoss, targetBoss.zone, x, y);
                            } catch (Exception ex) {}
                        }
                        targetBoss.changeStatus(nro.models.consts.BossStatus.ACTIVE);
                        sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã hồi sinh và đưa Boss " + targetBoss.name + " trở lại game! (HP: " + targetBoss.nPoint.hp + "/" + targetBoss.nPoint.hpMax + ")\"}");
                    } else {
                        nro.models.boss.Boss newBoss = nro.models.boss.Boss_Manager.BossManager.gI().createBoss(bossId);
                        if (newBoss != null) {
                            if (newBoss.currentLevel < 0) {
                                newBoss.currentLevel = 0;
                            }
                            newBoss.initBase();
                            newBoss.changeToTypeNonPK();
                            newBoss.changeStatus(nro.models.consts.BossStatus.ACTIVE);
                            sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã tạo mới và triệu hồi Boss thành công!\"}");
                        } else {
                            sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Không thể tạo mới Boss!\"}");
                        }
                    }
                } else if ("set_stats".equalsIgnoreCase(action)) {
                    if (targetBoss != null) {
                        long rawHp = body.containsKey("hp") ? ((Number) body.get("hp")).longValue() : -1;
                        long rawMaxHp = body.containsKey("maxHp") ? ((Number) body.get("maxHp")).longValue() : -1;
                        long rawDame = body.containsKey("dame") ? ((Number) body.get("dame")).longValue() : -1;
                        long rawDef = body.containsKey("def") ? ((Number) body.get("def")).longValue() : -1;

                        int newHp = (rawHp > 2_000_000_000L) ? 2_000_000_000 : (int) rawHp;
                        int newMaxHp = (rawMaxHp > 2_000_000_000L) ? 2_000_000_000 : (int) rawMaxHp;
                        int newDame = (rawDame > 2_000_000_000L) ? 2_000_000_000 : (int) rawDame;
                        int newDef = (rawDef > 2_000_000_000L) ? 2_000_000_000 : (int) rawDef;

                        if (newMaxHp > 0) {
                            targetBoss.nPoint.hpg = newMaxHp;
                            targetBoss.nPoint.hpMax = newMaxHp;
                        }
                        if (newHp >= 0) {
                            targetBoss.nPoint.hp = Math.min(newHp, targetBoss.nPoint.hpMax);
                        }
                        if (newDame > 0) {
                            targetBoss.nPoint.dameg = newDame;
                            targetBoss.nPoint.dame = newDame;
                        }
                        if (newDef >= 0) {
                            targetBoss.nPoint.defg = newDef;
                            targetBoss.nPoint.def = newDef;
                        }
                        targetBoss.nPoint.calPoint();
                        if (newDame > 0) {
                            targetBoss.nPoint.dame = newDame;
                        }
                        if (newDef >= 0) {
                            targetBoss.nPoint.def = newDef;
                        }

                        sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã cập nhật chỉ số Boss [" + targetBoss.name + "] thành công! (HP: " + targetBoss.nPoint.hp + "/" + targetBoss.nPoint.hpMax + ", Dame: " + targetBoss.nPoint.dame + ", Giáp: " + targetBoss.nPoint.def + ")\"}");
                    } else {
                        sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Không tìm thấy Boss!\"}");
                    }
                } else {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Hành động không được hỗ trợ\"}");
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- GET / POST /api/player-inventory ---
    private static class PlayerInventoryHandler implements HttpHandler {
        private String getBodySlotName(int slot) {
            return switch (slot) {
                case 0 -> "Áo";
                case 1 -> "Quần";
                case 2 -> "Găng";
                case 3 -> "Giày";
                case 4 -> "Rada/Thang";
                case 5 -> "Cải Trang";
                case 6 -> "Thẻ Đệ/Nón";
                case 7 -> "Phụ Kiện/Cánh";
                case 8 -> "Linh Thú/Pet";
                case 9 -> "Danh Hiệu";
                case 10 -> "Chân Mây/Bội";
                case 11 -> "Vũ Khí Khác";
                default -> "Trang Bị #" + slot;
            };
        }

        private JSONObject parseItemToJson(Item item, int slotIndex) {
            if (item == null || item.template == null) return null;
            JSONObject obj = new JSONObject();
            obj.put("slot", slotIndex);
            obj.put("slotName", getBodySlotName(slotIndex));
            obj.put("id", item.template.id);
            obj.put("name", item.template.name);
            obj.put("iconID", item.template.iconID != 0 ? item.template.iconID : item.template.id);
            obj.put("quantity", item.quantity);
            obj.put("type", item.template.type);

            JSONArray optArr = new JSONArray();
            if (item.itemOptions != null) {
                for (Item.ItemOption io : item.itemOptions) {
                    if (io != null && io.optionTemplate != null) {
                        JSONObject optObj = new JSONObject();
                        optObj.put("id", io.optionTemplate.id);
                        optObj.put("name", io.optionTemplate.name != null ? io.optionTemplate.name : ("Option #" + io.optionTemplate.id));
                        optObj.put("param", io.param);
                        optArr.add(optObj);
                    }
                }
            }
            obj.put("options", optArr);
            return obj;
        }

        private JSONObject parseRawDbItemToJson(String itemJsonStr, int slotIndex) {
            if (itemJsonStr == null || itemJsonStr.trim().isEmpty() || "null".equalsIgnoreCase(itemJsonStr)) return null;
            try {
                Object parsed = JSONValue.parse(itemJsonStr);
                if (!(parsed instanceof JSONArray)) return null;
                JSONArray dataItem = (JSONArray) parsed;
                if (dataItem.isEmpty()) return null;

                int tempId = Integer.parseInt(String.valueOf(dataItem.get(0)));
                if (tempId == -1) return null;

                int quantity = dataItem.size() > 1 ? Integer.parseInt(String.valueOf(dataItem.get(1))) : 1;

                ItemTemplate temp = ItemService.gI().getTemplate((short) tempId);
                if (temp == null) return null;

                JSONObject obj = new JSONObject();
                obj.put("slot", slotIndex);
                obj.put("slotName", getBodySlotName(slotIndex));
                obj.put("id", temp.id);
                obj.put("name", temp.name);
                obj.put("iconID", temp.iconID != 0 ? temp.iconID : temp.id);
                obj.put("quantity", quantity);
                obj.put("type", temp.type);

                JSONArray optArr = new JSONArray();
                if (dataItem.size() > 2) {
                    Object optsObj = dataItem.get(2);
                    JSONArray rawOpts = null;
                    if (optsObj instanceof JSONArray) {
                        rawOpts = (JSONArray) optsObj;
                    } else if (optsObj instanceof String) {
                        rawOpts = (JSONArray) JSONValue.parse((String) optsObj);
                    }
                    if (rawOpts != null) {
                        for (Object o : rawOpts) {
                            JSONArray singleOpt = null;
                            if (o instanceof JSONArray) singleOpt = (JSONArray) o;
                            else if (o instanceof String) singleOpt = (JSONArray) JSONValue.parse((String) o);

                            if (singleOpt != null && singleOpt.size() >= 2) {
                                int optId = Integer.parseInt(String.valueOf(singleOpt.get(0)));
                                int param = Integer.parseInt(String.valueOf(singleOpt.get(1)));
                                ItemOptionTemplate optTemp = Manager.ITEM_OPTION_TEMPLATES.stream().filter(t -> t != null && t.id == optId).findFirst().orElse(null);
                                JSONObject optObj = new JSONObject();
                                optObj.put("id", optId);
                                optObj.put("name", optTemp != null && optTemp.name != null ? optTemp.name : ("Option #" + optId));
                                optObj.put("param", param);
                                optArr.add(optObj);
                            }
                        }
                    }
                }
                obj.put("options", optArr);
                return obj;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                String query = exchange.getRequestURI().getQuery();
                String playerName = "";
                if (query != null && query.contains("playerName=")) {
                    playerName = query.split("playerName=")[1].split("&")[0];
                    playerName = java.net.URLDecoder.decode(playerName, StandardCharsets.UTF_8);
                }

                if (playerName.isEmpty() && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    JSONObject body = (JSONObject) JSONValue.parse(reader);
                    if (body != null && body.get("playerName") != null) {
                        playerName = body.get("playerName").toString().trim();
                    }
                }

                if (playerName.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Vui lòng cung cấp tên nhân vật!\"}");
                    return;
                }

                Player player = null;
                for (Player p : Client.gI().getPlayers()) {
                    if (p != null && p.name != null && p.name.equalsIgnoreCase(playerName)) {
                        player = p;
                        break;
                    }
                }

                JSONObject res = new JSONObject();
                res.put("status", "success");
                res.put("playerName", playerName);

                if (player != null && player.inventory != null) {
                    // LIVE ONLINE PLAYER
                    res.put("isOnline", true);
                    res.put("playerId", player.id);
                    res.put("gender", player.gender);
                    res.put("gold", player.inventory.gold);
                    res.put("gem", player.inventory.gem);
                    res.put("ruby", player.inventory.ruby);

                    JSONArray bodyArr = new JSONArray();
                    if (player.inventory.itemsBody != null) {
                        for (int i = 0; i < player.inventory.itemsBody.size(); i++) {
                            Item item = player.inventory.itemsBody.get(i);
                            JSONObject itemObj = parseItemToJson(item, i);
                            if (itemObj != null) bodyArr.add(itemObj);
                        }
                    }
                    res.put("itemsBody", bodyArr);

                    JSONArray bagArr = new JSONArray();
                    if (player.inventory.itemsBag != null) {
                        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                            Item item = player.inventory.itemsBag.get(i);
                            JSONObject itemObj = parseItemToJson(item, i);
                            if (itemObj != null) bagArr.add(itemObj);
                        }
                    }
                    res.put("itemsBag", bagArr);

                    JSONArray boxArr = new JSONArray();
                    if (player.inventory.itemsBox != null) {
                        for (int i = 0; i < player.inventory.itemsBox.size(); i++) {
                            Item item = player.inventory.itemsBox.get(i);
                            JSONObject itemObj = parseItemToJson(item, i);
                            if (itemObj != null) boxArr.add(itemObj);
                        }
                    }
                    res.put("itemsBox", boxArr);

                    if (player.pet != null && player.pet.inventory != null) {
                        res.put("hasPet", true);
                        res.put("petName", player.pet.name != null ? player.pet.name : "Đệ tử");
                        res.put("petGender", player.pet.gender);
                        res.put("petPower", player.pet.nPoint != null ? player.pet.nPoint.power : 0);

                        JSONArray petBodyArr = new JSONArray();
                        if (player.pet.inventory.itemsBody != null) {
                            for (int i = 0; i < player.pet.inventory.itemsBody.size(); i++) {
                                Item item = player.pet.inventory.itemsBody.get(i);
                                JSONObject itemObj = parseItemToJson(item, i);
                                if (itemObj != null) petBodyArr.add(itemObj);
                            }
                        }
                        res.put("itemsPetBody", petBodyArr);
                    } else {
                        res.put("hasPet", false);
                        res.put("itemsPetBody", new JSONArray());
                    }

                    sendJsonResponse(exchange, 200, res.toJSONString());
                    return;
                }

                // OFFLINE PLAYER FROM DATABASE
                try (Connection conn = LocalManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT id, name, gender, items_body, items_bag, items_box, data_inventory, pet FROM player WHERE name = ?")) {
                    ps.setString(1, playerName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            long pid = rs.getLong("id");
                            int gender = rs.getInt("gender");
                            String itemsBodyStr = rs.getString("items_body");
                            String itemsBagStr = rs.getString("items_bag");
                            String itemsBoxStr = rs.getString("items_box");
                            String inventoryStr = rs.getString("data_inventory");
                            String petStr = rs.getString("pet");

                            res.put("isOnline", false);
                            res.put("playerId", pid);
                            res.put("gender", gender);

                            long gold = 0, gem = 0, ruby = 0;
                            if (inventoryStr != null && !inventoryStr.isEmpty()) {
                                try {
                                    JSONArray invArr = (JSONArray) JSONValue.parse(inventoryStr);
                                    if (invArr != null) {
                                        if (invArr.size() > 0) gold = Long.parseLong(String.valueOf(invArr.get(0)));
                                        if (invArr.size() > 1) gem = Long.parseLong(String.valueOf(invArr.get(1)));
                                        if (invArr.size() > 2) ruby = Long.parseLong(String.valueOf(invArr.get(2)));
                                    }
                                } catch (Exception ex) {}
                            }
                            res.put("gold", gold);
                            res.put("gem", gem);
                            res.put("ruby", ruby);

                            JSONArray bodyArr = new JSONArray();
                            if (itemsBodyStr != null && !itemsBodyStr.isEmpty()) {
                                JSONArray rawArr = (JSONArray) JSONValue.parse(itemsBodyStr);
                                if (rawArr != null) {
                                    for (int i = 0; i < rawArr.size(); i++) {
                                        JSONObject itemObj = parseRawDbItemToJson(String.valueOf(rawArr.get(i)), i);
                                        if (itemObj != null) bodyArr.add(itemObj);
                                    }
                                }
                            }
                            res.put("itemsBody", bodyArr);

                            JSONArray bagArr = new JSONArray();
                            if (itemsBagStr != null && !itemsBagStr.isEmpty()) {
                                JSONArray rawArr = (JSONArray) JSONValue.parse(itemsBagStr);
                                if (rawArr != null) {
                                    for (int i = 0; i < rawArr.size(); i++) {
                                        JSONObject itemObj = parseRawDbItemToJson(String.valueOf(rawArr.get(i)), i);
                                        if (itemObj != null) bagArr.add(itemObj);
                                    }
                                }
                            }
                            res.put("itemsBag", bagArr);

                            JSONArray boxArr = new JSONArray();
                            if (itemsBoxStr != null && !itemsBoxStr.isEmpty()) {
                                JSONArray rawArr = (JSONArray) JSONValue.parse(itemsBoxStr);
                                if (rawArr != null) {
                                    for (int i = 0; i < rawArr.size(); i++) {
                                        JSONObject itemObj = parseRawDbItemToJson(String.valueOf(rawArr.get(i)), i);
                                        if (itemObj != null) boxArr.add(itemObj);
                                    }
                                }
                            }
                            res.put("itemsBox", boxArr);

                            if (petStr != null && !petStr.isEmpty() && !"null".equalsIgnoreCase(petStr)) {
                                try {
                                    JSONArray petDataArr = (JSONArray) JSONValue.parse(petStr);
                                    if (petDataArr != null && petDataArr.size() >= 3) {
                                        String petInfoStr = String.valueOf(petDataArr.get(0));
                                        String petPointStr = String.valueOf(petDataArr.get(1));
                                        String petBodyStr = String.valueOf(petDataArr.get(2));

                                        res.put("hasPet", true);

                                        String petName = "Đệ Tử";
                                        int petGender = 0;
                                        if (petInfoStr != null && !petInfoStr.isEmpty()) {
                                            JSONArray infoArr = (JSONArray) JSONValue.parse(petInfoStr);
                                            if (infoArr != null && infoArr.size() > 0) petName = String.valueOf(infoArr.get(0));
                                            if (infoArr != null && infoArr.size() > 1) petGender = Integer.parseInt(String.valueOf(infoArr.get(1)));
                                        }
                                        res.put("petName", petName);
                                        res.put("petGender", petGender);

                                        long petPower = 0;
                                        if (petPointStr != null && !petPointStr.isEmpty()) {
                                            JSONArray pointArr = (JSONArray) JSONValue.parse(petPointStr);
                                            if (pointArr != null && pointArr.size() > 1) petPower = Long.parseLong(String.valueOf(pointArr.get(1)));
                                        }
                                        res.put("petPower", petPower);

                                        JSONArray petBodyArr = new JSONArray();
                                        if (petBodyStr != null && !petBodyStr.isEmpty()) {
                                            JSONArray rawArr = (JSONArray) JSONValue.parse(petBodyStr);
                                            if (rawArr != null) {
                                                for (int i = 0; i < rawArr.size(); i++) {
                                                    JSONObject itemObj = parseRawDbItemToJson(String.valueOf(rawArr.get(i)), i);
                                                    if (itemObj != null) petBodyArr.add(itemObj);
                                                }
                                            }
                                        }
                                        res.put("itemsPetBody", petBodyArr);
                                    } else {
                                        res.put("hasPet", false);
                                        res.put("itemsPetBody", new JSONArray());
                                    }
                                } catch (Exception ex) {
                                    res.put("hasPet", false);
                                    res.put("itemsPetBody", new JSONArray());
                                }
                            } else {
                                res.put("hasPet", false);
                                res.put("itemsPetBody", new JSONArray());
                            }

                            sendJsonResponse(exchange, 200, res.toJSONString());
                            return;
                        }
                    }
                }

                sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Không tìm thấy nhân vật '" + playerName + "'!\"}");

            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"Lỗi server: " + e.getMessage() + "\"}");
            }
        }
    }

    // --- GET / POST /api/badges-tasks ---
    private static class BadgesTasksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                String playerName = "";
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null) {
                        for (String param : query.split("&")) {
                            String[] pair = param.split("=");
                            if (pair.length > 1 && "playerName".equalsIgnoreCase(pair[0])) {
                                playerName = java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8).trim();
                            }
                        }
                    }
                } else {
                    InputStream is = exchange.getRequestBody();
                    String bodyStr = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    JSONObject body = (JSONObject) JSONValue.parse(bodyStr);
                    if (body != null && body.get("playerName") != null) {
                        playerName = body.get("playerName").toString().trim();
                    }
                }

                if (playerName.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Thiếu tên nhân vật\"}");
                    return;
                }

                Player player = null;
                for (Player p : Client.gI().getPlayers()) {
                    if (p != null && p.name != null && p.name.equalsIgnoreCase(playerName)) {
                        player = p;
                        break;
                    }
                }

                JSONArray tasksArray = new JSONArray();

                if (player != null) {
                    nro.models.task.BadgesTaskService.checkInitTask(player);
                    if (player.dataTaskBadges != null) {
                        for (nro.models.task.BadgesTask task : player.dataTaskBadges) {
                            if (task != null) {
                                JSONObject obj = new JSONObject();
                                obj.put("id", task.id);
                                String name = nro.models.consts.ConstTaskBadges.getNameById(task.id);
                                if (Manager.TASKS_BADGES_TEMPLATE != null) {
                                    for (nro.models.task.BadgesTaskTemplate btt : Manager.TASKS_BADGES_TEMPLATE) {
                                        if (btt != null && btt.id == task.id && btt.name != null && !btt.name.isEmpty()) {
                                            name = btt.name;
                                            break;
                                        }
                                    }
                                }
                                obj.put("name", name);
                                obj.put("count", task.count);
                                obj.put("countMax", task.countMax);
                                obj.put("idBadgesReward", task.idBadgesReward);
                                obj.put("percent", task.getPercentProcess());
                                obj.put("isDone", task.isDone());

                                boolean hasBadge = false;
                                if (player.dataBadges != null) {
                                    for (nro.models.player_badges.BadgesData bg : player.dataBadges) {
                                        if (bg != null && bg.idBadGes == task.idBadgesReward) {
                                            hasBadge = true;
                                            break;
                                        }
                                    }
                                }
                                obj.put("isRewardGiven", hasBadge);
                                tasksArray.add(obj);
                            }
                        }
                    }
                } else {
                    // Offline DB query
                    try (Connection conn = LocalManager.getConnection();
                         PreparedStatement ps = conn.prepareStatement("SELECT dataTaskBadges, dataBadges FROM player WHERE name = ?")) {
                        ps.setString(1, playerName);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                String taskJson = rs.getString("dataTaskBadges");
                                String badgeJson = rs.getString("dataBadges");

                                JSONArray savedTasks = (taskJson != null && !taskJson.isEmpty()) ? (JSONArray) JSONValue.parse(taskJson) : new JSONArray();
                                JSONArray savedBadges = (badgeJson != null && !badgeJson.isEmpty()) ? (JSONArray) JSONValue.parse(badgeJson) : new JSONArray();

                                if (Manager.TASKS_BADGES_TEMPLATE != null) {
                                    for (nro.models.task.BadgesTaskTemplate btt : Manager.TASKS_BADGES_TEMPLATE) {
                                        if (btt == null) continue;
                                        JSONObject obj = new JSONObject();
                                        obj.put("id", btt.id);
                                        obj.put("name", btt.name != null && !btt.name.isEmpty() ? btt.name : nro.models.consts.ConstTaskBadges.getNameById(btt.id));
                                        
                                        int count = 0;
                                        int countMax = btt.count;
                                        int idBadgesReward = btt.idbadgesReward;

                                        if (savedTasks != null) {
                                            for (Object item : savedTasks) {
                                                JSONObject taskObj = (JSONObject) item;
                                                if (taskObj != null && taskObj.get("id") != null && Integer.parseInt(taskObj.get("id").toString()) == btt.id) {
                                                    count = Integer.parseInt(taskObj.get("count").toString());
                                                    if (taskObj.get("countMax") != null) countMax = Integer.parseInt(taskObj.get("countMax").toString());
                                                    break;
                                                }
                                            }
                                        }

                                        obj.put("count", count);
                                        obj.put("countMax", countMax);
                                        obj.put("idBadgesReward", idBadgesReward);
                                        int percent = countMax > 0 ? (count >= countMax ? 100 : (int)((long)count * 100 / countMax)) : 0;
                                        obj.put("percent", percent);
                                        obj.put("isDone", count >= countMax);

                                        boolean hasBadge = false;
                                        if (savedBadges != null) {
                                            for (Object item : savedBadges) {
                                                JSONObject bObj = (JSONObject) item;
                                                if (bObj != null && bObj.get("idBadGes") != null && Integer.parseInt(bObj.get("idBadGes").toString()) == idBadgesReward) {
                                                    hasBadge = true;
                                                    break;
                                                }
                                            }
                                        }
                                        obj.put("isRewardGiven", hasBadge);
                                        tasksArray.add(obj);
                                    }
                                }
                            }
                        }
                    }
                }

                JSONObject res = new JSONObject();
                res.put("status", "success");
                res.put("playerName", playerName);
                res.put("isOnline", player != null);
                res.put("data", tasksArray);
                sendJsonResponse(exchange, 200, res.toJSONString());
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- POST /api/complete-badges-task ---
    private static class CompleteBadgesTaskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            try {
                InputStream is = exchange.getRequestBody();
                String bodyStr = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject body = (JSONObject) JSONValue.parse(bodyStr);

                String playerName = body != null && body.get("playerName") != null
                        ? body.get("playerName").toString().trim()
                        : "";
                int taskId = body != null && body.get("taskId") != null
                        ? Integer.parseInt(body.get("taskId").toString())
                        : -1;

                if (playerName.isEmpty() || taskId < 0) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Thiếu thông tin tên nhân vật hoặc ID nhiệm vụ danh hiệu\"}");
                    return;
                }

                Player player = null;
                for (Player p : Client.gI().getPlayers()) {
                    if (p != null && p.name != null && p.name.equalsIgnoreCase(playerName)) {
                        player = p;
                        break;
                    }
                }

                String taskName = "ID #" + taskId;

                if (player != null) {
                    nro.models.task.BadgesTaskService.checkInitTask(player);
                    if (player.dataTaskBadges != null) {
                        for (nro.models.task.BadgesTask task : player.dataTaskBadges) {
                            if (task != null && (task.id == taskId || task.idBadgesReward == taskId)) {
                                task.count = task.countMax;
                                taskName = nro.models.consts.ConstTaskBadges.getNameById(task.id);
                                break;
                            }
                        }
                    }
                    nro.models.task.BadgesTaskService.updateDoneTask(player);
                    nro.models.database.PlayerDAO.updatePlayer(player);
                    Service.gI().sendThongBao(player, "Admin vừa duyệt hoàn thành Nhiệm Vụ Danh Hiệu: " + taskName);
                } else {
                    // Offline DB update
                    try (Connection conn = LocalManager.getConnection();
                         PreparedStatement ps = conn.prepareStatement("SELECT id, dataTaskBadges, dataBadges FROM player WHERE name = ?")) {
                        ps.setString(1, playerName);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                int playerId = rs.getInt("id");
                                String taskJson = rs.getString("dataTaskBadges");
                                String badgeJson = rs.getString("dataBadges");

                                JSONArray savedTasks = (taskJson != null && !taskJson.isEmpty()) ? (JSONArray) JSONValue.parse(taskJson) : new JSONArray();
                                JSONArray savedBadges = (badgeJson != null && !badgeJson.isEmpty()) ? (JSONArray) JSONValue.parse(badgeJson) : new JSONArray();

                                int rewardBadgeId = -1;
                                boolean found = false;

                                for (Object item : savedTasks) {
                                    JSONObject tObj = (JSONObject) item;
                                    if (tObj != null && tObj.get("id") != null && Integer.parseInt(tObj.get("id").toString()) == taskId) {
                                        int countMax = Integer.parseInt(tObj.get("countMax").toString());
                                        tObj.put("count", countMax);
                                        rewardBadgeId = Integer.parseInt(tObj.get("idBadgesReward").toString());
                                        found = true;
                                        break;
                                    }
                                }

                                if (!found && Manager.TASKS_BADGES_TEMPLATE != null) {
                                    for (nro.models.task.BadgesTaskTemplate btt : Manager.TASKS_BADGES_TEMPLATE) {
                                        if (btt != null && btt.id == taskId) {
                                            JSONObject newObj = new JSONObject();
                                            newObj.put("id", btt.id);
                                            newObj.put("count", btt.count);
                                            newObj.put("countMax", btt.count);
                                            newObj.put("idBadgesReward", btt.idbadgesReward);
                                            savedTasks.add(newObj);
                                            rewardBadgeId = btt.idbadgesReward;
                                            break;
                                        }
                                    }
                                }

                                if (rewardBadgeId > 0) {
                                    boolean hasBadge = false;
                                    for (Object item : savedBadges) {
                                        JSONObject bObj = (JSONObject) item;
                                        if (bObj != null && bObj.get("idBadGes") != null && Integer.parseInt(bObj.get("idBadGes").toString()) == rewardBadgeId) {
                                            hasBadge = true;
                                            break;
                                        }
                                    }
                                    if (!hasBadge) {
                                        JSONObject newBadge = new JSONObject();
                                        newBadge.put("idBadGes", rewardBadgeId);
                                        newBadge.put("timeofUseBadges", System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000);
                                        newBadge.put("isUse", false);
                                        savedBadges.add(newBadge);
                                    }
                                }

                                try (PreparedStatement updatePs = conn.prepareStatement("UPDATE player SET dataTaskBadges = ?, dataBadges = ? WHERE id = ?")) {
                                    updatePs.setString(1, JSONValue.toJSONString(savedTasks));
                                    updatePs.setString(2, JSONValue.toJSONString(savedBadges));
                                    updatePs.setInt(3, playerId);
                                    updatePs.executeUpdate();
                                }
                            } else {
                                sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Không tìm thấy nhân vật\"}");
                                return;
                            }
                        }
                    }
                }

                JSONObject res = new JSONObject();
                res.put("status", "success");
                res.put("message", "Đã hoàn thành Nhiệm Vụ Danh Hiệu thành công cho nhân vật [" + playerName + "]!");
                sendJsonResponse(exchange, 200, res.toJSONString());
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }
}
