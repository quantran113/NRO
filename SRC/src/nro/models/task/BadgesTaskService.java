package nro.models.task;

import nro.models.task.BadgesTask;
import nro.models.player.Player;
import nro.models.player.Pet;
import nro.models.player_badges.BadgesData;
import nro.models.server.Manager;
import nro.models.task.BadgesTaskTemplate;
import nro.models.consts.ConstTaskBadges;
import nro.models.item.Item;
import nro.models.services.Service;

import java.util.ArrayList;

/**
 *
 * @author By Mr Blue
 * 
 */

public class BadgesTaskService {

    public static void checkInitTask(Player player) {
        if (player == null) {
            return;
        }
        if (player.dataTaskBadges == null) {
            player.dataTaskBadges = new ArrayList<>();
        }
        if (Manager.TASKS_BADGES_TEMPLATE != null) {
            for (BadgesTaskTemplate BTT : Manager.TASKS_BADGES_TEMPLATE) {
                boolean exists = false;
                for (BadgesTask data : player.dataTaskBadges) {
                    if (data != null && data.id == BTT.id) {
                        exists = true;
                        data.countMax = BTT.count;
                        data.idBadgesReward = BTT.idbadgesReward;
                        break;
                    }
                }
                if (!exists) {
                    BadgesTask data = new BadgesTask();
                    data.id = BTT.id;
                    data.count = 0;
                    data.countMax = BTT.count;
                    data.idBadgesReward = BTT.idbadgesReward;
                    player.dataTaskBadges.add(data);
                }
            }
        }
    }

    public static void createAndResetTask(Player player) {
        if (player == null) {
            return;
        }
        if (player.dataTaskBadges == null) {
            player.dataTaskBadges = new ArrayList<>();
        } else {
            player.dataTaskBadges.clear();
        }
        if (Manager.TASKS_BADGES_TEMPLATE != null) {
            for (BadgesTaskTemplate BTT : Manager.TASKS_BADGES_TEMPLATE) {
                BadgesTask data = new BadgesTask();
                data.id = BTT.id;
                data.count = 0;
                data.countMax = BTT.count;
                data.idBadgesReward = BTT.idbadgesReward;
                player.dataTaskBadges.add(data);
            }
        }
    }

    public static void updateDoneTask(Player player) {
        if (player == null) {
            return;
        }
        checkInitTask(player);
        for (BadgesTask data : player.dataTaskBadges) {
            if (data != null && data.isDone()) {
                boolean alreadyHave = false;
                if (player.dataBadges != null) {
                    for (BadgesData bg : player.dataBadges) {
                        if (bg != null && bg.idBadGes == data.idBadgesReward) {
                            alreadyHave = true;
                            break;
                        }
                    }
                }
                if (!alreadyHave) {
                    BadgesData danhHieu = new BadgesData(player, data.idBadgesReward, 30);
                    if (player.dataBadges == null) {
                        player.dataBadges = new ArrayList<>();
                    }
                    player.dataBadges.add(danhHieu);
                    Service.gI().sendThongBao(player, "Chúc mừng bạn vừa mở khóa Danh Hiệu mới!");
                }
            }
        }
    }

    public static void updateCountBagesTask(Player player, int id, int amount) {
        if (player == null) {
            return;
        }
        if (player.isPet) {
            player = ((Pet) player).master;
        }
        if (player == null) {
            return;
        }
        checkInitTask(player);

        for (BadgesTask data : player.dataTaskBadges) {
            if (data != null && data.id == id) {
                data.count += amount;
                if (data.count > data.countMax) {
                    data.count = data.countMax;
                }
                break;
            }
        }
        updateDoneTask(player);
    }

    public static boolean hasPlus7Item(Player player) {
        if (player == null || player.inventory == null) {
            return false;
        }
        if (player.inventory.itemsBody != null) {
            for (Item item : player.inventory.itemsBody) {
                if (item != null && item.isNotNullItem() && item.itemOptions != null) {
                    for (Item.ItemOption io : item.itemOptions) {
                        if (io != null && io.optionTemplate != null && io.optionTemplate.id == 72 && io.param >= 7) {
                            return true;
                        }
                    }
                }
            }
        }
        if (player.inventory.itemsBag != null) {
            for (Item item : player.inventory.itemsBag) {
                if (item != null && item.isNotNullItem() && item.itemOptions != null) {
                    for (Item.ItemOption io : item.itemOptions) {
                        if (io != null && io.optionTemplate != null && io.optionTemplate.id == 72 && io.param >= 7) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static int sendPercenBadgesTask(Player player, int idBadgesReward) {
        if (player == null) {
            return 0;
        }
        if (player.dataBadges != null) {
            for (BadgesData bg : player.dataBadges) {
                if (bg != null && bg.idBadGes == idBadgesReward) {
                    return 100;
                }
            }
        }

        checkInitTask(player);
        if (player.dataTaskBadges != null) {
            for (BadgesTask data : player.dataTaskBadges) {
                if (data != null && data.idBadgesReward == idBadgesReward) {
                    return data.getPercentProcess();
                }
            }
        }
        return 0;
    }

    public static int sendDay(Player player, int id) {
        if (player == null || player.dataBadges == null) {
            return 0;
        }
        for (BadgesData data : player.dataBadges) {
            if (data != null && data.idBadGes == id) {
                long timeDifference = data.timeofUseBadges - System.currentTimeMillis();
                return (int) (timeDifference / (24 * 60 * 60 * 1000L));
            }
        }
        return 0;
    }

}
