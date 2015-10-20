# Including Twig in your project #

## Maven dependency ##

You can add Twig to a [Maven](http://maven.apache.org/) project with the following pom.xml snippet:

```
<dependencies>
	<dependency>
		<groupId>com.vercer.engine.persist</groupId>
		<artifactId>twig-persist</artifactId>
		<version>1.0.4</version>
	</dependency>
</dependencies>

<repositories>
	<repository>
	<id>twig</id>
	<url>http://mvn.twig-persist.googlecode.com/hg</url>
	</repository>
</repositories>
```

## Download compiled Jar ##

Get the latest jar from the [downloads page](http://code.google.com/p/twig-persist/downloads/list)

Get the single external dependency:
  1. [Google Collections 1.0](http://repo1.maven.org/maven2/com/google/collections/google-collections/1.0/google-collections-1.0.jar)

## Building from source ##

To keep up to date with the latest changes or make alterations you can check out the [source](http://code.google.com/p/twig-persist/source/checkout) and create a project for your IDE e.g.:

```
mvn eclipse:eclipse
```

Build the project to create a jar file:
```
mvn clean install
```