# PDA Simulator

PDA Simulator is a visual simulator for pushdown automata. Its intended use is as an educational
tool for beginners. It includes support for multiple different but equivalent definitions. There are
a number of functionalities such as loading & saving, quick run, step-by-step mode, animation mode
and a transition table format. For more details and/or to download the latest JAR file, you can visit
[https://rambo37.github.io/PDA-Simulator-Web/](https://rambo37.github.io/PDA-Simulator-Web/).

## Getting started

These instructions are written for developers using Windows 10.

### Prerequisites

This application was built and run using IntelliJ IDEA with Java SE 8. Later Java SE versions should
also work.

Download [JavaFX 17.0.2](https://gluonhq.com/products/javafx/)

Download [JDK 17](https://www.oracle.com/java/technologies/downloads/#java17)

Download [IntelliJ IDEA](https://www.jetbrains.com/idea/)

### Setting up IntelliJ IDEA to run the application

Once IntelliJ has been downloaded, select File > New > Project from existing sources... and then
select the parent directory of both this README file and the src directory.

Next, repeatedly press the Next button until you are asked to specify the Project SDK. Click on the
plus button and select the downloaded JDK 17. On Windows, the folder was named jdk-17. Keep pressing
the Next button until the Finish button appears. Once it does, click on that.

After this, select File > Project structure > Libraries and then click on the plus button followed
by the Java option in the dropdown menu. Find and select the lib directory of the javafx-sdk-17.0.2
directory and then press the Ok button. Make sure to select both the main and test modules in the
"Choose modules" dialog before pressing the Ok button. Use the shift key and left-click on the test
module to do this. Next, press the Apply button and then the Ok button.

With that done, the next step is to edit the configurations. Before this can be done, go to the
PDASimulator class and click the green triangle to attempt to run it. You can click on either the
green triangle next to the class declaration or next to the main method declaration. Upon doing
this, you should get the following error message:

Error: JavaFX runtime components are missing, and are required to run this application

Now, go to the Run menu and select the Edit configurations... item. Make sure the PDASimulator class
is selected in the Run/Debug Configurations dialog and click on modify options. In the dropdown
menu, select "Add VM Options". In the newly added VM options text field, paste the following:

--module-path "PATH/TO/JAVAFX/LIB" --add-modules javafx.controls,javafx.fxml

with PATH/TO/JAVAFX/LIB replaced by the path to the lib folder in the javafx-sdk-17.0.2 folder.
Click Apply and then Ok.

The final step before being able to run the application within the IDE is to mark the resources
directory in the main directory as the resources root. This is done by right-clicking on it,
clicking on the "Mark directory as" option and then the "Resources Root" option. _Be careful to
click on Resources Root rather than Test Resources Root_.

Now you should be able to run PDASimulator without any errors being thrown.

## Running the tests
Before the tests can be run, it is necessary to add the org.junit.jupiter:junit-jupiter:5.7.0
Library. This can be done by navigating to any of the test files such as PDATest and right-clicking
on any of the import statements with "junit" in them. Click the "Show Context Actions" item and then
click the "Add 'Junit5.7.0' to class path" item. Then, press the Ok button.

With that done, the tests can now be run. You can run tests individually with the green icon next to
each test, or you can run all the tests in a file by clicking the green icon next to the class
declaration.
