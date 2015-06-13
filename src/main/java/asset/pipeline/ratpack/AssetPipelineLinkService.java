package asset.pipeline.ratpack;

import asset.pipeline.AssetPipelineConfigHolder;

class AssetPipelineLinkService {
    String getAt(String path) {
        if(AssetPipelineConfigHolder.manifest != null) {
            String pathAlias = AssetPipelineConfigHolder.manifest.getProperty(path);
            if(pathAlias == null) {
                pathAlias = path;
            }
        }
    }
}