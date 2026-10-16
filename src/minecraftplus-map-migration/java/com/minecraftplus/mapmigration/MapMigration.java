package com.minecraftplus.mapmigration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Xaero's minimap and world map keep their data in folders named after the server address
 * (xaero/minimap/Multiplayer_<host>, xaero/world-map/Multiplayer_<host>). When the address changes, this merges the
 * old folder into the new one once per rule, before any world is joined. Rules: config/minecraftplus-address-migration.txt,
 * lines "old.host -> new.host". The old folders are left untouched.
 *   - files only in the old folder are copied
 *   - waypoint files (dim%*\/mw$*.txt): waypoints from the old file that the new file lacks are appended
 *   - world map regions (*.zip) in both: merged chunk by chunk (XaeroRegion): every explored chunk of both is kept,
 *     where both have a chunk the new address's (newer) one wins; that map's texture cache is cleared (rebuilt)
 *   - everything else (configs, locks, caches) in both: the new folder's copy is kept
 */
@Mod("minecraftplus_mapmigration")
public class MapMigration {
    public MapMigration() {
        try {
            run(FMLPaths.GAMEDIR.get());
        } catch (Throwable t) {
            System.err.println("[MinecraftPlus map migration] failed, map data unchanged where not yet copied: " + t);
        }
    }

    static void run(Path game) throws IOException {
        Path rules = game.resolve("config/minecraftplus-address-migration.txt");
        Path xaero = game.resolve("xaero");
        if (!Files.isRegularFile(rules) || !Files.isDirectory(xaero)) return;
        // Friends only: installs that play from the server's home network (LAN address) are left alone (Autumn).
        if (Files.isDirectory(xaero.resolve("minimap/Multiplayer_192.168.178.132"))
                || Files.isDirectory(xaero.resolve("world-map/Multiplayer_192.168.178.132"))) return;
        Path done = xaero.resolve("minecraftplus-migrated.txt");
        Set<String> doneRules = new HashSet<>(Files.isRegularFile(done) ? Files.readAllLines(done) : List.of());
        for (String raw : Files.readAllLines(rules, StandardCharsets.UTF_8)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#") || !line.contains("->")) continue;
            String from = line.substring(0, line.indexOf("->")).strip(), to = line.substring(line.indexOf("->") + 2).strip();
            String key = "v2 " + from + " -> " + to;   // v2: chunk-level region merge (v1 in pack 0.4.4 replaced whole regions)
            if (from.isEmpty() || to.isEmpty() || from.equals(to) || doneRules.contains(key)) continue;
            String stats = "";
            for (String top : new String[] {"minimap", "world-map"}) {
                Path src = xaero.resolve(top).resolve("Multiplayer_" + from), dst = xaero.resolve(top).resolve("Multiplayer_" + to);
                if (Files.isDirectory(src)) stats += " " + top + ": " + merge(src, dst);
            }
            Files.writeString(done, key + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("[MinecraftPlus map migration] " + key + stats);
        }
    }

    static String merge(Path src, Path dst) throws IOException {
        int copied = 0, waypoints = 0, regions = 0, failed = 0;
        Set<Path> staleMaps = new HashSet<>();
        List<Path> files;
        try (Stream<Path> s = Files.walk(src)) { files = s.filter(Files::isRegularFile).toList(); }
        for (Path f : files) {
            String name = f.getFileName().toString();
            if (name.equals(".lock")) continue;
            Path rel = src.relativize(f), d = dst.resolve(rel.toString());
            if (!Files.exists(d)) {
                Files.createDirectories(d.getParent());
                Files.copy(f, d, StandardCopyOption.COPY_ATTRIBUTES);
                copied++;
                if (name.endsWith(".zip")) staleMaps.add(d.getParent());
            } else if (name.endsWith(".txt") && name.startsWith("mw$") && rel.getNameCount() >= 2
                    && rel.getName(rel.getNameCount() - 2).toString().startsWith("dim%")) {
                waypoints += mergeWaypoints(f, d);
            } else if (name.endsWith(".zip")) {
                Path tmp = d.resolveSibling(name + ".mcplus-tmp");
                try {
                    XaeroRegion.merge(XaeroRegion.read(f.toFile()), XaeroRegion.read(d.toFile())).write(tmp.toFile());
                    XaeroRegion.read(tmp.toFile());   // must read back before it replaces the new region
                    Files.move(tmp, d, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    regions++;
                    staleMaps.add(d.getParent());
                } catch (Exception e) {   // unknown format or damaged file: keep the new address's region untouched
                    Files.deleteIfExists(tmp);
                    failed++;
                }
            }
        }
        for (Path map : staleMaps) {   // texture caches are rebuilt by the world map from the region files
            try (Stream<Path> s = Files.list(map)) {
                for (Path c : s.filter(p -> Files.isDirectory(p) && p.getFileName().toString().startsWith("cache")).toList()) deleteTree(c);
            }
        }
        return copied + " files copied, " + waypoints + " waypoints added, " + regions + " regions merged, " + failed + " regions skipped";
    }

    /** Appends old waypoints missing from the new file (same name and x/y/z = same waypoint); merges the "sets:" line. */
    static int mergeWaypoints(Path oldFile, Path newFile) throws IOException {
        List<String> out = new ArrayList<>(Files.readAllLines(newFile, StandardCharsets.UTF_8));
        Set<String> have = new HashSet<>();
        for (String l : out) if (l.startsWith("waypoint:")) have.add(wpKey(l));
        int added = 0;
        for (String l : Files.readAllLines(oldFile, StandardCharsets.UTF_8)) {
            if (l.startsWith("waypoint:") && have.add(wpKey(l))) { out.add(l); added++; }
            else if (l.startsWith("sets:")) {
                int i = -1;
                for (int j = 0; j < out.size(); j++) if (out.get(j).startsWith("sets:")) i = j;
                if (i < 0) { out.add(0, l); continue; }
                LinkedHashSet<String> sets = new LinkedHashSet<>(Arrays.asList(out.get(i).split(":")));
                sets.addAll(Arrays.asList(l.split(":")));
                out.set(i, String.join(":", sets));
            }
        }
        if (added > 0 || !out.equals(Files.readAllLines(newFile, StandardCharsets.UTF_8))) {
            Path tmp = newFile.resolveSibling(newFile.getFileName() + ".mcplus-tmp");
            Files.write(tmp, out, StandardCharsets.UTF_8);
            Files.move(tmp, newFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        return added;
    }

    static String wpKey(String l) {
        String[] p = l.split(":", -1);
        return p.length > 5 ? p[1] + ":" + p[3] + ":" + p[4] + ":" + p[5] : l;
    }

    static void deleteTree(Path p) throws IOException {
        try (Stream<Path> s = Files.walk(p)) {
            for (Path q : s.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(q);
        }
    }
}
