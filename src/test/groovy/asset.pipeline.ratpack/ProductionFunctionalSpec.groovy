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

package asset.pipeline.ratpack

import io.netty.handler.codec.http.HttpHeaderNames
import ratpack.func.Action
import ratpack.guice.Guice
import ratpack.server.RatpackServerSpec
import ratpack.server.ServerConfig
import ratpack.test.embed.EmbeddedApp
import spock.lang.AutoCleanup
import spock.lang.Specification

import static asset.pipeline.ratpack.TestConstants.PROD_BASE_DIR

/**
 * Created by danw on 7/13/15.
 */
class ProductionFunctionalSpec extends Specification {

  @AutoCleanup
  @Delegate
  EmbeddedApp app = of({ spec ->
    spec
        .serverConfig(ServerConfig.embedded(PROD_BASE_DIR).development(false))
        .registry(Guice.registry { b -> b
          .module(AssetPipelineModule)
        })
        .handlers { }
  } as Action<RatpackServerSpec>)

  void "should serve prod files"() {
    given:
    def response = httpClient.get("index.html")

    expect:
    response.statusCode == 200
    response.body.text == PROD_BASE_DIR.resolve("assets/index.html").text
  }

  void "gzipped accept-encoding should be respected"() {
    given:
    def response = httpClient.requestSpec { spec -> spec
      .headers { h -> h
        .set(HttpHeaderNames.ACCEPT_ENCODING, "gzip")
      }
      .decompressResponse(false)
    }.get("index.html")
    def bytes = response.body.bytes
    def assetBytes = PROD_BASE_DIR.resolve("assets/index.html.gz").bytes

    expect:
    response.statusCode == 200
    bytes == assetBytes
  }

  void "should serve index file for default path"() {
    given:
    def response = httpClient.get()

    expect:
    response.statusCode == 200
    response.body.text == PROD_BASE_DIR.resolve("assets/index.html").text
  }
}
