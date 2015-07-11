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

import asset.pipeline.AssetPipeline;
import asset.pipeline.AssetPipelineConfigHolder;
import asset.pipeline.AssetPipelineResponseBuilder;
import io.netty.handler.codec.http.HttpHeaderNames;
import ratpack.file.MimeTypes;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.path.PathBinding;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static ratpack.file.internal.DefaultFileRenderer.readAttributes;
import static ratpack.util.Exceptions.uncheck;

/**
* This handler is registered via a GUICE module to handle all assets
* @author David Estes
*/
public class AssetPipelineHandler implements Handler {
    ConcurrentHashMap<String,AssetAttributes> fileCache = new ConcurrentHashMap<String,AssetAttributes>();

    public void handle(Context context) throws Exception {
        Request request = context.getRequest();
        Response response = context.getResponse();
        AssetPipelineModule.Config config = context.get(AssetPipelineModule.Config.class);
        String baseAssetUrl = config.getUrl();

        String path = normalizePath(context.maybeGet(PathBinding.class)
            .map(PathBinding::getPastBinding)
            .orElse(request.getPath()));
        
        if(baseAssetUrl != null) {
            if(path.startsWith(baseAssetUrl)) {
                path = path.substring(baseAssetUrl.length());
            } else {
                context.next();
                return;
            }    
        }
        

        if (!request.getMethod().isGet()) {
            context.clientError(405);
            return;
        }

        try {
          path = path.length() > 0 ? new URI(path).getPath() : "/";
        } catch (URISyntaxException e) {
          throw uncheck(e);
        }
        if(path.endsWith("/")) {
            path = path + config.getIndexFile();
        }

        // System.out.println("Requested File at Path" + path + " From - " + request.getPath());

        byte[] fileContents;
        String encoding = request.getQueryParams().get("encoding");
        if(encoding == null) {
            encoding = request.getBody().getContentType().getCharset();;
        }
        String format   = context.get(MimeTypes.class).getContentType(path);

        //Development/Runtime Mode
        if(AssetPipelineConfigHolder.manifest == null) {
            if("false".equals(request.getQueryParams().get("compile"))) {
                fileContents = AssetPipeline.serveUncompiledAsset(path,format, null, encoding);
            } else {
                fileContents = AssetPipeline.serveAsset(path,format, null, encoding);
            }

            if(fileContents == null && !path.endsWith('/'+config.getIndexFile())) {
                path = path + '/' + config.getIndexFile();
                format =  "text/html";
                if("false".equals(request.getQueryParams().get("compile"))) {
                    fileContents = AssetPipeline.serveUncompiledAsset(path,format, null, encoding);
                } else {
                    fileContents = AssetPipeline.serveAsset(path,format, null, encoding);
                }                
            }

            if (fileContents != null) {
                response.getHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
                response.getHeaders().set("Pragma", "no-cache"); // HTTP 1.0.
                response.getHeaders().setDate("Expires", new Date(0)); // Proxies.
                response.getHeaders().set("Content-Length", Integer.toString(fileContents.length));
                
                response.contentTypeIfNotSet(format);
                try {
                    response.send(fileContents);
                } catch(Exception e) {

                    //Log something here
                }
            } else {
                context.clientError(404);
            }
        } else {
            serveProductionAsset(path, config, context, request, response, format);

        }
    }

