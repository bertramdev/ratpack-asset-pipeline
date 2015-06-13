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
import ratpack.file.MimeTypes;
import static ratpack.util.Exceptions.uncheck;


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

        System.out.println("Requested File at Path" + path + " From - " + request.getPath());

        byte[] fileContents;
        String encoding = request.getQueryParams().get("encoding");
        if(encoding == null) {
            encoding = request.getBody().getContentType().getCharset();;
        }
        String format   = context.get(MimeTypes.class).getContentType(path);
              // response.contentTypeIfNotSet(() -> context.get(MimeTypes.class).getContentType(file.getFileName().toString()));

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
    }
}