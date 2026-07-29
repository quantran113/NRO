/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  nro.models.Bot.BotManager
 *  nro.models.boss.Boss_Manager.BossManager
 *  nro.models.boss.Boss_Manager.BrolyManager
 *  nro.models.boss.Boss_Manager.FinalBossManager
 *  nro.models.boss.Boss_Manager.GasDestroyManager
 *  nro.models.boss.Boss_Manager.OtherBossManager
 *  nro.models.boss.Boss_Manager.RedRibbonHQManager
 *  nro.models.boss.Boss_Manager.SkillSummonedManager
 *  nro.models.boss.Boss_Manager.SnakeWayManager
 *  nro.models.boss.Boss_Manager.TreasureUnderSeaManager
 *  nro.models.boss.Boss_Manager.YardartManager
 *  nro.models.data.LocalManager
 *  nro.models.database.HistoryTransactionDAO
 *  nro.models.event.EventManager
 *  nro.models.interfaces.IKeySessionHandler
 *  nro.models.interfaces.IMessageHandler
 *  nro.models.interfaces.IMessageSendCollect
 *  nro.models.interfaces.ISession
 *  nro.models.interfaces.ISessionAcceptHandler
 *  nro.models.managers.ShenronEventManager
 *  nro.models.managers.SuperRankManager
 *  nro.models.map.Map
 *  nro.models.matches.giai_dau.DeathOrAliveArenaManager
 *  nro.models.matches.giai_dau.The23rdMartialArtCongressManager
 *  nro.models.matches.giai_dau.WorldMartialArtsTournamentManager
 *  nro.models.minigame.ChonAiDay_Gem
 *  nro.models.minigame.ChonAiDay_Gold
 *  nro.models.minigame.ConSoMayManGem
 *  nro.models.minigame.ConSoMayManGold
 *  nro.models.network.MessageSendCollect
 *  nro.models.network.MyKeyHandler
 *  nro.models.network.MySession
 *  nro.models.network.Network
 *  nro.models.server.AutoMaintenance
 *  nro.models.server.Client
 *  nro.models.server.Controller
 *  nro.models.server.Maintenance
 *  nro.models.server.Manager
 *  nro.models.services.ClanService
 *  nro.models.services_dungeon.NgocRongNamecService
 *  nro.models.shop_ky_gui.ConsignShopManager
 *  nro.models.utils.Logger
 *  nro.models.utils.TimeUtil
 */
package nro.models.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import nro.models.Bot.BotManager;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.boss.Boss_Manager.FinalBossManager;
import nro.models.boss.Boss_Manager.GasDestroyManager;
import nro.models.boss.Boss_Manager.OtherBossManager;
import nro.models.boss.Boss_Manager.RedRibbonHQManager;
import nro.models.boss.Boss_Manager.SkillSummonedManager;
import nro.models.boss.Boss_Manager.SnakeWayManager;
import nro.models.boss.Boss_Manager.TreasureUnderSeaManager;
import nro.models.boss.Boss_Manager.YardartManager;
import nro.models.data.LocalManager;
import nro.models.database.HistoryTransactionDAO;
import nro.models.event.EventManager;
import nro.models.interfaces.IKeySessionHandler;
import nro.models.interfaces.IMessageHandler;
import nro.models.interfaces.IMessageSendCollect;
import nro.models.interfaces.ISession;
import nro.models.interfaces.ISessionAcceptHandler;
import nro.models.managers.ShenronEventManager;
import nro.models.managers.SuperRankManager;
import nro.models.matches.giai_dau.DeathOrAliveArenaManager;
import nro.models.matches.giai_dau.The23rdMartialArtCongressManager;
import nro.models.matches.giai_dau.WorldMartialArtsTournamentManager;
import nro.models.minigame.ChonAiDay_Gem;
import nro.models.minigame.ChonAiDay_Gold;
import nro.models.minigame.ConSoMayManGem;
import nro.models.minigame.ConSoMayManGold;
import nro.models.network.MessageSendCollect;
import nro.models.network.MyKeyHandler;
import nro.models.network.MySession;
import nro.models.network.Network;
import nro.models.server.AutoMaintenance;
import nro.models.server.Client;
import nro.models.server.Controller;
import nro.models.server.Maintenance;
import nro.models.server.Manager;
import nro.models.services.ClanService;
import nro.models.services_dungeon.NgocRongNamecService;
import nro.models.shop_ky_gui.ConsignShopManager;
import nro.models.utils.Logger;
import nro.models.utils.TimeUtil;

