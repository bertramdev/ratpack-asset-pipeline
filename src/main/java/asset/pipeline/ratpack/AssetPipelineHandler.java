package asset.pipeline.ratpack;

import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.launch.HandlerFactory;
import asset.pipeline.*;
import asset.pipeline.fs.*;
import ratpack.path.PathBinding;
import ratpack.http.Request;
import ratpack.http.Response;
import java.net.URISyntaxException;
import java.net.URI;
import java.util.Date;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import ratpack.file.MimeTypes;
import java.nio.file.Files;
import static ratpack.file.internal.DefaultFileRenderer.sendFile;
import static ratpack.file.internal.DefaultFileRenderer.readAttributes;
import io.netty.handler.codec.http.HttpHeaderNames;
import ratpack.http.internal.HttpHeaderConstants;
import static ratpack.util.Exceptions.uncheck;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

/**
* This handler is registered via a GUICE module to handle all assets
* @author David Estes
*/
class AssetPipelineHandler implements Handler {

    public void handle(Context context) throws Exception {
        Request request = context.getRequest();
        Response response = context.getResponse();

        String path = context.maybeGet(PathBinding.class)
        .map(PathBinding::getPastBinding)
        .orElse(request.getPath());
        String baseAssetUrl = "assets/";
        if(path.startsWith(baseAssetUrl)) {
            path = path.substring(baseAssetUrl.length());
        } else {
            context.next();
            return;
        }

        if (!request.getMethod().isGet()) {
            context.clientError(405);
            return;
        }

        try {
          path = new URI(path).getPath();
        } catch (URISyntaxException e) {
          throw uncheck(e);
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
            if(request.getQueryParams().get("compile") == "false") {
                fileContents = AssetPipeline.serveUncompiledAsset(path,format, null, encoding);
            } else {
                fileContents = AssetPipeline.serveAsset(path,format, null, encoding);
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
                response.status(404);
            }
        } else {
            //Production Mode!
            final Properties manifest = AssetPipelineConfigHolder.manifest;
            final String manifestPath = path.startsWith("/") ? path.substring(1) : path;
            final Path asset = context.file("assets/" + manifestPath);
            final AssetPipelineResponseBuilder responseBuilder = new AssetPipelineResponseBuilder(path,request.getHeaders().get(HttpHeaderNames.IF_NONE_MATCH));

            readAttributes(context, asset, attributes -> {
                if (attributes == null || !attributes.isRegularFile()) {
                    context.clientError(404);
                } else {
                    
                    if(responseBuilder.headers != null) {
                        for (Map.Entry<String, String> cursor : responseBuilder.headers.entrySet()) {
                            response.getHeaders().set(cursor.getKey(), cursor.getValue());
                        }
                    }
                        
                    if(responseBuilder.statusCode != null) {
                        response.status(responseBuilder.statusCode);
                    }

                    if(responseBuilder.statusCode != 304) {
                        String acceptsEncoding = request.getHeaders().get("Accept-Encoding");
                        String [] encodingArgs = acceptsEncoding.split(",");
                        if(Arrays.asList(encodingArgs).contains("gzip")) {
                            Path gzipFile = context.file("assets/" + manifestPath + ".gz");
                            readAttributes(context, gzipFile, gzipAttributes -> {
                                if (gzipAttributes == null || !gzipAttributes.isRegularFile()) {
                                    response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(attributes.size()));
                                    response.sendFile(asset);
                                } else {
                                    response.getHeaders().set("Content-Encoding","gzip");
                                    response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(gzipAttributes.size()));
                                    response.sendFile(gzipFile);
                                }
                            });
                        } else {
                            response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(attributes.size()));
                            response.sendFile(asset);
                        }
                    }
                }
            });
        }
    }
}