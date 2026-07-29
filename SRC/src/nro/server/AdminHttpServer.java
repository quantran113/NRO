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
            server.createContext("/api/reload-giftcode", new ReloadGiftCodeHandler());
            server.createContext("/api/use-giftcode", new UseGiftCodeHandler());
            server.createContext("/api/adjust-player-power", new AdjustPlayerPowerHandler());
            
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
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Dữ liệu yêu cầu không hợp lệ\"}");
                    return;
                }

                String playerName = (String) body.get("playerName");
                long itemId = ((Number) body.get("itemId")).longValue();
                int quantity = body.containsKey("quantity") ? ((Number) body.get("quantity")).intValue() : 1;
                int starCount = body.containsKey("stars") ? ((Number) body.get("stars")).intValue() : 0;
                JSONArray optionsReq = (JSONArray) body.get("options");

                if (playerName == null || playerName.trim().isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Tên nhân vật không được để trống\"}");
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
                        sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Vật phẩm không tồn tại (ID: " + itemId + ")\"}");
                        return;
                    }

                    // Add star slots option (107)
                    if (starCount > 0) {
                        item.itemOptions.add(new Item.ItemOption(107, starCount));
                    }

                    // Add custom options
                    if (optionsReq != null) {
                        for (Object o : optionsReq) {
                            JSONObject optObj = (JSONObject) o;
                            int optId = ((Number) optObj.get("id")).intValue();
                            int param = ((Number) optObj.get("param")).intValue();
                            item.itemOptions.add(new Item.ItemOption(optId, param));
                        }
                    }

                    boolean added = InventoryService.gI().addItemBag(targetPlayer, item);
                    if (added) {
                        InventoryService.gI().sendItemBags(targetPlayer);
                        Service.gI().sendThongBao(targetPlayer, "Admin vừa cấp cho bạn: " + item.template.name + " (x" + quantity + ")");
                        
                        JSONObject res = new JSONObject();
                        res.put("status", "success");
                        res.put("message", "Đã cấp thành công [" + item.template.name + "] cho nhân vật " + targetPlayer.name + " (Đang ONLINE)");
                        sendJsonResponse(exchange, 200, res.toJSONString());
                    } else {
                        sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Hành trang nhân vật " + targetPlayer.name + " đã đầy!\"}");
                    }
                } else {
                    // --- PLAYER IS OFFLINE ---
                    try (Connection conn = LocalManager.getConnection();
                         PreparedStatement psSelect = conn.prepareStatement("SELECT id, items_bag FROM player WHERE name = ?")) {
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
                                sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Hành trang nhân vật " + playerName + " đã đầy! (30/30 ô)\"}");
                                return;
                            }

                            // Save back to DB
                            try (PreparedStatement psUpdate = conn.prepareStatement("UPDATE player SET items_bag = ? WHERE id = ?")) {
                                psUpdate.setString(1, bagArray.toJSONString());
                                psUpdate.setLong(2, pid);
                                psUpdate.executeUpdate();
                            }

                            JSONObject res = new JSONObject();
                            res.put("status", "success");
                            res.put("message", "Đã cấp thành công vật phẩm cho nhân vật " + playerName + " (Đang OFFLINE)");
                            sendJsonResponse(exchange, 200, res.toJSONString());
                        } else {
                            sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Không tìm thấy nhân vật [" + playerName + "] trong dữ liệu\"}");
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
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Tên nhân vật không được để trống\"}");
                    return;
                }

                if (itemsReq == null || itemsReq.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Danh sách vật phẩm cấp trống\"}");
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
                        int quantity = itemJson.containsKey("quantity") ? ((Number) itemJson.get("quantity")).intValue() : 1;
                        int starCount = itemJson.containsKey("stars") ? ((Number) itemJson.get("stars")).intValue() : 0;
                        JSONArray optionsReq = (JSONArray) itemJson.get("options");

                        Item item = ItemService.gI().createNewItem((short) itemId, quantity);
                        if (item != null) {
                            if (starCount > 0) {
                                item.itemOptions.add(new Item.ItemOption(107, starCount));
                            }
                            if (optionsReq != null) {
                                for (Object o : optionsReq) {
                                    JSONObject optObj = (JSONObject) o;
                                    int optId = ((Number) optObj.get("id")).intValue();
                                    int param = ((Number) optObj.get("param")).intValue();
                                    item.itemOptions.add(new Item.ItemOption(optId, param));
                                }
                            }
                            boolean added = InventoryService.gI().addItemBag(targetPlayer, item);
                            if (added) successCount++;
                        }
                    }

                    if (successCount > 0) {
                        InventoryService.gI().sendItemBags(targetPlayer);
                        Service.gI().sendThongBao(targetPlayer, "Admin vừa cấp cho bạn " + successCount + " vật phẩm!");
                        
                        JSONObject res = new JSONObject();
                        res.put("status", "success");
                        res.put("message", "Đã cấp thành công " + successCount + " vật phẩm cho nhân vật [" + targetPlayer.name + "] (Đang ONLINE)");
                        sendJsonResponse(exchange, 200, res.toJSONString());
                    } else {
                        sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Hành trang nhân vật đã đầy!\"}");
                    }
                } else {
                    // --- OFFLINE PLAYER ---
                    try (Connection conn = LocalManager.getConnection();
                         PreparedStatement psSelect = conn.prepareStatement("SELECT id, items_bag FROM player WHERE name = ?")) {
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
                                int quantity = itemJson.containsKey("quantity") ? ((Number) itemJson.get("quantity")).intValue() : 1;
                                int starCount = itemJson.containsKey("stars") ? ((Number) itemJson.get("stars")).intValue() : 0;
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

                            try (PreparedStatement psUpdate = conn.prepareStatement("UPDATE player SET items_bag = ? WHERE id = ?")) {
                                psUpdate.setString(1, bagArray.toJSONString());
                                psUpdate.setLong(2, pid);
                                psUpdate.executeUpdate();
                            }

                            JSONObject res = new JSONObject();
                            res.put("status", "success");
                            res.put("message", "Đã cấp thành công " + successCount + " vật phẩm cho nhân vật [" + playerName + "] (Đang OFFLINE)");
                            sendJsonResponse(exchange, 200, res.toJSONString());
                        } else {
                            sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Không tìm thấy nhân vật [" + playerName + "]\"}");
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
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Tên nhân vật không được để trống\"}");
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
                            case 1: nro.models.services.PetService.gI().changeMabuPet(targetPlayer, petGender); break;
                            case 2: nro.models.services.PetService.gI().changeUubPet(targetPlayer); break;
                            case 3: nro.models.services.PetService.gI().changeKidBeerPet(targetPlayer); break;
                            case 4: nro.models.services.PetService.gI().changeJirenPet(targetPlayer); break;
                            default: nro.models.services.PetService.gI().changeNormalPet(targetPlayer, petGender); break;
                        }
                    } else {
                        switch (petType) {
                            case 1: nro.models.services.PetService.gI().createMabuPet(targetPlayer, petGender); break;
                            case 2: nro.models.services.PetService.gI().createUubPet(targetPlayer); break;
                            case 3: nro.models.services.PetService.gI().createKidBeerPet(targetPlayer); break;
                            case 4: nro.models.services.PetService.gI().createJirenPet(targetPlayer); break;
                            default: nro.models.services.PetService.gI().createNormalPet(targetPlayer, petGender); break;
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
                                    if (pVal >= 80000000000L) calcLimit = 9;
                                    else if (pVal >= 70000000000L) calcLimit = 8;
                                    else if (pVal >= 60000000000L) calcLimit = 7;
                                    else if (pVal >= 50000000000L) calcLimit = 6;
                                    else if (pVal >= 39000000000L) calcLimit = 5;
                                    else if (pVal >= 29000000000L) calcLimit = 4;
                                    else if (pVal >= 24000000000L) calcLimit = 3;
                                    else if (pVal >= 19000000000L) calcLimit = 2;
                                    else if (pVal >= 17000000000L) calcLimit = 1;

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
                                        if (plTarget.pet.playerSkill.skills.size() > 4 && pVal >= 40000000000L && pType >= 2) {
                                            plTarget.pet.openSkill5();
                                        }
                                    }

                                    plTarget.pet.joinMapMaster();
                                    nro.models.services.Service.gI().point(plTarget);
                                    nro.models.database.PlayerDAO.updatePlayer(plTarget);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }

                    JSONObject res = new JSONObject();
                    res.put("status", "success");
                    res.put("message", "Đã cấp/đổi Đệ tử thành công cho nhân vật [" + targetPlayer.name + "] (Đang ONLINE)");
                    sendJsonResponse(exchange, 200, res.toJSONString());
                } else {
                    sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Nhân vật [" + playerName + "] đang OFFLINE\"}");
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
                        if (body.containsKey("lunarNewYear")) EventManager.LUNNAR_NEW_YEAR = (Boolean) body.get("lunarNewYear");
                        if (body.containsKey("womensDay")) EventManager.INTERNATIONAL_WOMANS_DAY = (Boolean) body.get("womensDay");
                        if (body.containsKey("halloween")) EventManager.HALLOWEEN = (Boolean) body.get("halloween");
                        if (body.containsKey("christmas")) EventManager.CHRISTMAS = (Boolean) body.get("christmas");
                        if (body.containsKey("hungVuong")) EventManager.HUNG_VUONG = (Boolean) body.get("hungVuong");
                        if (body.containsKey("trungThu")) EventManager.TRUNG_THU = (Boolean) body.get("trungThu");
                        if (body.containsKey("topUp")) EventManager.TOP_UP = (Boolean) body.get("topUp");
                        
                        try {
                            EventManager.gI().init();
                        } catch (Exception ex) {}
                        
                        JSONObject res = new JSONObject();
                        res.put("status", "success");
                        res.put("message", "Đã cập nhật Hệ số X" + Manager.RATE_EXP_SERVER + " TNSM và Sự Kiện Server thành công!");
                        sendJsonResponse(exchange, 200, res.toJSONString());
                    } else {
                        sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Dữ liệu không hợp lệ\"}");
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
                            sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã thêm quy tắc rơi đồ theo map thành công!\"}");
                        } else if ("delete".equals(action)) {
                            int id = ((Number) body.get("id")).intValue();
                            nro.models.item.DropManager.gI().removeRule(id);
                            sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã xóa quy tắc rơi đồ!\"}");
                        } else if ("toggle".equals(action)) {
                            int id = ((Number) body.get("id")).intValue();
                            for (nro.models.item.DropManager.DropRule r : nro.models.item.DropManager.dropRules) {
                                if (r.id == id) {
                                    r.active = !r.active;
                                    break;
                                }
                            }
                            sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã thay đổi trạng thái quy tắc rơi đồ!\"}");
                        } else {
                            sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Hành động không hợp lệ\"}");
                        }
                    } else {
                        sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Dữ liệu không hợp lệ\"}");
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
                                shopObj.put("npcName", rs.getString("npc_name") != null ? rs.getString("npc_name") : ("NPC #" + rs.getInt("npc_id")));
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
                                                        itemObj.put("name", rsItem.getString("item_name") != null ? rsItem.getString("item_name") : ("Item #" + rsItem.getInt("temp_id")));
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
                            int iconSpec = body.containsKey("iconSpec") ? ((Number) body.get("iconSpec")).intValue() : -1;
                            int isNew = body.containsKey("isNew") ? ((Number) body.get("isNew")).intValue() : 0;
                            JSONArray options = (JSONArray) body.get("options");

                            try (Connection conn = LocalManager.getConnection()) {
                                String sqlInsert = "INSERT INTO item_shop (tab_id, temp_id, is_new, cost, icon_spec, type_sell, is_sell, create_time) VALUES (?, ?, ?, ?, ?, ?, 1, NOW())";
                                try (PreparedStatement ps = conn.prepareStatement(sqlInsert, java.sql.Statement.RETURN_GENERATED_KEYS)) {
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
                                    } catch (Exception ex) {}
                                }
                            }
                            sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã thêm vật phẩm vào Shop NPC thành công!\"}");
                        } else if ("delete_item".equals(action)) {
                            int itemShopId = ((Number) body.get("itemShopId")).intValue();
                            try (Connection conn = LocalManager.getConnection()) {
                                try (PreparedStatement psOpt = conn.prepareStatement("DELETE FROM item_shop_option WHERE item_shop_id = ?")) {
                                    psOpt.setInt(1, itemShopId);
                                    psOpt.executeUpdate();
                                }
                                try (PreparedStatement psItem = conn.prepareStatement("DELETE FROM item_shop WHERE id = ?")) {
                                    psItem.setInt(1, itemShopId);
                                    psItem.executeUpdate();
                                }
                                // Reload in-memory shops if server is running
                                try {
                                    nro.models.server.Manager.SHOPS = nro.models.database.ShopDAO.getShops(conn);
                                } catch (Exception ex) {}
                            }
                            sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã xóa vật phẩm khỏi Shop NPC!\"}");
                        } else {
                            sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Hành động không hợp lệ\"}");
                        }
                    } else {
                        sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Dữ liệu không hợp lệ\"}");
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

                String playerName = body != null && body.get("playerName") != null ? body.get("playerName").toString().trim() : "";
                int taskId = body != null && body.get("taskId") != null ? Integer.parseInt(body.get("taskId").toString()) : -1;

                if (playerName.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Thiếu tên nhân vật\"}");
                    return;
                }

                Player player = null;
                for (Player p : Client.gI().getPlayers()) {
                    if (p != null) {
                        if ((p.name != null && p.name.equalsIgnoreCase(playerName)) ||
                            (p.getSession() != null && p.getSession().uu != null && p.getSession().uu.equalsIgnoreCase(playerName))) {
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
                    Service.gI().sendThongBao(player, "Admin đã chuyển nhiệm vụ cho bạn: " + player.playerTask.taskMain.name);

                    try (Connection conn = LocalManager.getConnection();
                         PreparedStatement psUpdate = conn.prepareStatement("UPDATE player SET data_task = ? WHERE id = ?")) {
                        JSONArray taskArr = new JSONArray();
                        taskArr.add(player.playerTask.taskMain.id);
                        taskArr.add(player.playerTask.taskMain.index);
                        if (player.playerTask.taskMain.subTasks != null && !player.playerTask.taskMain.subTasks.isEmpty()) {
                            taskArr.add(player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count);
                        } else {
                            taskArr.add(0);
                        }
                        taskArr.add(System.currentTimeMillis());
                        psUpdate.setString(1, taskArr.toJSONString());
                        psUpdate.setLong(2, player.id);
                        psUpdate.executeUpdate();
                    } catch (Exception ex) {}

                    sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã chuyển qua nhiệm vụ LIVE (Online) cho [" + player.name + "] sang nhiệm vụ #" + player.playerTask.taskMain.id + "!\"}");
                    return;
                }

                // If offline or player not in memory, update MySQL directly
                try (Connection conn = LocalManager.getConnection();
                     PreparedStatement psSelect = conn.prepareStatement("SELECT id, gender, data_task FROM player WHERE name = ?")) {
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
                            taskArr.add(0); taskArr.add(0); taskArr.add(0);
                        }
                        int currentTaskId = Integer.parseInt(taskArr.get(0).toString());
                        int nextTaskId = taskId >= 0 ? taskId : (currentTaskId + 1);

                        taskArr.set(0, nextTaskId);
                        taskArr.set(1, 0);
                        taskArr.set(2, 0);

                        try (PreparedStatement psUpdate = conn.prepareStatement("UPDATE player SET data_task = ? WHERE id = ?")) {
                            psUpdate.setString(1, taskArr.toJSONString());
                            psUpdate.setInt(2, pId);
                            psUpdate.executeUpdate();
                        }

                        sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã chuyển nhiệm vụ (offline) cho [" + playerName + "] sang nhiệm vụ #" + nextTaskId + "!\"}");
                        return;
                    }
                }

                sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Không tìm thấy nhân vật [" + playerName + "]\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
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
                sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã tải lại danh sách GiftCode (" + GiftCodeManager.gI().listGiftCode.size() + " mã)!\"}");
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

                String playerName = body != null && body.get("playerName") != null ? body.get("playerName").toString().trim() : "";
                String code = body != null && body.get("code") != null ? body.get("code").toString().trim() : "";

                if (playerName.isEmpty() || code.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Thiếu tên nhân vật hoặc mã giftcode\"}");
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
                    sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Nhân vật [" + playerName + "] phải ONLINE trong game để nhập giftcode!\"}");
                    return;
                }

                GiftCodeService.gI().giftCode(player, code);
                sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã xử lý nhập GiftCode cho [" + player.name + "]!\"}");
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
                    nro.models.services.Service.gI().sendThongBao(player, "Admin vừa điều chỉnh Sức mạnh & Tiềm năng cho bạn!");
                    nro.models.database.PlayerDAO.updatePlayer(player);

                    sendJsonResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Đã điều chỉnh SM & Tiềm năng cho nhân vật [" + player.name + "] (ONLINE)!\"}");
                } else {
                    sendJsonResponse(exchange, 404, "{\"status\": \"error\", \"message\": \"Nhân vật [" + playerName + "] phải ONLINE trong game để điều chỉnh Sức Mạnh!\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }
}

