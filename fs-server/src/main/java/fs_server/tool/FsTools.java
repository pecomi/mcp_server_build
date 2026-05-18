package fs_server.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class FsTools {

    private static final Logger log = LoggerFactory.getLogger(FsTools.class);

    public String readFile(String path) throws Exception {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path는 필수입니다.");
        }
        log.info("read_file path={}", path);
        return Files.readString(Paths.get(path));
    }
}