public class ServerManager {
    public static String timeStart;
    public static final Map<Object, Object> CLIENTS;
    public static String NAME_SERVER;
    public static String DOMAIN;
    public static String NAME;
    public static String IP;
    public static int PORT;
    public static int EVENT_SEVER;
    private static ServerManager instance;
    public static boolean isRunning;
    private ScheduledExecutorService topUpdater;

    public void init() {
        Manager.gI();
        HistoryTransactionDAO.deleteHistory();
    }

    public static ServerManager gI() {
        if (instance == null) {
            instance = new ServerManager();
            instance.init();
        }
        return instance;
    }

    public static void main(String[] stringArray) {
        try {
            timeStart = TimeUtil.getTimeNow((String)"dd/MM/yyyy HH:mm:ss");
            new Thread(() -> {
                try {
                    ServerManager.gI().run();
                }
                catch (Exception exception) {
                    Logger.logException(ServerManager.class, (Exception)exception, (String[])new String[0]);
                }
            }, "ServerMain").start();
            ServerManager.activeCommandLine();
        }
        catch (Exception exception) {
            Logger.logException(ServerManager.class, (Exception)exception, (String[])new String[0]);
        }
    }

    public static void logException(Class<?> clazz, Exception exception) {
        System.err.println("L\u1ed7i t\u1ea1i " + clazz.getSimpleName() + ": " + exception.getMessage());
        exception.printStackTrace();
    }

    public void run() {
        try {
            isRunning = true;
            this.activeServerSocket();
            nro.server.AdminHttpServer.gI().start(14446);
            new Thread((Runnable)NgocRongNamecService.gI(), "Update NRNM").start();
            new Thread((Runnable)SuperRankManager.gI(), "Update Super Rank").start();
            new Thread((Runnable)The23rdMartialArtCongressManager.gI(), "Update DHVT23").start();
            new Thread((Runnable)DeathOrAliveArenaManager.gI(), "Update V\u00f5 \u0110\u00e0i Sinh T\u1eed").start();
            new Thread((Runnable)WorldMartialArtsTournamentManager.gI(), "Update WMAT").start();
            new Thread((Runnable)AutoMaintenance.gI(), "Update B\u1ea3o Tr\u00ec T\u1ef1 \u0110\u1ed9ng").start();
            AutoMaintenance.AutoMaintenance = true;
            AutoMaintenance.gI().start();
            new Thread((Runnable)ShenronEventManager.gI(), "Update Shenron").start();
            BossManager.gI().loadBoss();
            Manager.MAPS.forEach(nro.models.map.Map::initBoss);
            EventManager.gI().init();
            new Thread((Runnable)BossManager.gI(), "Update boss").start();
            new Thread((Runnable)YardartManager.gI(), "Update yardart boss").start();
            new Thread((Runnable)FinalBossManager.gI(), "Update final boss").start();
            new Thread((Runnable)SkillSummonedManager.gI(), "Update skill-summoned boss").start();
            new Thread((Runnable)BrolyManager.gI(), "Update broly boss").start();
            new Thread((Runnable)OtherBossManager.gI(), "Update other boss").start();
            new Thread((Runnable)RedRibbonHQManager.gI(), "Update red ribbon hq boss").start();
            new Thread((Runnable)TreasureUnderSeaManager.gI(), "Update treasure under sea boss").start();
            new Thread((Runnable)SnakeWayManager.gI(), "Update snake way boss").start();
            new Thread((Runnable)GasDestroyManager.gI(), "Update gas destroy boss").start();
            new Thread((Runnable)BotManager.gI(), "Thread Bot Game").start();
            new Thread((Runnable)ChonAiDay_Gem.gI(), "Thread MiniGame").start();
            new Thread((Runnable)ChonAiDay_Gold.gI(), "Thread MiniGame").start();
            new Thread((Runnable)ConSoMayManGold.gI(), "ConSoMayManGoldThread").start();
            new Thread((Runnable)ConSoMayManGem.gI(), "ConSoMayManGemThread").start();
            this.startTopUpdater();
        }
        catch (Exception exception) {
            Logger.logException(this.getClass(), (Exception)exception, (String[])new String[0]);
        }
    }

