package asset.pipeline.ratpack;

import asset.pipeline.*;
import com.google.common.cache.CacheBuilder;
import com.google.common.reflect.TypeToken;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import ratpack.file.FileSystemBinding;
import ratpack.guice.ConfigurableModule;
import ratpack.guice.internal.GuiceUtil;
import ratpack.server.ServerConfig;
import ratpack.handling.HandlerDecorator;
import com.google.inject.multibindings.Multibinder;
import java.util.Map;

public class AssetPipelineModule extends ConfigurableModule<AssetPipelineModule.Config>{
    public static class Config {
        private Map<String,Object> assets;

        public Config assets(Map<String,Object> assets) {
        	this.assets = assets;
        	return this;
        }

        public Map<String,Object> getAssets() {
        	return this.assets;
        }
    }

    @Override
    protected void configure() {
        bind(AssetPipelineService.class).in(Singleton.class);
        Multibinder.newSetBinder(binder(), HandlerDecorator.class).addBinding().toInstance(HandlerDecorator.prepend(new AssetPipelineHandler()));
    }
}