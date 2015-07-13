/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package asset.pipeline.ratpack;

import asset.pipeline.AssetPipelineConfigHolder;
import asset.pipeline.fs.FileSystemAssetResolver;
import ratpack.file.FileSystemBinding;
import ratpack.server.ServerConfig;
import ratpack.server.Service;
import ratpack.server.StartEvent;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
/**
* This service provides a startup configuration binding to tell the AP Config where to look for files
* @author David Estes
*/
public class AssetPipelineService implements Service {
    public List<String> events = new LinkedList<>();

    public void onStart(StartEvent startEvent) throws Exception {
        FileSystemBinding fileSystemBinding = startEvent.getRegistry().get(FileSystemBinding.class);
        ServerConfig serverConfig = startEvent.getRegistry().get(ServerConfig.class);
        AssetPipelineModule.Config config = startEvent.getRegistry().get(AssetPipelineModule.Config.class);
        
        if(config != null && config.getAssets() != null) {
            AssetPipelineConfigHolder.config = config.getAssets();
        }
        Path path = fileSystemBinding.getFile();
        Path manifest = fileSystemBinding.file("assets/manifest.properties");
        if(manifest != null && Files.exists(manifest)) {
            // We are in production mode
            AssetPipelineConfigHolder.config.put("precompiled",true);
            Properties manifestProps = new Properties();
            InputStream manIs = Files.newInputStream(manifest);
            manifestProps.load(manIs);
            manIs.close();
            AssetPipelineConfigHolder.manifest = manifestProps;
        } else {
            AssetPipelineConfigHolder.registerResolver(new FileSystemAssetResolver("application",path.toString() + "/../assets"));
            // AssetPipelineConfigHolder.registerResolver(new FileSystemAssetResolver("application","assets"));
        }
    }
}