    private void serveProductionAsset(String path, AssetPipelineModule.Config config, Context context, Request request, Response response, String format) throws Exception{
            //Production Mode!
            final Properties manifest = AssetPipelineConfigHolder.manifest;
            String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

            final String indexedPath = String.format("%s/%s", path, config.getIndexFile());
            final String manifestPath = manifest.getProperty(normalizedPath,normalizedPath);
            final Path asset = context.file("assets/" + manifestPath);
            final AssetPipelineResponseBuilder responseBuilder = new AssetPipelineResponseBuilder(path,request.getHeaders().get(HttpHeaderNames.IF_NONE_MATCH));

            //Is this in the attribute cache?
            AssetAttributes attributeCache = fileCache.get(manifestPath);
            if(attributeCache != null) {
                if(attributeCache.exists()) {
                    response.contentTypeIfNotSet(format);
                    if(responseBuilder.headers != null) {
                        for (Map.Entry<String, String> cursor : responseBuilder.headers.entrySet()) {
                            response.getHeaders().set(cursor.getKey(), cursor.getValue());
                        }
                    }
                    if(responseBuilder.statusCode != null) {
                        response.status(responseBuilder.statusCode);
                    }

                    if(responseBuilder.statusCode == null || responseBuilder.statusCode != 304) {
                        if(acceptsGzip(request) && attributeCache.gzipExists()) {
                            Path gzipFile = context.file("assets/" + manifestPath + ".gz");
                            response.getHeaders().set("Content-Encoding","gzip");
                            response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(attributeCache.getGzipFileSize()));
                            response.sendFile(gzipFile);
                        } else {
                            response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(attributeCache.getFileSize()));
                            response.noCompress().sendFile(asset);
                        }
                    } else {
                        response.send();
                    }
                } else if(attributeCache.isDirectory()) {
                    serveProductionAsset(indexedPath, config, context, request, response, "text/html");
                    return;
                } else {
                    response.status(404).send();
                }
            } else {
                readAttributes(context, asset, attributes -> {
                    if (attributes == null || !attributes.isRegularFile()) {
                        
                        if(!path.endsWith('/'+config.getIndexFile()) && attributes != null) {
                            fileCache.put(manifestPath,new AssetAttributes(false,false,true, null , null));
                            serveProductionAsset(indexedPath, config, context, request, response, "text/html");
                            return;
                        } else {
                            fileCache.put(manifestPath,new AssetAttributes(false,false,false, null , null));
                            response.status(404).send();
                        }
                    } else {
                        response.contentTypeIfNotSet(format);
                        if(responseBuilder.headers != null) {
                            for (Map.Entry<String, String> cursor : responseBuilder.headers.entrySet()) {
                                response.getHeaders().set(cursor.getKey(), cursor.getValue());
                            }
                        }
                            
                        if(responseBuilder.statusCode != null) {
                            response.status(responseBuilder.statusCode);
                        }

                        if(responseBuilder.statusCode == null || responseBuilder.statusCode != 304) {
                            Path gzipFile = context.file("assets/" + manifestPath + ".gz");
                            if(acceptsGzip(request)) {
                                readAttributes(context, gzipFile, gzipAttributes -> {
                                    if (gzipAttributes == null || !gzipAttributes.isRegularFile()) {
                                        response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(attributes.size()));
                                        fileCache.put(manifestPath,new AssetAttributes(true,false,false, attributes.size() , null));
                                        response.noCompress().sendFile(asset);
                                    } else {
                                        response.getHeaders().set("Content-Encoding","gzip");
                                        response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(gzipAttributes.size()));
                                        fileCache.put(manifestPath,new AssetAttributes(true,true,false, attributes.size() , gzipAttributes.size()));
                                        response.sendFile(gzipFile);
                                    }
                                });
                            } else {
                                response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(attributes.size()));
                                response.noCompress().sendFile(asset);
                                readAttributes(context, gzipFile, gzipAttributes -> {
                                    if (gzipAttributes == null || !gzipAttributes.isRegularFile()) {
                                        fileCache.put(manifestPath,new AssetAttributes(true,false,false, attributes.size() , null));
                                    } else {
                                        fileCache.put(manifestPath,new AssetAttributes(true,true,false, attributes.size() , gzipAttributes.size()));
                                    }
                                });
                            }
                        } else {
                            response.send();
                        }
                    }
                });               
            }
    }

    private boolean acceptsGzip(Request request) {
        String acceptsEncoding = request.getHeaders().get("Accept-Encoding");
        String [] encodingArgs = acceptsEncoding.split(",");
        if(Arrays.asList(encodingArgs).contains("gzip")) {
            return true;
        }
        return false;
    }

    private static String normalizePath(String path) {
      String[] parts = path.split("/");
      String fileName = parts[parts.length-1];
      if (!fileName.contains(".")) {
        path = path + '/';
      }
      return path;
    }
}