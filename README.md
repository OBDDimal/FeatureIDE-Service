# FeatureIDE-Service
A microservice proving an RESTful API for the feature model conversion using the FeatureIDE library.

## Installation

## How to Use CLI 

1. Download the [FeatureIDE](https://featureide.github.io/) jar and save it in the `lib` folder.
2. Download the needed jars (currently found at [GitHub](https://github.com/FeatureIDE/FeatureIDE/tree/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib)): 
the [antlr-3.4.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/antlr-3.4.jar), 
   [org.sat4j.core.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/org.sat4j.core.jar),
   and [uvl-parser.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/uvl-parser.jar) 
   into the `lib` folder.
3. In the `build.gradle.kts` file check that the version numbers of the libraries match.

### For Converting and Check
Options:  
    --path, -p -> Input path for file or directory. (always required) { String } <br />
    --check, -c -> Input path for the second file that should be checked with the first one. { String } <br />
    --all, -a [false] -> Parsers all files from path into all formats.  <br />
    --dimacs, -d [false] -> Parses all files from path into dimacs files. <br />
    --uvl, -u [false] -> Parses all files from path into uvl files. <br />
    --sxfm, -sf [false] -> Parses all files from path into sxfm(xml) files. <br />
    --featureIde, -fi [false] -> Parses all files from path into featureIde(xml) files. <br />

    For Example:  <br />
    -p "FeatureIDE.xml" -c "FeatureIDE2.dimacs"  --> Checks if the Hashes of both files are the same after Converting it to the same format <br />
    -p "FeatureIDE.xml" -u  --> This command takes the FeatureIDE.xml File and converts it to an uvl file <br />
    
### For Slicing
Options:  
    --path, -p -> Input path for file. (always required) { String } <br />
    --selection, -s -> The names of the features that should be sliced separated by ','. For example: Antenna,AHEAD. { String } <br />

    For Example:  <br />
    -p "FeatureIDE.xml" -s "Antenna"  --> This command takes the FeatureIDE.xml File and slices out the Feature Antenna <br />
    
### For Configuration Samples
Options:  
    --path, -p -> Input path for file. (always required) { String } <br />
    --algorithm, -alg -> The algorithm to generate a configuration sample as csv file { String } <br />
    --t, -t [0] -> The t wise pairing that should be covered by the configuration sample { Int } <br />
    --limit, -l [2147483647] -> The maximum amount of configurations for the configuration sample { Int } <br />

    For Example:  <br />
    -p "FeatureIDE.xml" -alg "yasa_50" -t 2 -l 10  --> This command takes the FeatureIDE.xml File and gives an sampling for the model with the parameters iterations 50, 2-wise and a limit of 10 <br />

### For DecisionPropagation
Options:  
    --path, -p -> Input path for file. (always required) { String } <br />
    --selection, -s -> The names of the features that are already selected separated by ','. For example: Antenna,AHEAD. { String } <br />

    For Example:  <br />
    -p "FeatureIDE.xml" -s "Antenna"  --> This command takes the FeatureIDE.xml File and returns a Array of Features that need to be selected to provide a valid configuration <br />


All of the Files are saved after the operation in the "files/output" directory which is completely cleared before saving any files. <br />
 --help, -h -> Usage info  <br />

### Start in Intellij

Use the CLI Main Method to start the Application and use the configurations option in Intellij to configure Commandline Arguments.

### Start in Terminal

Use "./gradlew run -Pcli="typeOfCLI" --args="fill in arguments"" in a Terminal

The different types are: 
* conv - Converting files and checking
* slice - Slicing FMs
* conf - Configuration Samples
* prop - Decision Propagation

  
## How to Start Server API

### Build and Run as Docker Container

1. Change dir to ktor-api directory 
2. Run docker compose up

Building the jar takes some time (1-2 minutes).

### Use it in Intellij
1. Download the [FeatureIDE](https://featureide.github.io/) jar and save it in the `lib` folder.
2. Download the needed jars (currently found at [GitHub](https://github.com/FeatureIDE/FeatureIDE/tree/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib)): 
the [antlr-3.4.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/antlr-3.4.jar), 
   [org.sat4j.core.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/org.sat4j.core.jar),
   and [uvl-parser.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/uvl-parser.jar) 
   into the `lib` folder.
3. In the `build.gradle.kts` file check that the version numbers of the libraries match.
4. Run Main Method of Application

### Use it in Terminal
1. Download the [FeatureIDE](https://featureide.github.io/) jar and save it in the `lib` folder.
2. Download the needed jars (currently found at [GitHub](https://github.com/FeatureIDE/FeatureIDE/tree/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib)): 
the [antlr-3.4.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/antlr-3.4.jar), 
   [org.sat4j.core.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/org.sat4j.core.jar),
   and [uvl-parser.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/uvl-parser.jar) 
   into the `lib` folder.
3. In the `build.gradle.kts` file check that the version numbers of the libraries match.
4. Run "./gradlew run" in Commandline

## How to Use API NOT UPDATED

Send the files you want to convert with an HTTP POST to the path `/convert`.
You will receive a status from the server with a `requestNumber`.
You can use this request number to check for the status of your conversion at the path `/check/{requestNumber}`.
The API answers with a status.
At the path `/result/{requestNumber}` the API returns the result of the conversion (when the service converted all requested conversions of this request) and deletes it from the server.

### InputSlice
The service accepts as input a list of files as JSON.
The format of the entry:
* name: String
* typeOutput: array of Strings
* fileContent: array of Bytes

#### Description
* name: the name of the file
* typeOutput: a list of types you want to convert the file to (currently supported: dimacs, uvl, sxfm, featureIde)
* fileContent: the content of the file

### OutputSlice
Sends a list of the outputted files as JSON.
The format of the entry::
* name: String
* type: String
* content: array of Bytes

#### Description
* name: the name of the converted file
* type: the type of the converted file
* fileContent: the content of the file

## Testing

1. Download the [FeatureIDE](https://featureide.github.io/) jar and save it in the `lib` folder.
2. Download the needed jars (currently found at [GitHub](https://github.com/FeatureIDE/FeatureIDE/tree/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib)): 
the [antlr-3.4.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/antlr-3.4.jar), 
   [org.sat4j.core.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/org.sat4j.core.jar),
   and [uvl-parser.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/uvl-parser.jar) 
   into the `lib` folder.
3. In the `build.gradle.kts` file check that the version numbers of the libraries match.

To run all tests:
`gradle test `
