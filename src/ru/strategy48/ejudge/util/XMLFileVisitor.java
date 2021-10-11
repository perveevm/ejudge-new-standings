package ru.strategy48.ejudge.util;

import ru.strategy48.ejudge.standings.StandingsTableConfig;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class XMLFileVisitor implements FileVisitor<Path> {
    private List<StandingsTableConfig> foundConfigFiles = new ArrayList<>();

    public List<StandingsTableConfig> getFoundConfigFiles() {
        return foundConfigFiles;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (!file.getFileName().endsWith(".xml")) {
            return FileVisitResult.CONTINUE;
        }

        try {
            foundConfigFiles.add(XMLUtils.parseConfigFile(file.toFile()));
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
