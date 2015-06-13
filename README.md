## Ratpack Asset-Pipeline

This module provides runtime for both development and production to the Ratpack framework for rendering files compiled by Asset-Pipeline. This should be paired with the asset-pipeline-gradle plugin appropriately

**NOTE**: This is a WORK IN PROGRESS. 

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'com.bertramlabs.plugins:asset-pipeline-gradle:2.2.5'
    //Example additional LESS support
    //classpath 'com.bertramlabs.plugins:less-asset-pipeline:2.0.7'
  }
}

apply plugin: 'asset-pipeline'
assets {
    compileDir = "src/ratpack/assets"
}


dependencies {
	compile 'com.bertramlabs.plugins:ratpack-asset-pipeline:2.2.8'
	//Example additional LESS support
    //provided 'com.bertramlabs.plugins:less-asset-pipeline:2.0.7'
}
afterEvaluate{
 processResources.dependsOn assetCompile
}
```
Also add the Module to your registry

```groovy
bindings {
  module new AssetPipelineModule()
}
```

In your ratpack assets folder create organizational subfolders i.e. `assets/javascripts` or `assets/stylesheets`.

You can reference these files in your html templates via `/assets/application.js`. The md5 url replacement work is still in progress as Ratpack spec gets finalized.

### Things to be Done

* Integrate with ratpack configuration module for mapping to asset pipeline config map
* Url replacements for templates
* Testing