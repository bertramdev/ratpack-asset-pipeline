package asset.pipeline.ratpack

import asset.pipeline.*
import asset.pipeline.fs.*
import ratpack.file.FileSystemBinding
import ratpack.server.Service;
import ratpack.server.StartEvent;
import ratpack.server.StopEvent;
import java.nio.file.Path;

/**
* This service provides a startup configuration binding to tell the AP Config where to look for files
* @author David Estes
*/
public class AssetPipelineService implements Service {
    public List<String> events = new LinkedList<>();

    public void onStart(StartEvent startEvent) {
        FileSystemBinding fileSystemBinding = startEvent.registry.get(FileSystemBinding.class);
        Path path = fileSystemBinding.getFile();
        if(path.fileSystem.class.name == "com.sun.nio.zipfs.ZipFileSystem") {
            // We are in production mode
            AssetPipelineConfigHolder.config.put("precompiled",true);
            Path manifest = fileSystemBinding.file("assets/manifest.properties")
            if(manifest != null) {
                Properties = manifestProps = new Properties();
                InputStream manIs = File.newInputStream(manifest);
                manifestProps.load(manIs);
                manIs.close()
                AssetPipelineConfigHolder.manifest = manifestProps;
            }

        } else {
            AssetPipelineConfigHolder.registerResolver(new FileSystemAssetResolver("application",path.toString() + "/assets"));
        }
    }
}
