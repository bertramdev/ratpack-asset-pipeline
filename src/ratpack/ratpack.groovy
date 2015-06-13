import static ratpack.groovy.Groovy.ratpack
import static ratpack.handlebars.Template.handlebarsTemplate;
import ratpack.guice.Guice;
import ratpack.handlebars.HandlebarsModule;
import asset.pipeline.ratpack.AssetPipelineHandler;
import asset.pipeline.ratpack.AssetPipelineService;
import asset.pipeline.ratpack.AssetPipelineModule;

ratpack {
	bindings {
		add(new HandlebarsModule())
		add(new AssetPipelineModule())
	}
    handlers {
    	
        get {
            render handlebarsTemplate("index.html",[name:'test'])
        }
        
    	// handler(new AssetPipelineHandler())	
        
        
        
    }
}