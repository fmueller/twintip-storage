# TWINTIP

TWINTIP is an API definition crawler, that constantly crawls a list of applications for their API definitions.
It works in conjunction with [Kio](http://zalando-stups.github.io) to get a list of all applications and their
service endpoints and fetches their swagger specifications.

## Download

Releases are pushed as Docker images in the [public Docker registry](https://registry.hub.docker.com/u/stups/):

You can run it with Docker:

    $ docker run -it stups/twintip-storage

## Requirements

* PostgreSQL 9.3+

## Configuration

Configuration is provided via environment variables during start.

Variable         | Default                    | Description
---------------- | -------------------------- | -----------
HTTP_PORT        | `8080`                     | TCP port to provide the HTTP API.
HTTP_CORS_ORIGIN |                            | Domain for cross-origin JavaScript requests. If set, the Access-Control headers will be set.
DB_SUBNAME       | `//localhost:5432/twintip` | JDBC connection information of your database.
DB_USER          | `postgres`                 | Database user.
DB_PASSWORD      | `postgres`                 | Database password.

Example:

```
$ docker run -it \
    -e HTTP_CORS_ORIGIN="*.zalando.de" \
    -e DB_USER=twintip \
    -e DB_PASSWORD=twintip123 \
    stups/twintip-storage
```

## Building

    $ lein uberjar
    $ lein docker build

## Releasing

    $ lein release :minor

## Developing

Twintip embeds the [reloaded](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded) workflow for interactive
development:

    $ lein repl
    user=> (go)
    user=> (reset)

## License

Copyright © 2015 Zalando SE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
