[![Micrometer Galleon Pack Java CI](https://github.com/jasondlee/wildfly-micrometer-extension/actions/workflows/ci.yml/badge.svg)](https://github.com/jasondlee/wildfly-micrometer-extension/actions/workflows/ci.yml)

# Wildfly Galleon Micrometer Feature Pack 

Adds Micrometer support to WildFly. Details coming...

## Building the Galleon feature pack

To build the feature pack, simply clone this repository, and on your command line go to the checkout folder and
run

```
mvn install
```

and it will build everything, and run the testsuite. An example patched server will be created in the `build/target/`
directory. We will explore how to use Galleon CLI to provision a server from the command line later on.

## Running the example application

The example application lives in the [`example/`](example) directory. It is a trivial application exposing a
[REST endpoint](example/src/main/java/org/wildfly/extension/micrometer/example/MetricResource.java)
which is injected with an instance of Micrometer `io.micrometer.core.instrument.MeterRegistry`.

Start the server by running

```
./build/target/wildfly-<WildFly Version>-micrometer-<Feature Pack Version>/bin/standalone.sh
```

In another terminal window run:

```
mvn package wildfly:deploy -pl example
```

and see the application gets deployed.

Then go to http://localhost:8080/example/ and see the current count of the meter `demo-counter`. To see the meter as 
reported by the server, access http://localhost:9990/micrometer and search for `demo-counter`.

## Installing the feature pack into a WildFly installation with Galleon CLI

The server we have seen from the `build/` folder is handy for running our example, but it is not really how we install
Galleon feature packs in real life.

To do this we first provision WildFly itself using Galleon CLI. This is an alternative to the more common way of
downloading and extracting the zip from the [WildFly downloads page](https://www.wildfly.org/downloads).

You first need to [download Galleon](https://github.com/wildfly/galleon/releases) and unzip it somewhere. In my case I 
just have it in my `~/Downloads` folder, and I am using Galleon 4.2.8.

As we will see below there are two main ways to use Gallon CLI to provision servers. We can either run commands directly
in Galleon CLI directly, or we can provide an XML file which contains all the information for Galleon CLI to be able to
provision a server.

### Using CLI commands directly

This consists of two steps.

1) Installing the base WildFly server
2) Installing our Galleon feature pack with the extra functionality

#### Install main server

First we run the CLI to install the full WildFly server (the result will be the same as the downloaded zip):

```
$ galleon.sh install wildfly:current --dir=wildfly
```

The `wildfly:current` above tells Galleon to provision the latest version of WildFly which at the time of writing is
24.0.1.Final. If you want to install a particular version of WildFly, you can append the version, e.g:

* `wildfly:current#24.0.1.Final` - installs WildFly from locally build maven artifacts

`--dir` specifies the directory to install the server into, in this case, a relative directory called `wildfly`.

If you want to trim the base server that we install (similar to what we did in the testsuite and the example server
build), you can specify which layers to install by passing in the `--layers` option. To install the same server as we 
used to run the example above, you can run:

```
$ galleon.sh install wildfly:current --dir=wildfly --layers=jaxrs,management
```

Note that we did not install our layer `micrometer` because this is unknown in the main WildFly feature pack. We will 
add it in the next step.

#### Install our layer

Next we want to install our layer. We do this by running:

```
$ galleon.sh install org.wildfly.extras:micrometer-feature-pack:1.0.0.Alpha-SNAPSHOT --layers=micrometer --dir=wildfly
``` 

`org.wildfly.extras:micrometer-feature-pack:1.0.0.Alpha-SNAPSHOT` is the Maven GAV of the Galleon feature pack (i.e. 
what we have in [`feature-pack/pom.xml`](feature-pack/pom.xml)).

If you went with the trimmed server in the previous step, and you look at `wildfly/standalone/configuration/standalone.xml`, 
you should see that both the `micrometer` and the `weld` subsystems were added in this second step. Weld is our CDI 
implementation, and as we have seen CDI is a dependency of our layer, so Galleon pulls it in too!

Now you can start the server by running

```
$ ./wildfly/bin/standalone.sh
``` 

and in another terminal window you can deploy the example into this server

```
$ mvn package wildfly:deploy -pl example/
```

and then go to http://localhost:8080/example/greeting as before.

### Provisioning from an XML file

An alternative to having to type all those CLI commands every time you want to provision a server is to use an XML file
as input to the Galleon CLI. There is an example in
[`provision.xml`](provision.xml)

As you can see, it lists the feature pack(s) we depend on, and our feature pack. For each of those we specify the GAV,
as in the previous section. We can set what to include from each feature pack (Refer to the
[Galleon documentation](https://docs.wildfly.org/galleon/) for more in-depth explanation of what each setting does). And
finally, we say that we want the `cloud-profile`
and `micrometer` layers. `cloud-profile` is just to give you another example server, we could have used the same
layers as in the previous section.

To provision the server, you now simply run the following command:

```
$ galleon.sh provision /path/to/provision.xml --dir=wildfly
``` 

Now you can start the server and run the example as we saw in the previous section.

## How do I know which layers to depend on?

There is a list of all our layers defined by WildFly and WildFly Core in our
[documentation](https://docs.wildfly.org/24/Galleon_Guide.html#wildfly_galleon_layers).
