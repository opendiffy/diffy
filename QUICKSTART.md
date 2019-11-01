# Getting started
## Running the example
The example.sh script included here builds and launches example servers as well as a diffy instance. Verify
that the following ports are available (9000, 9100, 9200, 8880, 8881, & 8888) and run `./example/run.sh start`.

Once your local Diffy instance is deployed, you send it a few requests by running `./example/traffic.sh`.

You can now go to your browser at
[http://localhost:8888](http://localhost:8888) to see what the differences across our example instances look like.

## Digging deeper
That was cool but now you want to compare old and new versions of your own service. Here’s how you can
start using Diffy to compare three instances of your service:

1. Deploy your old code to `localhost:9990`. This is your primary.
2. Deploy your old code to `localhost:9991`. This is your secondary.
3. Deploy your new code to `localhost:9992`. This is your candidate.
4. Download the latest Diffy binary from maven central or build your own from the code using `./sbt assembly`.
5. Run the Diffy jar with following command line arguments:

    ```
    java -jar diffy-server.jar \
    -candidate=localhost:9992 \
    -master.primary=localhost:9990 \
    -master.secondary=localhost:9991 \
    -service.protocol=http \
    -serviceName=Fancy-Service \
    -proxy.port=:8880 \
    -admin.port=:8881 \
    -http.port=:8888 \
    -rootUrl="localhost:8888" \
    -summary.email="info@diffy.ai" \
    -summary.delay="5"
    ```

6. Send a few test requests to your Diffy instance on its proxy port:

    ```
    curl localhost:8880/your/application/route?with=queryparams
    ```

7. Watch the differences show up in your browser at [http://localhost:8888](http://localhost:8888).

8. Note that after ```summary.delay``` minutes, your Diffy instance will email a summary report to your ```summary.email``` address.

*NOTE*: By default the names of the resources in the UI are fetched from the `Canonical-Resource` header in each
request. However this can be configured at boot with a static line, example: 
```
-resource.mapping='/foo/:param;f,/b/*;b,/ab/*/g;g,/z/*/x/*/p;p' \
```
In the snippet above the configuration form is: `<pattern>;<resource-name>[,<pattern>;<resource-name>]*`

The first matching configuration will be found

## Using Diffy with Docker

You can pull the [official docker image](https://hub.docker.com/r/diffy/diffy/) with `docker pull diffy/diffy`

And run it with
```
docker run -d --name diffy-01 \
  -p 8880:8880 -p 8881:8881 -p 8888:8888 \
  diffy/diffy \
    -candidate=localhost:9992 \
    -master.primary=localhost:9990 \
    -master.secondary=localhost:9991 \
    -service.protocol=http \
    -serviceName="Tier-Service" \
    -proxy.port=:8880 \
    -admin.port=:8881 \
    -http.port=:8888 \
    -rootUrl=localhost:8888 \
    -summary.email="docker@diffy.ai" \
    -summary.delay="5"
```

You should now be able to point to:
 - http://localhost:8888 to see the web interface
 - http://localhost:8881/admin for admin console
 - Use port 8880 to make the API requests

*NOTE*: You can  pull the [sample service](https://hub.docker.com/r/diffy/example-service/) and deploy the `production` (primary, secondary) and `candidate` tags to start playing with diffy right away.

You can always build the image from source with `docker build -t diffy .`


## FAQ's
   For safety reasons `POST`, `PUT`, ` DELETE ` are ignored by default . Add ` -allowHttpSideEffects=true ` to your command line arguments to enable these verbs.

## HTTPS
If you are trying to run Diffy over a HTTPS API, the config required is:

    -service.protocol=https

And in case of the HTTPS port be different than 443:

    -https.port=123
