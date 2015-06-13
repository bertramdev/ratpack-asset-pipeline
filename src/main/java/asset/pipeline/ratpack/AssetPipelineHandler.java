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
import java.util.Arrays;
import ratpack.file.MimeTypes;
import static ratpack.file.internal.DefaultFileRenderer.sendFile;
import static ratpack.util.Exceptions.uncheck;

/**
* This handler is registered via a GUICE module to handle all assets
* @author David Estes
*/
class AssetPipelineHandler implements Handler {

    public void handle(Context context) {
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
        if(!AssetPipelineConfigHolder.manifest) {
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
            Properties manifest = AssetPipelineConfigHolder.manifest;
            String manifestPath = path;
            if(path.startsWith('/')) {
              manifestPath = path.substring(1); //Omit forward slash
            }

            Path asset = context.file("assets/" + manifestPath);
            if(asset != null) {
                AssetPipelineResponseBuilder responseBuilder = new AssetPipelineResponseBuilder(path,request.getHeaders().get("If-None-Match"));
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
                    String [] encodingArgs = acceptsEncoding.split(',')
                    if(Arrays.asList(encodingArgs).contains("gzip")) {
                        Path gzipFile = context.file("assets/" + manifestPath + ".gz");
                        if(gzipFile != null) {
                            asset = gzipFile;
                            response.getHeaders().set("Content-Encoding","gzip");
                        }
                    }
                    context.sendFile(asset);
                    // We have to serve a file now
                }
            } else {
                context.next();
            }
            
        }
    }
}