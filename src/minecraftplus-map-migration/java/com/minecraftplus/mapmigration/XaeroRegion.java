package com.minecraftplus.mapmigration;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.*;

/**
 * Xaero's World Map region file (x_z.zip with one entry "region.xaero"), save format 6.8 (World Map 1.47, the
 * version in the pack). Mirrors MapSaveLoad.saveRegion/loadRegion/savePixel/loadPixel/saveOverlay/loadOverlay.
 * A region is 8x8 tile chunks of 4x4 tiles; a tile is one Minecraft chunk (16x16 pixels). Block states and
 * biomes are stored in per-file palettes, so tiles can only be moved between files by decoding and re-encoding.
 * Palette references are resolved to raw NBT bytes / biome ids on read and rebuilt on write.
 */
final class XaeroRegion {
    static final int VERSION = 393224;   // major 6, minor 8
    static final int STATE = 1, OVERLAYS = 2, HEIGHT_BYTE = 64, BIOME = 1048576, NEW_STATE = 2097152,
            NEW_BIOME = 4194304, BIOME_AS_INT = 8388608, TOP_HEIGHT = 16777216;
    static final int O_STATE = 1, O_EXTRA_INT = 4, O_NEW_STATE = 1024;

    record Overlay(int params, byte[] state, int extra) {}
    record Pixel(int params, byte[] state, int height, int topHeight, Overlay[] overlays, String biome) {}
    record Tile(Pixel[] pixels, int interpretation, int caveStart, int caveDepth) {}

    /** chunk coordinate byte (o << 4 | p) -> 16 tiles (index i * 4 + j), null = tile not explored */
    final TreeMap<Integer, Tile[]> chunks = new TreeMap<>();

