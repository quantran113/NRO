package nro.models.Bot;

import java.util.Random;
import java.util.List;
import nro.models.item.Item;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import nro.models.services.PlayerService;
import nro.models.services.ChatGlobalService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services_func.Trade;

public class BotGiaoDich {

    public int idItem;
    public int idItTd;
    public int slot;

    private long lastimeChat;
    private long lastimeChatTrain;
    private Trade trade;

    public Bot bot;
    private Player pl;

    public BotGiaoDich(int item, int traodoi, int slot) {
        this.idItem = item;
        this.idItTd = traodoi;
        this.slot = slot;
    }

    public BotGiaoDich(BotGiaoDich shop) {
        if (shop != null) {
            this.idItem = shop.idItem;
            this.idItTd = shop.idItTd;
            this.slot = shop.slot;
        } else {
            this.idItem = 457;
            this.idItTd = 457;
            this.slot = 1;
        }
    }

    public void update() {
        if (this.bot == null) {
            return;
        }
        this.mapL();
        this.chat();
    }

    public String getChat() {
        try {
            Item it = ItemService.gI().createNewItem((short) this.idItem);
            Item it1 = ItemService.gI().createNewItem((short) this.idItTd);
            String name1 = (it != null && it.template != null) ? it.template.name : "Vật phẩm";
            String name2 = (it1 != null && it1.template != null) ? it1.template.name : "Thỏi vàng";
            String mapName = (this.bot != null && this.bot.zone != null && this.bot.zone.map != null) ? this.bot.zone.map.mapName : "Map";
            int zoneId = (this.bot != null && this.bot.zone != null) ? this.bot.zone.zoneId : 0;
            return String.format("Bán %s x%d lấy %s tại %s khu %d", name1, this.slot, name2, mapName, zoneId);
        } catch (Exception e) {
            return "Bán đồ giao dịch tại Map!";
        }
    }

    public void chat() {
        if (this.bot == null) return;
        if (this.lastimeChat < (System.currentTimeMillis() - ((100 + new Random().nextInt(100)) * 1000))) {
            ChatGlobalService.gI().chat1(this.bot, this.getChat());
            this.lastimeChat = System.currentTimeMillis();
        }
        if (this.lastimeChatTrain < (System.currentTimeMillis() - ((5 + new Random().nextInt(5)) * 1000))) {
            Service.gI().chat(this.bot, getChat());
            this.lastimeChatTrain = System.currentTimeMillis();
        }
    }

    public void activeTraDe(Player pl) {
        if (this.bot == null || pl == null) return;
        trade = new Trade(pl, bot);
        this.pl = pl;
        this.trade.openTabTrade();
    }

    public void CheckTraDe(List<Item> item) {
        if (item == null || this.trade == null) return;
        int slot1 = item.stream()
                .filter(it -> it != null && it.template != null && it.template.id == this.idItTd && it.quantity >= this.slot)
                .mapToInt(it -> it.quantity)
                .findFirst()
                .orElse(0);
        boolean check = slot1 > 0;
        if (check) {
            active(slot1);
        } else {
            this.trade.cancelTrade();
        }
    }

    public void active(int sl) {
        if (this.trade == null || this.bot == null) return;
        int sl1 = (int) Math.round((double) sl / (this.slot > 0 ? this.slot : 1));
        Item it = ItemService.gI().createNewItem((short) this.idItem, sl1);
        this.trade.addItemBot(it);
        this.trade.lockTran(this.bot);
        this.trade.acceptTrade();
    }

    public void mapL() {
        if (this.bot == null || this.bot.zone == null || this.bot.zone.map == null) return;
        int currentMap = this.bot.zone.map.mapId;
        // Keep in current village map if placed there, or teleport to Map 84 (Trạm phi thuyền) if elsewhere
        if (currentMap != 84 && currentMap != 0 && currentMap != 7 && currentMap != 14 && currentMap != 5) {
            Zone zone = this.bot.getRandomZone(84);
            if (zone != null) {
                ChangeMapService.gI().goToMap(this.bot, zone);
                this.bot.zone.load_Me_To_Another(this.bot);
                PlayerService.gI().playerMove(this.bot, 81 + new Random().nextInt(716), 336);
            }
        }
    }
}