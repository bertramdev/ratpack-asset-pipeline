package asset.pipeline.ratpack;

import asset.pipeline.*;
import asset.pipeline.fs.*;
import ratpack.file.FileSystemBinding;
import ratpack.server.Service;
import ratpack.server.StartEvent;
import ratpack.server.StopEvent;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.InputStream;
import java.util.Properties;
import java.util.List;
import java.util.LinkedList;
import java.io.IOException;

/**
* This service provides a startup configuration binding to tell the AP Config where to look for files
* @author David Estes
*/
public class AssetPipelineService implements Service {
    public List<String> events = new LinkedList<>();

    public void onStart(StartEvent startEvent) throws IOException {
        FileSystemBinding fileSystemBinding = startEvent.getRegistry().get(FileSystemBinding.class);
        Path path = fileSystemBinding.getFile();
        if(path.getFileSystem().getClass().getName() == "com.sun.nio.zipfs.ZipFileSystem") {
            // We are in production mode
            AssetPipelineConfigHolder.config.put("precompiled",true);
            Path manifest = fileSystemBinding.file("assets/manifest.properties");
            if(manifest != null) {
                Properties manifestProps = new Properties();
                InputStream manIs = Files.newInputStream(manifest);
                manifestProps.load(manIs);
                manIs.close();
                AssetPipelineConfigHolder.manifest = manifestProps;
            }

        } else {
            AssetPipelineConfigHolder.registerResolver(new FileSystemAssetResolver("application",path.toString() + "/assets"));
        }
    }
}
