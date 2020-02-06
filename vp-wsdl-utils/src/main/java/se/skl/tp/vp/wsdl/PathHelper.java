package se.skl.tp.vp.wsdl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.util.ResourceUtils;

public class PathHelper {

  private PathHelper() {
  }

  public static final String PATH_PREFIX = "classpath:";


  public static Path getPath(String filePath) throws FileNotFoundException, URISyntaxException {
    URL url = ResourceUtils.getURL(filePath);
    return Paths.get(url.toURI());
  }

  public static List<File> findFoldersInDirectory(String directoryPath, String folderNameRegEx)
      throws IOException {
    List<File> res = new ArrayList<>();

    try (Stream<Path> walk = Files.walk(Paths.get(directoryPath))) {
      walk.filter(p -> p.toFile().isDirectory() && p.toFile().getName().matches(folderNameRegEx))
          .forEach(p -> res.add(p.toFile()));
    }

    return res;
  }

  public static List<File> findFilesInDirectory(String directoryPath, String fileNameRegEx)
      throws IOException {
    List<File> res = new ArrayList<>();

    try (Stream<Path> walk = Files.walk(Paths.get(directoryPath))) {
      walk.filter(p -> !p.toFile().isDirectory() && p.toFile().getName().matches(fileNameRegEx))
          .forEach(p -> res.add(p.toFile()));
    }
    return res;
  }

  public static String subtractDirectoryFromPath(File dir, File path) {
    return dir.toURI().relativize(path.toURI()).getPath();
  }

  public static String subtractDirectoryFromPath(String dir, File path) {
    return subtractDirectoryFromPath(new File(dir), path);
  }

  /**
   * Delete a non empty dir and all files within (From internet)
   */
  public static void deleteDirectory(Path path) throws IOException {
    FileVisitor visitor =
        new SimpleFileVisitor<Path>() {

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc != null) {
              throw exc;
            }
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
          }
        };

    Files.walkFileTree(path, visitor);
  }
}