    private void startTopUpdater() {
        this.topUpdater = Executors.newSingleThreadScheduledExecutor();
        this.topUpdater.scheduleAtFixedRate(() -> {
            if (this.shouldUpdateTop()) {
                this.updateTop();
                Manager.resetTopFlags();
            }
        }, 0L, 3000L, TimeUnit.MILLISECONDS);
    }

    private boolean shouldUpdateTop() {
        return Manager.isTopMaydamChanged || Manager.isTopSukienChanged || Manager.isTopSukien1Changed || Manager.isTopSukien2Changed || Manager.isTopWhisChanged;
    }

    private void stopTopUpdater() {
        if (this.topUpdater != null && !this.topUpdater.isShutdown()) {
            this.topUpdater.shutdown();
            System.out.println("Top updater stopped.");
        }
    }

    private void updateTop() {
        try {
            LocalManager.gI();
            try (Connection connection = LocalManager.getConnection();){
                if (Manager.isTopMaydamChanged) {
                    Manager.Topmaydam = Manager.realTop((String)"SELECT id, point_maydam, total_damage_maydam FROM player ORDER BY point_maydam DESC LIMIT 100", (Connection)connection);
                }
                if (Manager.isTopSukienChanged) {
                    Manager.Topsukien = Manager.realTop((String)"SELECT id, point_sukien FROM player ORDER BY point_sukien DESC LIMIT 100", (Connection)connection);
                }
                if (Manager.isTopSukien1Changed) {
                    Manager.Topsukien1 = Manager.realTop((String)"SELECT id, point_sukien1 FROM player ORDER BY point_sukien1 DESC LIMIT 100", (Connection)connection);
                }
                if (Manager.isTopSukien2Changed) {
                    Manager.Topsukien1 = Manager.realTop((String)"SELECT id, point_sukien1 FROM player ORDER BY point_sukien1 DESC LIMIT 100", (Connection)connection);
                }
                if (Manager.isTopWhisChanged) {
                    Manager.Topwhis = Manager.realTop((String)"SELECT id, thachdauwhis FROM player ORDER BY thachdauwhis DESC LIMIT 100", (Connection)connection);
                }
                Manager.resetTopFlags();
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void activeServerSocket() {
        try {
            Network.gI().init().setAcceptHandler(new ISessionAcceptHandler(){

                public void sessionInit(ISession iSession) {
                    if (!ServerManager.this.canConnectWithIp(iSession.getIP())) {
                        iSession.disconnect();
                        return;
                    }
                    iSession.setMessageHandler((IMessageHandler)Controller.gI()).setSendCollect((IMessageSendCollect)new MessageSendCollect()).setKeyHandler((IKeySessionHandler)new MyKeyHandler()).startCollect();
                }

                public void sessionDisconnect(ISession iSession) {
                    Client.gI().kickSession((MySession)iSession);
                    ServerManager.this.disconnect((MySession)iSession);
                }
            }).setTypeSessioClone(MySession.class).setDoSomeThingWhenClose(() -> {
                Logger.error((String)"SERVER CLOSE\n");
                System.exit(0);
            }).start(PORT);
        }
        catch (Exception exception) {
            Logger.error((String)("L\u1ed7i khi kh\u1edfi \u0111\u1ed9ng m\u00e1y ch\u1ee7: " + exception.getMessage()));
        }
    }

    private boolean canConnectWithIp(String string) {
        Object object = CLIENTS.get(string);
        if (object == null) {
            CLIENTS.put(string, 1);
            return true;
        }
        int n = Integer.parseInt(String.valueOf(object));
        if (n < Manager.MAX_PER_IP) {
            CLIENTS.put(string, ++n);
            return true;
        }
        return false;
    }

    public void disconnect(MySession mySession) {
        Object object = CLIENTS.get(mySession.getIP());
        if (object != null) {
            int n = Integer.parseInt(String.valueOf(object));
            if (--n < 0) {
                n = 0;
            }
            CLIENTS.put(mySession.getIP(), n);
        }
    }

    public void resetNhanQuaHangNgay() {
        String string = "jdbc:mysql://localhost:3306/ngocrong";
        String string2 = "root";
        String string3 = "";
        String string4 = "[1,1,\"1970-01-01T00:00:00\"]";
        try (Connection connection = DriverManager.getConnection(string, string2, string3);){
            String string5 = "UPDATE player SET checkNhanQua = ? WHERE checkNhanQua != ?";
            PreparedStatement preparedStatement = connection.prepareStatement(string5);
            preparedStatement.setString(1, string4);
            preparedStatement.setString(2, string4);
            int n = preparedStatement.executeUpdate();
            Logger.success((String)("\u0110\u00e3 reset nh\u1eadn qu\u00e0 h\u1eb1ng ng\u00e0y cho " + n + " ng\u01b0\u1eddi ch\u01a1i v\u1edbi d\u1eef li\u1ec7u: " + string4));
        }
        catch (SQLException sQLException) {
            System.err.println("L\u1ed7i reset nh\u1eadn qu\u00e0 h\u1eb1ng ng\u00e0y: " + sQLException.getMessage());
        }
    }

    public void close() {
        isRunning = false;
        try {
            ClanService.gI().close();
        }
        catch (Exception exception) {
            Logger.error((String)"L\u1ed7i save clan!\n");
        }
        try {
            ConsignShopManager.gI().save();
        }
        catch (Exception exception) {
            Logger.error((String)"L\u1ed7i save shop k\u00fd g\u1eedi!\n");
        }
        Client.gI().close();
        Logger.success((String)"SUCCESSFULLY MAINTENANCE!\n");
        try {
            Runtime.getRuntime().exec("cmd /c start restart_server.bat");
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
        System.exit(0);
    }

    private static void activeCommandLine() {
        Scanner scanner = new Scanner(System.in);
        block14: while (true) {
            String string;
            switch (string = scanner.nextLine()) {
                case "bt": {
                    Maintenance.gI().startSeconds(5);
                    continue block14;
                }
                case "bat": {
                    AutoMaintenance.AutoMaintenance = true;
                    System.out.println("\u0110\u00e3 b\u1eadt ch\u1ebf \u0111\u1ed9 b\u1ea3o tr\u00ec t\u1ef1 \u0111\u1ed9ng.");
                    continue block14;
                }
                case "tat": {
                    AutoMaintenance.AutoMaintenance = false;
                    System.out.println("\u0110\u00e3 t\u1eaft ch\u1ebf \u0111\u1ed9 b\u1ea3o tr\u00ec t\u1ef1 \u0111\u1ed9ng.");
                    continue block14;
                }
                case "run": {
                    try {
                        ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", "run.bat");
                        processBuilder.inheritIO();
                        processBuilder.start();
                        System.out.println("\u0110\u00e3 ch\u1ea1y run.bat");
                    }
                    catch (IOException iOException) {
                        System.out.println("L\u1ed7i khi ch\u1ea1y run.bat: " + iOException.getMessage());
                    }
                    continue block14;
                }
            }
            System.out.println("L\u1ec7nh kh\u00f4ng h\u1ee3p l\u1ec7.");
        }
    }

    static {
        CLIENTS = new HashMap<Object, Object>();
        NAME_SERVER = "Ng\u1ecdc R\u1ed3ng Onlime";
        DOMAIN = "Server 1";
        NAME = "Ng\u1ecdc R\u1ed3ng Online";
        IP = "36.50.135.149";
        PORT = 14445;
        EVENT_SEVER = 0;
    }
}
