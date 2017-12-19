package org.helioviewer.jhv.base;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Scanner;

public class FileUtils {

    public static InputStream getResourceInputStream(String resourcePath) {
        return FileUtils.class.getResourceAsStream(resourcePath);
    }

    public static String convertStreamToString(InputStream is) {
        try (Scanner s = new Scanner(is, StandardCharsets.UTF_8.name())) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    private static final SimpleFileVisitor<Path> nukeVisitor = new SimpleFileVisitor<Path>() {

        private FileVisitResult delete(Path file) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            return delete(file);
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return delete(file);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return delete(dir);
        }

    };

    public static void deleteDir(File dir) throws IOException {
        Files.walkFileTree(dir.toPath(), nukeVisitor);
    }

    public static String tempDir(File parent, String name) throws IOException {
        String suffix = ".lock";
        // delete all directories without a lock file
        FileFilter filter = p -> p.getName().startsWith(name) && !p.getName().endsWith(suffix);
        File[] dirs = parent.listFiles(filter);
        if (dirs == null)
            throw new IOException("I/O error or not a directory: " + parent);

        for (File dir : dirs) {
            if (new File(dir + suffix).exists())
                continue;
            deleteDir(dir);
        }

        String tempDir = Files.createTempDirectory(parent.toPath(), name).toString();
        File lock = new File(tempDir + suffix);
        lock.createNewFile();
        lock.deleteOnExit();

        return tempDir;
    }

}
