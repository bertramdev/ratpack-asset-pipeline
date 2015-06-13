package asset.pipeline.ratpack

import asset.pipeline.*
import asset.pipeline.fs.*
import ratpack.file.FileSystemBinding
import ratpack.server.Service;
import ratpack.server.StartEvent;
import ratpack.server.StopEvent;
import java.nio.file.Path;

  public class AssetPipelineService implements Service {
    public List<String> events = new LinkedList<>();
    public void onStart(StartEvent startEvent) {
    	println "AP Init"
    	FileSystemBinding fileSystemBinding = startEvent.registry.get(FileSystemBinding.class)
    	Path path = fileSystemBinding.getFile();
    	if(path.fileSystem.class.name == "com.sun.nio.zipfs.ZipFileSystem") {
    		// We are in production mode
    		AssetPipelineConfigHolder.config.precompiled = true
    	} else {
    		println "Initializing"
	        AssetPipelineConfigHolder.registerResolver(new FileSystemAssetResolver('application',path.toString() + "/assets"))
    	}
    }
    
  }
