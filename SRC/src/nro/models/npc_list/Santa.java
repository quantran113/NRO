package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.shop.ShopService;
import nro.models.services_func.Input;
import nro.models.services.InventoryService;

public class Santa extends Npc {

    public Santa(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {

            Item pGG = InventoryService.gI().findItem(player.inventory.itemsBag, 459);
            int soLuong = 0;
            if (pGG != null) {
                soLuong = pGG.quantity;
            }
            List<String> menu = new ArrayList<>(Arrays.asList(
                    "Cửa hàng",
                    "Cửa hàng\nVIP",
                    "Mở rộng\nHành trang\nRương đồ",
                    "Nhập mã\nquà tặng",
                    "Cửa hàng\nHạn sử dụng",
                    "Tiệm\nHớt tóc",
                    "Danh\nhiệu"));

            if (soLuong >= 1) {
                menu.add(1, "Giảm giá\n80%");
            }

            String[] menus = menu.toArray(new String[0]);

            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Xin chào, ta có một số vật phẩm đặc biệt cậu có muốn xem không?", menus);
        }

    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            Item pGG = InventoryService.gI().findItem(player.inventory.itemsBag, 459);
            boolean hasCoupon = (pGG != null && pGG.quantity >= 1);

            if (this.mapId == 5 || this.mapId == 13 || this.mapId == 20) {
                if (player.idMark.isBaseMenu()) {
                    if (hasCoupon) {
                        switch (select) {
                            case 0 -> ShopService.gI().opendShop(player, "SANTA", false);
                            case 1 -> ShopService.gI().opendShop(player, "SANTA_GIAM_GIA_1", false);
                            case 2 -> ShopService.gI().opendShop(player, "SHOP_VIP", false);
                            case 3 -> ShopService.gI().opendShop(player, "SANTA_MO_RONG_HANH_TRANG", false);
                            case 4 -> Input.gI().createFormGiftCode(player);
                            case 5 -> ShopService.gI().opendShop(player, "SANTA_HAN_SU_DUNG", false);
                            case 6 -> ShopService.gI().opendShop(player, "SANTA_HEAD", false);
                            case 7 -> ShopService.gI().opendShop(player, "SANTA_DANH_HIEU", false);
                        }
                    } else {
                        switch (select) {
                            case 0 -> ShopService.gI().opendShop(player, "SANTA", false);
                            case 1 -> ShopService.gI().opendShop(player, "SHOP_VIP", false);
                            case 2 -> ShopService.gI().opendShop(player, "SANTA_MO_RONG_HANH_TRANG", false);
                            case 3 -> Input.gI().createFormGiftCode(player);
                            case 4 -> ShopService.gI().opendShop(player, "SANTA_HAN_SU_DUNG", false);
                            case 5 -> ShopService.gI().opendShop(player, "SANTA_HEAD", false);
                            case 6 -> ShopService.gI().opendShop(player, "SANTA_DANH_HIEU", false);
                        }
                    }
                }
            }
        }
    }
}
