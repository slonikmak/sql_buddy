# SQL Buddy. Yor personal SQL assistant.

The AI powered SQL assistant that helps you write SQL queries faster and more efficiently.


## Specify the OPEN-API key

You should set the OPEN-API key in the system environment variable `OPENAI_API_KEY` before running the application.

## Development mode

To start the Figwheel compiler, navigate to the project folder and run the following command in the terminal:

```
lein figwheel
```

Figwheel will automatically push cljs changes to the browser. The server will be available at [http://localhost:3449](http://localhost:3449) once Figwheel starts up. 

Figwheel also starts `nREPL` using the value of the `:nrepl-port` in the `:figwheel`
config found in `project.clj`. By default the port is set to `7002`.

The figwheel server can have unexpected behaviors in some situations such as when using
websockets. In this case it's recommended to run a standalone instance of a web server as follows:

```
lein do clean, run
```

The application will now be available at [http://localhost:3000](http://localhost:3000).


### Optional development tools

Start the browser REPL:

```
$ lein repl
```
The Jetty server can be started by running:

```clojure
(start-server)
```
and stopped by running:
```clojure
(stop-server)
```


## Building for release

```
lein do clean, uberjar
```

## Building the Docker image

```
docker build -f setup/Dockerfile -t sql-buddy .
```

### Running the Docker image

```
 docker run -p 3000:3000 -e "OPENAI_API_KEY=your-openai-api-key" sql-buddy  
```