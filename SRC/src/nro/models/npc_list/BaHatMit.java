package nro.models.npc_list;

import nro.models.combine.CheTaoCuonSachCu;
import nro.models.combine.CombineService;
import nro.models.combine.DoiSachTuyetKy;
import nro.models.daily_Giftcode.DailyGiftService;
import nro.models.item.Item;
import nro.models.matches.dai_hoi_vo_thuat.DeathOrAliveArena;
import nro.models.matches.giai_dau.DeathOrAliveArenaManager;
import nro.models.matches.dai_hoi_vo_thuat.DeathOrAliveArenaService;

import nro.models.consts.ConstDailyGift;
import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;

import nro.models.player.Player;
import nro.models.map.service.ChangeMapService;
import nro.models.services.InventoryService;

import nro.models.services.ItemService;
import nro.models.services.Service;

import java.util.ArrayList;
import java.util.List;
import nro.models.shop.ShopService;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 *
 */
public class BaHatMit extends Npc {

    public BaHatMit(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            switch (this.mapId) {
                case 5 -> {
                    boolean hasBt2 = InventoryService.gI().findItemBongTaiCap2(player)
                            || InventoryService.gI().findItem(player, 921);
                    boolean hasBt3 = InventoryService.gI().findItem(player, 1819);

                    String textBt2 = hasBt2 ? "Mở chỉ số\nBông tai\nPorata cấp 2" : "Nâng cấp\nBông tai\nPorata";
                    String textBt3 = hasBt3 ? "Mở chỉ số\nBông tai\nPorata cấp 3" : "Nâng cấp\nBông tai\nPorata cấp 3";

                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi tìm ta có việc gì?",
                            "Chức năng\npha lê",
                            "Chuyển hóa\nTrang bị",
                            "Nâng cấp\nVật phẩm",
                            textBt2,
                            textBt3,
                            "Võ đài\nSinh tử",
                            "Phân rã\nTrang bị\nKích hoạt",
                            "Tái tạo\nCapsule\nKích hoạt");
                }
                case 174, 181 ->
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi tìm ta có việc gì?",
                            "Quay về", "Từ chối");
                default -> {
                    boolean hasBt2 = InventoryService.gI().findItemBongTaiCap2(player)
                            || InventoryService.gI().findItem(player, 921);
                    boolean hasBt3 = InventoryService.gI().findItem(player, 1819);

                    String textBt2 = hasBt2 ? "Mở chỉ số\nBông tai\nPorata cấp 2" : "Nâng cấp\nBông tai\nPorata";
                    String textBt3 = hasBt3 ? "Mở chỉ số\nBông tai\nPorata cấp 3" : "Nâng cấp\nBông tai\nPorata cấp 3";

                    List<String> menu = new ArrayList<>();
                    if (DailyGiftService.checkDailyGift(player, ConstDailyGift.NHAN_BUA_MIEN_PHI)) {
                        menu.add("Thưởng\nBùa 1h\nngẫu nhiên");
                    }
                    menu.add("Sách\nTuyệt Kỹ");
                    menu.add("Cửa hàng\nBùa");
                    menu.add("Nâng cấp\nVật phẩm");
                    menu.add(textBt2);
                    menu.add(textBt3);
                    menu.add("Làm phép\nNhập đá");
                    menu.add("Nhập\nNgọc Rồng");

                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                            menu.toArray(new String[0]));
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (this.mapId) {
                case 5 -> {
                    if (player.idMark.isBaseMenu()) {
                        switch (select) {
                            case 0 ->
                                createOtherMenu(player, 3,
                                        "Ta có thể giúp gì cho ngươi ?",
                                        "Ép sao\ntrang bị",
                                        "Pha lê\nhóa\ntrang bị",
                                        "Nâng cấp\nSao pha lê",
                                        "Đánh bóng\nSao pha lê",
                                        "Cường hóa\nlỗ sao\npha lê",
                                        "Tạo đá\nHematite");
                            case 1 ->
                                createOtherMenu(player, 4,
                                        "Ta có thể giúp gì cho ngươi ?",
                                        "Chuyển hóa\nVàng",
                                        "Chuyển hóa\nNgọc");
                            case 2 ->
                                CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_VAT_PHAM);
                            case 3 -> {
                                boolean hasBt2 = InventoryService.gI().findItemBongTaiCap2(player)
                                        || InventoryService.gI().findItem(player, 921);
                                if (hasBt2) {
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CHI_SO_BONG_TAI);
                                } else {
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_BONG_TAI);
                                }
                            }
                            case 4 -> {
                                boolean hasBt3 = InventoryService.gI().findItem(player, 1819);
                                boolean hasBt2 = InventoryService.gI().findItemBongTaiCap2(player)
                                        || InventoryService.gI().findItem(player, 921);
                                if (hasBt3) {
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CHI_SO_BONG_TAI3);
                                } else if (hasBt2) {
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_BONG_TAI3);
                                } else {
                                    Service.gI().sendThongBao(player, "Cần có Bông tai Porata cấp 2 hoặc 3.");
                                }
                            }
                            case 5 ->
                                ChangeMapService.gI().changeMapNonSpaceship(player, 112, 200 + Util.nextInt(-100, 100),
                                        408);
                            case 6 ->
                                CombineService.gI().openTabCombine(player, CombineService.PHAN_RA_TRANG_BI_KH);
                            case 7 ->
                                CombineService.gI().openTabCombine(player, CombineService.TAI_TAO_CAPSULE_KH);
                        }
                    } else if (player.idMark.getIndexMenu() == 3) {
                        switch (select) {
                            case 0 -> CombineService.gI().openTabCombine(player, CombineService.EP_SAO_TRANG_BI);
                            case 1 -> CombineService.gI().openTabCombine(player, CombineService.PHA_LE_HOA_TRANG_BI);
                            case 2 -> CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_SAO_PHA_LE);
                            case 3 -> CombineService.gI().openTabCombine(player, CombineService.DANH_BONG_SAO_PHA_LE);
                            case 4 -> CombineService.gI().openTabCombine(player, CombineService.CUONG_HOA_LO_SAO_PHA_LE);
                            case 5 -> CombineService.gI().openTabCombine(player, CombineService.TAO_DA_HEMATITE);
                        }
                    } else if (player.idMark.getIndexMenu() == 4) {
                        switch (select) {
                            case 0 -> CombineService.gI().openTabCombine(player, CombineService.CHUYEN_HOA_TRANG_BI_VANG);
                            case 1 -> CombineService.gI().openTabCombine(player, CombineService.CHUYEN_HOA_TRANG_BI_NGOC);
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
                        switch (select) {
                            case 0 -> CombineService.gI().startCombine(player);
                            case 1 -> CombineService.gI().startCombineVip(player, 10);
                            case 2 -> CombineService.gI().startCombineVip(player, 100);
                        }
                    }
                }
                case 174 -> {
                    if (player.idMark.isBaseMenu()) {
                        if (select == 0) {
                            ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                        }
                    }
                }
                case 181 -> {
                    if (player.idMark.isBaseMenu()) {
                        if (select == 0) {
                            ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                        }
                    }
                }
                default -> {
                    if (player.idMark.isBaseMenu()) {
                        boolean hasGift = DailyGiftService.checkDailyGift(player, ConstDailyGift.NHAN_BUA_MIEN_PHI);
                        int actionIndex = hasGift ? select : (select + 1);

                        switch (actionIndex) {
                            case 0 -> {
                                if (DailyGiftService.checkDailyGift(player, ConstDailyGift.NHAN_BUA_MIEN_PHI)) {
                                    int idItem = Util.nextInt(213, 219);
                                    player.charms.addTimeCharms(idItem, 60);
                                    Item bua = ItemService.gI().createNewItem((short) idItem);
                                    Service.gI().sendThongBao(player, "Bạn vừa nhận thưởng " + bua.template.name);
                                    DailyGiftService.updateDailyGift(player, ConstDailyGift.NHAN_BUA_MIEN_PHI);
                                } else {
                                    Service.gI().sendThongBao(player, "Hôm nay bạn đã nhận bùa miễn phí rồi!!!");
                                }
                            }
                            case 1 -> {
                                createOtherMenu(player, ConstNpc.MENU_SACH_TUYET_KY, "Ta có thể giúp gì cho ngươi ?",
                                        "Đóng thành\nSách cũ",
                                        "Đổi Sách\nTuyệt kỹ",
                                        "Giám định\nSách",
                                        "Tẩy\nSách",
                                        "Nâng cấp\nSách\nTuyệt kỹ",
                                        "Hồi phục\nSách",
                                        "Phân rã\nSách");
                            }
                            case 2 -> {
                                createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA,
                                        "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để "
                                                + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                        "Bùa\n1 giờ",
                                        "Bùa\n8 giờ",
                                        "Bùa\n1 tháng", "Đóng");
                            }
                            case 3 -> {
                                CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_VAT_PHAM);
                            }
                            case 4 -> {
                                boolean hasBt2 = InventoryService.gI().findItemBongTaiCap2(player)
                                        || InventoryService.gI().findItem(player, 921);
                                if (hasBt2) {
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CHI_SO_BONG_TAI);
                                } else {
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_BONG_TAI);
                                }
                            }
                            case 5 -> {
                                boolean hasBt3 = InventoryService.gI().findItem(player, 1819);
                                boolean hasBt2 = InventoryService.gI().findItemBongTaiCap2(player)
                                        || InventoryService.gI().findItem(player, 921);
                                if (hasBt3) {
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CHI_SO_BONG_TAI3);
                                } else if (hasBt2) {
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_BONG_TAI3);
                                } else {
                                    Service.gI().sendThongBao(player, "Cần có Bông tai Porata cấp 2 hoặc 3.");
                                }
                            }
                            case 6 -> {
                                CombineService.gI().openTabCombine(player, CombineService.LAM_PHEP_NHAP_DA);
                            }
                            case 7 -> {
                                CombineService.gI().openTabCombine(player, CombineService.NHAP_NGOC_RONG);
                            }
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_SACH_TUYET_KY) {
                        switch (select) {
                            case 0 -> CheTaoCuonSachCu.showCombine(player);
                            case 1 -> DoiSachTuyetKy.showCombine(player);
                            case 2 -> CombineService.gI().openTabCombine(player, CombineService.GIAM_DINH_SACH);
                            case 3 -> CombineService.gI().openTabCombine(player, CombineService.TAY_SACH);
                            case 4 -> CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_SACH_TUYET_KY);
                            case 5 -> CombineService.gI().openTabCombine(player, CombineService.HOI_PHUC_SACH);
                            case 6 -> CombineService.gI().openTabCombine(player, CombineService.PHAN_RA_SACH);
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_OPTION_SHOP_BUA) {
                        switch (select) {
                            case 0 -> ShopService.gI().opendShop(player, "BUA_1H", true);
                            case 1 -> ShopService.gI().opendShop(player, "BUA_8H", true);
                            case 2 -> ShopService.gI().opendShop(player, "BUA_1M", true);
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.DONG_THANH_SACH_CU) {
                        CheTaoCuonSachCu.cheTaoCuonSachCu(player);
                    } else if (player.idMark.getIndexMenu() == ConstNpc.DOI_SACH_TUYET_KY) {
                        if (select == 0) {
                            DoiSachTuyetKy.doiSachTuyetKy(player, false);
                        } else if (select == 1 && InventoryService.gI().findItemBag(player, 1794) != null) {
                            DoiSachTuyetKy.doiSachTuyetKy(player, true);
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
                        switch (select) {
                            case 0 -> CombineService.gI().startCombine(player);
                            case 1 -> CombineService.gI().startCombineVip(player, 10);
                            case 2 -> CombineService.gI().startCombineVip(player, 100);
                        }
                    }
                }
            }
        }
    }
}
