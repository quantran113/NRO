package nro.models.boss.Boss_mini;

import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.PetService;
import nro.models.services.Service;

public class BossBeerusSanDe extends Boss {

    public BossBeerusSanDe() throws Exception {
        super(BossID.BOSS_BEERUS_SAN_DE, true, true, BossesData.BOSS_BEERUS_SAN_DE);
    }

    @Override
    public void reward(Player plKill) {
        if (plKill == null) return;
        Service.gI().sendThongBao(plKill, "Chúc mừng bạn đã hạ gục Thần Beerus và đoạt lấy Trứng Đệ Tử!");

        // Drop Item 568 (Quả Trứng)
        Service.gI().dropItemMap(this.zone, new ItemMap(this.zone, 568, 1,
                this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id));

        // Drop 10 Thỏi Vàng (Item 457)
        // Service.gI().dropItemMap(this.zone, new ItemMap(this.zone, 457, 10,
        //         this.location.x + 20, this.zone.map.yPhysicInTop(this.location.x + 20, this.location.y - 24), plKill.id));

        // If player has no pet yet, automatically gift Kid Beer pet
        if (plKill.pet == null) {
            PetService.gI().createKidBeerPet(plKill);
            Service.gI().sendThongBao(plKill, "Bạn đã nhận được Đệ Tử Kid Beerus Mới!");
        }
    }
}
