package lneux.voidRanks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class VoidRanks extends JavaPlugin {

    private LuckPerms luckPerms;
    private final Map<String, Integer> rankTimes = new LinkedHashMap<>();
    private String promotedMsg, promotedFinalMsg;
    private int gracePeriod;

    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        setupDataFile();

        luckPerms = LuckPermsProvider.get();

        new PlaytimeTask().runTaskTimer(this, 20 * 60, 20 * 60); // every minute
        getLogger().info("VoidRanks habilitado!");
    }

    @Override
    public void onDisable() {
        saveData();
        getLogger().info("VoidRanks desabilitado e dados salvos.");
    }

    private void loadConfig() {
        rankTimes.clear();
        List<Map<?, ?>> ranks = getConfig().getMapList("ranks");
        for (Map<?, ?> rank : ranks) {
            String name = (String) rank.get("name");
            int time = (int) rank.get("time");
            rankTimes.put(name, time);
        }
        promotedMsg = translate(getConfig().getString("messages.promoted", "&aParabéns! Você foi promovido para &e{rank}&a!"));
        promotedFinalMsg = translate(getConfig().getString("messages.promoted_final", "&6&l{player} alcançou o rank máximo: &e{rank}&6&l!"));
        gracePeriod = getConfig().getInt("settings.grace_period", 5);
    }

    private String translate(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private void setupDataFile() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class PlaytimeTask extends BukkitRunnable {
        @Override
        public void run() {
            long now = System.currentTimeMillis() / 1000;
            Set<String> seenPlayers = new HashSet<>();

            // Track online playtime
            for (Player player : Bukkit.getOnlinePlayers()) {
                String uuid = player.getUniqueId().toString();
                seenPlayers.add(uuid);

                int playtime = dataConfig.getInt(uuid + ".playtime", 0);
                playtime++;
                dataConfig.set(uuid + ".playtime", playtime);

                dataConfig.set(uuid + ".last_seen", now);
                dataConfig.set(uuid + ".offline_time", 0);

                checkPromotion(player, playtime);
            }

            // Track offline time
            for (String uuid : dataConfig.getKeys(false)) {
                if (seenPlayers.contains(uuid)) continue;

                int offline = dataConfig.getInt(uuid + ".offline_time", 0);
                offline++;
                dataConfig.set(uuid + ".offline_time", offline);

                int playtime = dataConfig.getInt(uuid + ".playtime", 0);
                checkDemotion(uuid, playtime, offline);
            }

            saveData();
        }
    }

    private void checkPromotion(Player player, int playtimeMinutes) {
        String currentRank = getCurrentRank(player);
        String nextRank = getNextRank(currentRank);

        if (nextRank == null) return; // Already max rank

        int requiredMinutes = rankTimes.get(nextRank);
        if (playtimeMinutes >= requiredMinutes + gracePeriod) {
            promotePlayer(player, nextRank);
        }
    }

    private void checkDemotion(String uuid, int playtime, int offlineMinutes) {
        String currentRank = getCurrentRank(UUID.fromString(uuid));
        String prevRank = getPreviousRank(currentRank);
        if (prevRank == null) return;

        int requiredMinutes = rankTimes.get(currentRank);
        if (offlineMinutes >= requiredMinutes + gracePeriod) {
            demotePlayer(uuid, prevRank);
        }
    }

    private String getCurrentRank(Player player) {
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        for (String rank : rankTimes.keySet()) {
            if (user.getPrimaryGroup().equalsIgnoreCase(rank.toLowerCase())) {
                return rank;
            }
        }
        return rankTimes.keySet().iterator().next();
    }

    private String getCurrentRank(UUID uuid) {
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return rankTimes.keySet().iterator().next();
        for (String rank : rankTimes.keySet()) {
            if (user.getPrimaryGroup().equalsIgnoreCase(rank.toLowerCase())) {
                return rank;
            }
        }
        return rankTimes.keySet().iterator().next();
    }

    private String getNextRank(String currentRank) {
        List<String> ranks = new ArrayList<>(rankTimes.keySet());
        int index = ranks.indexOf(currentRank);
        if (index == -1 || index + 1 >= ranks.size()) return null;
        return ranks.get(index + 1);
    }

    private String getPreviousRank(String currentRank) {
        List<String> ranks = new ArrayList<>(rankTimes.keySet());
        int index = ranks.indexOf(currentRank);
        if (index <= 0) return null;
        return ranks.get(index - 1);
    }

    private void promotePlayer(Player player, String nextRank) {
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        user.data().add(Node.builder("group." + nextRank.toLowerCase()).build());
        user.setPrimaryGroup(nextRank.toLowerCase());
        luckPerms.getUserManager().saveUser(user);

        List<String> ranks = new ArrayList<>(rankTimes.keySet());
        boolean isFinalRank = ranks.indexOf(nextRank) == ranks.size() - 1;

        if (isFinalRank) {
            String msg = promotedFinalMsg
                    .replace("{player}", player.getName())
                    .replace("{rank}", nextRank);
            Bukkit.broadcastMessage(msg);
        } else {
            String msg = promotedMsg.replace("{rank}", nextRank);
            player.sendMessage(msg);
        }
    }

    private void demotePlayer(String uuid, String prevRank) {
        User user = luckPerms.getUserManager().getUser(UUID.fromString(uuid));
        if (user == null) return;

        user.data().add(Node.builder("group." + prevRank.toLowerCase()).build());
        user.setPrimaryGroup(prevRank.toLowerCase());
        luckPerms.getUserManager().saveUser(user);
    }

    // 🔹 Commands
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ptime")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Somente jogadores podem usar este comando.");
                return true;
            }
            Player player = (Player) sender;
            String uuid = player.getUniqueId().toString();
            int playtime = dataConfig.getInt(uuid + ".playtime", 0);
            String currentRank = getCurrentRank(player);
            String nextRank = getNextRank(currentRank);

            player.sendMessage(ChatColor.YELLOW + "Seu tempo de jogo: " + playtime + " minutos.");
            player.sendMessage(ChatColor.YELLOW + "Seu rank atual: " + currentRank);

            if (nextRank != null) {
                int nextTime = rankTimes.get(nextRank);
                int remaining = Math.max(0, nextTime - playtime);
                player.sendMessage(ChatColor.YELLOW + "Próximo rank: " + nextRank + " em " + remaining + " minutos.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Você está no rank máximo!");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("vranks")) {
            if (!sender.hasPermission("voidranks.admin")) {
                sender.sendMessage(ChatColor.RED + "Sem permissão.");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage(ChatColor.YELLOW + "/vranks reload - Recarrega a config");
                sender.sendMessage(ChatColor.YELLOW + "/vranks check <jogador> - Verifica o tempo de jogo");
                sender.sendMessage(ChatColor.YELLOW + "/vranks reset <jogador> - Reseta o tempo de jogo");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                loadConfig();
                sender.sendMessage(ChatColor.GREEN + "Configuração recarregada.");
                return true;
            }

            if (args[0].equalsIgnoreCase("check") && args.length == 2) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String uuid = target.getUniqueId().toString();
                int playtime = dataConfig.getInt(uuid + ".playtime", 0);
                sender.sendMessage(ChatColor.YELLOW + target.getName() + " tem " + playtime + " minutos de jogo.");
                return true;
            }

            if (args[0].equalsIgnoreCase("reset") && args.length == 2) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String uuid = target.getUniqueId().toString();
                dataConfig.set(uuid + ".playtime", 0);
                saveData();
                sender.sendMessage(ChatColor.GREEN + "Tempo de jogo de " + target.getName() + " resetado.");
                return true;
            }
        }
        return false;
    }
}
