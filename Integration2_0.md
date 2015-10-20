# Building from source #

Pull the latest source from the Mercurial repository [here](http://code.google.com/p/twig-persist/source/checkout) and switch to the v2.0 branch using the command "hg update -C v2.0".  If you use TortoiseHg you can choose "TortoiseHg > Update > Update to" and choose v2.0.

Twig uses Apache Maven to manage dependencies and make the build process easier.  Simply run "mvn install -Dmaven.test.skip=true" to build the source into a jar file that you can include with your project.  This tells maven to skip the !JUnit tests as during development some of the tests may not pass which would cause the build to fail.

There is only one dependency: [Google Guava](http://code.google.com/p/guava-libraries/) which contains the very useful Collections library.  Make sure to put this jar in your /WEB-INF/lib/ directory along with the twig-persist-2.0.jar.

During development of v2.0 it can be good to keep up with the latest fixes and features as they are added to the repository.  To save yourself the bother of rebuilding the jar files every time you update the source code you can link the source code folders into your project so the class files are compiled directly to your own /WEB-INF/classes output directory.

In Eclipse this is done by creating a folder in your project (e.g. in /src/modules/twig/) and choosing "Advanced > Link to alternate location" then selecting the src folder from your local Twig repo.  Finally, in the Project Explorer you can right click on /src/modules/twig/src/main/java/ and choose "Build Path > Use as Source Folder".