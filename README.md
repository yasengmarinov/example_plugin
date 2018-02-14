# Example Plugin

This is a sample project which aims to illustrate the use of the GraphDB Plugin API. Is is created entirely for training
purposes.

Additional documentation on the Plugin API and the project itself you can find
[here](http://graphdb.ontotext.com/free/plug-in-api.html) 

## Functionality

The Example Plugin has two responsibilities:
- It interprets patterns such as `?s <http://example.com/time> ?o` and binds their object component to a literal,
containing the repository local date and time.
- If a `FROM <http://example.com/time>` clause is detected in the query, the result is a single binding set in which
all projected variables are bound to a literal containing the repository local date and time at the beginning of the
query execution.

Also The IRI used by the plugin is configurable

## Overview

The main class in this project is `com.ontotext.trree.plugin.ExamplePlugin`. It extends
`com.ontotext.trree.sdk.PluginBase` - the base class for all Plugins.

The interfaces which the plugin implements are:
- `PatternInterpreter`- allows interpretation of basic triple patterns
- `Preprocessor` - used to add context to a request at the beginning if the processing
- `Postprocessor` - used to modify the query results
- `Configurable` - allows the plugin to use the System Options 

As the `PluginBase` implements `com.ontotext.trree.sdk.Service` we need to have service descriptor in
`META-INT/services/`. 

## Deployment
Below you can find the deployment steps for this plugin
1. In the `POM` set the version of GraphDB you are using
2. Build the project using `mvn clean install`
3. Move `./target/example-plugin-1.0-SNAPSHOT.jar` to  *<GDB_HOME>*/lib (or anywhere else on the classpath)

Once you start GraphDB you will see the following entries in the log:
```
Registering plugin example
Initializing plugin 'example'
Configured parameter 'predicate-uri' to default value 'http://example.com/time'
Example plugin initialized!
```

## Usage
Run the following query to retrieve the current System Time:
```
select ?o
where { 
    ?s <http://example.com/time> ?o .
} 
```

Run the following query to get all unbound variables set to System Time:
```
select *
FROM <http://example.com/time>
where
{
    ?s ?p ?o .
}
```

Change the `http://example.com/time` predicate to `http://example.com/newtime` by starting GraphDB with parameter
`-Dpredicate-uri="http://example.com/newtime"`

## Caution

Please be extremely careful when adding a new Plugin to GraphDB. Faulty plugins can have a devastating effect on the
system - they can have big performance impact, cause memory leaks or lead to nondeterministic behaviour!