    static XaeroRegion read(File zip) throws IOException {
        try (ZipInputStream z = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)))) {
            ZipEntry e = z.getNextEntry();
            if (e == null || !e.getName().equals("region.xaero")) throw new IOException("no region.xaero in " + zip);
            return read(new DataInputStream(new BufferedInputStream(z)));
        }
    }

    static XaeroRegion read(DataInputStream in) throws IOException {
        if (in.read() != 255 || in.readInt() != VERSION) throw new IOException("unsupported Xaero region save version");
        XaeroRegion r = new XaeroRegion();
        List<byte[]> states = new ArrayList<>();
        List<String> biomes = new ArrayList<>();
        int coords;
        while ((coords = in.read()) != -1) {
            if (r.chunks.containsKey(coords)) throw new IOException("tile chunk saved twice");
            Tile[] tiles = new Tile[16];
            for (int t = 0; t < 16; t++) {
                int first = in.readInt();
                if (first == -1) continue;
                Pixel[] px = new Pixel[256];
                for (int k = 0; k < 256; k++) px[k] = readPixel(k == 0 ? first : in.readInt(), in, states, biomes);
                tiles[t] = new Tile(px, in.read(), in.readInt(), in.read());
            }
            r.chunks.put(coords, tiles);
        }
        return r;
    }

    static Pixel readPixel(int params, DataInputStream in, List<byte[]> states, List<String> biomes) throws IOException {
        byte[] state = null;
        if ((params & STATE) != 0) {
            if ((params & NEW_STATE) != 0) states.add(state = readNbt(in));
            else state = states.get(in.readInt());
        }
        int height = (params & HEIGHT_BYTE) != 0 ? in.read() : -1;
        int top = (params & TOP_HEIGHT) != 0 ? in.read() : -1;
        Overlay[] overlays = null;
        if ((params & OVERLAYS) != 0) {
            overlays = new Overlay[in.read()];
            for (int i = 0; i < overlays.length; i++) {
                int op = in.readInt();
                byte[] os = null;
                if ((op & O_STATE) != 0) {
                    if ((op & O_NEW_STATE) != 0) states.add(os = readNbt(in));
                    else os = states.get(in.readInt());
                }
                overlays[i] = new Overlay(op, os, (op & O_EXTRA_INT) != 0 ? in.readInt() : 0);
            }
        }
        String biome = null;
        if ((params & BIOME) != 0) {
            if ((params & NEW_BIOME) != 0) {
                if ((params & BIOME_AS_INT) != 0) throw new IOException("old numeric biome ids not supported");
                biomes.add(biome = in.readUTF());
            } else biome = biomes.get(in.readInt());
        }
        return new Pixel(params, state, height, top, overlays, biome);
    }

    void write(File zip) throws IOException {
        try (ZipOutputStream z = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zip)))) {
            z.putNextEntry(new ZipEntry("region.xaero"));
            DataOutputStream out = new DataOutputStream(z);
            write(out);
            out.flush();
            z.closeEntry();
        }
    }

    void write(DataOutputStream out) throws IOException {
        out.write(255);
        out.writeInt(VERSION);
        Map<ByteBuffer, Integer> states = new HashMap<>();
        Map<String, Integer> biomes = new HashMap<>();
        for (int o = 0; o < 8; o++) for (int p = 0; p < 8; p++) {   // same order as Xaero's saveRegion
            Tile[] tiles = chunks.get(o << 4 | p);
            if (tiles == null) continue;
            out.write(o << 4 | p);
            for (Tile t : tiles) {
                if (t == null) { out.writeInt(-1); continue; }
                for (Pixel px : t.pixels) writePixel(px, out, states, biomes);
                out.write(t.interpretation);
                out.writeInt(t.caveStart);
                out.write(t.caveDepth);
            }
        }
    }

    static void writePixel(Pixel px, DataOutputStream out, Map<ByteBuffer, Integer> states, Map<String, Integer> biomes) throws IOException {
        int params = px.params & ~(NEW_STATE | NEW_BIOME | BIOME_AS_INT);
        Integer si = px.state == null ? null : states.get(ByteBuffer.wrap(px.state));
        if (px.state != null && si == null) params |= NEW_STATE;
        Integer bi = px.biome == null ? null : biomes.get(px.biome);
        if (px.biome != null && bi == null) params |= NEW_BIOME;
        out.writeInt(params);
        if (px.state != null) writeState(px.state, si, out, states);
        if ((params & HEIGHT_BYTE) != 0) out.write(px.height);
        if ((params & TOP_HEIGHT) != 0) out.write(px.topHeight);
        if (px.overlays != null) {
            out.write(px.overlays.length);
            for (Overlay ov : px.overlays) {
                int op = ov.params & ~O_NEW_STATE;
                Integer oi = ov.state == null ? null : states.get(ByteBuffer.wrap(ov.state));
                if (ov.state != null && oi == null) op |= O_NEW_STATE;
                out.writeInt(op);
                if (ov.state != null) writeState(ov.state, oi, out, states);
                if ((op & O_EXTRA_INT) != 0) out.writeInt(ov.extra);
            }
        }
        if (px.biome != null) {
            if (bi != null) out.writeInt(bi);
            else { out.writeUTF(px.biome); biomes.put(px.biome, biomes.size()); }
        }
    }

    static void writeState(byte[] nbt, Integer index, DataOutputStream out, Map<ByteBuffer, Integer> states) throws IOException {
        if (index != null) out.writeInt(index);
        else { out.write(nbt); states.put(ByteBuffer.wrap(nbt), states.size()); }
    }

    /** Copies one complete NBT root tag (type byte, name, payload), as NbtIo.write writes it, as raw bytes. */
    static byte[] readNbt(DataInputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream copy = new DataOutputStream(buf);
        int type = in.readUnsignedByte();
        copy.write(type);
        if (type != 0) {
            copy.writeUTF(in.readUTF());
            copyPayload(type, in, copy, 0);
        }
        return buf.toByteArray();
    }

    static void copyPayload(int type, DataInputStream in, DataOutputStream out, int depth) throws IOException {
        if (depth > 512) throw new IOException("NBT too deep");
        switch (type) {
            case 1 -> out.write(in.readUnsignedByte());
            case 2 -> out.writeShort(in.readShort());
            case 3, 5 -> out.writeInt(in.readInt());
            case 4, 6 -> out.writeLong(in.readLong());
            case 7 -> { int n = in.readInt(); out.writeInt(n); copyBytes(in, out, n); }
            case 8 -> out.writeUTF(in.readUTF());
            case 9 -> {
                int et = in.readUnsignedByte(), n = in.readInt();
                out.write(et); out.writeInt(n);
                for (int i = 0; i < n; i++) copyPayload(et, in, out, depth + 1);
            }
            case 10 -> {
                int t;
                while ((t = in.readUnsignedByte()) != 0) {
                    out.write(t);
                    out.writeUTF(in.readUTF());
                    copyPayload(t, in, out, depth + 1);
                }
                out.write(0);
            }
            case 11 -> { int n = in.readInt(); out.writeInt(n); copyBytes(in, out, n * 4L); }
            case 12 -> { int n = in.readInt(); out.writeInt(n); copyBytes(in, out, n * 8L); }
            default -> throw new IOException("bad NBT tag " + type);
        }
    }

    static void copyBytes(DataInputStream in, DataOutputStream out, long n) throws IOException {
        if (n < 0 || n > (1 << 26)) throw new IOException("bad NBT array length");
        byte[] b = new byte[(int) n];
        in.readFully(b);
        out.write(b);
    }

    /** Every explored tile of both; where both have a tile, the newer file's (preferred) tile wins. */
    static XaeroRegion merge(XaeroRegion older, XaeroRegion preferred) {
        XaeroRegion r = new XaeroRegion();
        for (var e : older.chunks.entrySet()) r.chunks.put(e.getKey(), e.getValue().clone());
        for (var e : preferred.chunks.entrySet()) {
            Tile[] into = r.chunks.computeIfAbsent(e.getKey(), k -> new Tile[16]);
            for (int t = 0; t < 16; t++) if (e.getValue()[t] != null) into[t] = e.getValue()[t];
        }
        return r;
    }

    int tileCount() {
        int n = 0;
        for (Tile[] ts : chunks.values()) for (Tile t : ts) if (t != null) n++;
        return n;
    }
}
