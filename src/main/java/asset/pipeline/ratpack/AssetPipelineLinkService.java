package asset.pipeline.ratpack;

import asset.pipeline.AssetPipelineConfigHolder;

class AssetPipelineLinkService {
    String getAt(String path) {
    	String pathAlias = path;
        if(AssetPipelineConfigHolder.manifest != null) {
            pathAlias = AssetPipelineConfigHolder.manifest.getProperty(path);
            if(pathAlias == null) {
                pathAlias = path;
            }
        }
        //TODO: FINISH THIS
        return pathAlias;
    }
}