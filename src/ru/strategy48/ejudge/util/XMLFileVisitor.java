package ru.strategy48.ejudge.util;

import ru.strategy48.ejudge.standings.StandingsTableConfig;
import ru.strategy48.ejudge.standings.StandingsTableEntity;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class XMLFileVisitor implements FileVisitor<Path> {
    private final List<StandingsTableEntity> foundConfigFiles = new ArrayList<>();
    private final Path startPath;

    public XMLFileVisitor(final Path startPath) {
        this.startPath = startPath;
    }

    public List<StandingsTableEntity> getFoundConfigFiles() {
        return foundConfigFiles;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        StandingsTableConfig config;
        try {
            config = XMLUtils.parseConfigFile(file.toFile());
            Path curPath = startPath;
            StringBuilder realPath = new StringBuilder();
            while (!curPath.getParent().equals(startPath)) {
                realPath.insert(0, curPath.getFileName().toString());
                curPath = curPath.getParent();
            }
            foundConfigFiles.add(new StandingsTableEntity(config, realPath.toString()));
        } catch (Exception ignored) {
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }
}
