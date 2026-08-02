package nro.models.task;

import nro.models.task.BadgesTask;
import nro.models.player.Player;
import nro.models.player_badges.BadgesData;
import nro.models.server.Manager;
import nro.models.task.BadgesTaskTemplate;

/**
 *
 * @author By Mr Blue
 * 
 */

public class BadgesTaskService {

    public static void createAndResetTask(Player player) {
        player.dataTaskBadges.clear();
        for (BadgesTaskTemplate BTT : Manager.TASKS_BADGES_TEMPLATE) {
            BadgesTask data = new BadgesTask();
            data.id = BTT.id;
            data.count = 0;
            data.countMax = BTT.count;
            data.idBadgesReward = BTT.idbadgesReward;
            player.dataTaskBadges.add(data);
        }
    }

    public static void updateDoneTask(Player player) {
        if (player == null || player.dataTaskBadges == null) {
            return;
        }
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
                        player.dataBadges = new java.util.ArrayList<>();
                    }
                    player.dataBadges.add(danhHieu);
                }
            }
        }
    }

    public static void updateCountBagesTask(Player player, int id, int amount) {
        if (player == null || player.dataTaskBadges == null) {
            return;
        }
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
        for (BadgesData data : player.dataBadges) {
            if (data.idBadGes == id) {
                long timeDifference = data.timeofUseBadges - System.currentTimeMillis();
                return (int) (timeDifference / (24 * 60 * 60 * 1000L));
            }
        }
        return 0;
    }

}
