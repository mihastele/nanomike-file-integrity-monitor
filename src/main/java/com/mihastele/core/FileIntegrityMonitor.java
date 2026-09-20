package com.mihastele.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public class FileIntegrityMonitor {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: java FileIntegrityMonitor <init|check> <directory> <baseline-file>");
            System.exit(1);
        }
        String command = args[0];
        Path root = Paths.get(args[1]).toAbsolutePath().normalize();
        Path baseline = Paths.get(args[2]).toAbsolutePath().normalize();

        if (!Files.isDirectory(root)) {
            System.err.println("Root should be a directory " + root);
            System.exit(2);
        }
        switch (command) {
            case "init" -> init(root, baseline);
            case "check" -> check(root, baseline);
            default -> {
                System.err.println("Unknown command: " + command);
                System.exit(1);
            }
        }
    }

    public static void init(Path root, Path baseline) throws IOException, NoSuchAlgorithmException {
        Map<String, String> hashes = scan(root, baseline);
        List<String> lines = hashes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getValue() + '\t' + e.getKey())
                .toList();
        Files.write(baseline, lines);
        System.out.println("Baseline written: " + baseline);
        System.out.println("Files recorded: " + hashes.size());
    }

    static void check(Path root, Path baseline) throws IOException, NoSuchAlgorithmException {
        if (!Files.isRegularFile(baseline)) {
            System.err.println("Baseline file not found " + baseline);
            System.exit(2);
        }

        Map<String, String> oldHashes = loadBaseline(baseline);
        Map<String, String> newHashes = scan(root, baseline);

        int added = 0, modified = 0, deleted = 0;
        for (String path : newHashes.keySet()) {
            if (!oldHashes.containsKey(path)) {
                System.out.println("ADDED " + path);
                added++;
            }
            if (!oldHashes.get(path).equals(newHashes.get(path))) {
                System.out.println("MODIFIED " + path);
                modified++;
            }

        }

        for (String path : oldHashes.keySet()) {
            if (!newHashes.containsKey(path)) {
                System.out.println("DELETED " + path);
                deleted++;
            }
        }

        System.out.printf("%nSummary: %d added, %d modified, %d deleted%n", added, modified, deleted);
    }

    static Map<String, String> scan(Path root, Path baseline) throws IOException, NoSuchAlgorithmException {
        Map<String, String> result = new TreeMap<>();
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toAbsolutePath().normalize().equals(baseline))
                    .toList();

            for (Path file : files) {
                String relative = root.relativize(file).toString().replace(File.separatorChar, '/');
                result.put(relative, sha256(file));
            }
        }
        return result;
    }

    static Map<String, String> loadBaseline(Path baseline) throws IOException {
        Map<String, String> map = new TreeMap<>();
        for (String line : Files.readAllLines(baseline)) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\t", 2);
            if (parts.length == 2) {
                map.put(parts[1], parts[0]);
            }
        }
        return map;
    }

    static String sha256(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(in)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = bis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b & 0xff));
        }
        return hex.toString();
    }
}