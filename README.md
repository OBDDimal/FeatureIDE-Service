# FeatureIDE-Service
A microservice proving an RESTful API for the feature model conversion using the FeatureIDE library.

## Installation
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
4. Run Main Method of ApplicationTest

## How to Use CLI in Intellij

Use the CLI Main Method to start the Application and use the different options to get your needed Files.

Value for option --path should be always provided in command line.
Usage: featureide-cli options_list
Options: 
    --path, -p -> Input path for file or directory. (always required) { String }
    --slice, -s -> The names of the features that should be sliced separated by ','. For example: Antenna,AHEAD. { String }
    --check, -c -> Input path for the second file that should be checked with the first one. { String }
    --algorithm, -alg -> The algorithm to generate a configuration sample as csv file { String }
    --all, -a [false] -> Parsers all files from path into all formats. 
    --dimacs, -d [false] -> Parses all files from path into dimacs files. 
    --uvl, -u [false] -> Parses all files from path into uvl files. 
    --sxfm, -sf [false] -> Parses all files from path into sxfm(xml) files. 
    --featureIde, -fi [false] -> Parses all files from path into featureIde(xml) files. 
    --t, -t [0] -> The t wise pairing that should be covered by the configuration sample { Int }
    --limit, -l [2147483647] -> The maximum amount of configurations for the configuration sample { Int }
    --help, -h -> Usage info 

For Example: 
-p "FeatureIDE.xml" -s "Antenna"  --> This command takes the FeatureIDE.xml File and slices out the Feature Antenna
-p "FeatureIDE.xml" -c "FeatureIDE2.dimacs"  --> Checks if the Hashes of both files are the same after Converting it to the same format
-p "FeatureIDE.xml" -alg "yasa_50" -t 2 -l 10  --> This command takes the FeatureIDE.xml File and gives an sampling for the model with the parameters iterations 50, 2-wise and a limit of 10
-p "FeatureIDE.xml" -u  --> This command takes the FeatureIDE.xml File and converts it to an uvl file

All of the Files are saved after the operation in the "files/output" directory which is completely cleared before saving any files.


## How to Use API NOT UPDATED

Send the files you want to convert with an HTTP POST to the path `/convert`.
You will receive a status from the server with a `requestNumber`.
You can use this request number to check for the status of your conversion at the path `/check/{requestNumber}`.
The API answers with a status.
At the path `/result/{requestNumber}` the API returns the result of the conversion (when the service converted all requested conversions of this request) and deletes it from the server.

### Status

The format of the entry:
* requestNumber: Int
* finished: Boolean
* amountToProcess: Int
* resourceLocation: String

#### Description

* requestNumber: the number of the request
* finished: indicates if the service finished the conversion request and all converted files are ready to be downloaded.
* amountToProcess: the amount of files still needed to be converted by the server
* resourceLocation: the location of the results (only set, if finished = true)

### Input
The service accepts as input a list of files as JSON.
The format of the entry:
* name: String
* typeOutput: array of Strings
* fileContent: array of Bytes

#### Description
* name: the name of the file
* typeOutput: a list of types you want to convert the file to (currently supported: dimacs, uvl, sxfm, featureIde)
* fileContent: the content of the file

### Output
Sends a list of the outputted files as JSON.
The format of the entry::
* name: String
* originalName: String
* type: String
* success: Boolean
* content: array of Bytes

#### Description
* name: the name of the converted file
* originalName: the name of the original uploaded file
* type: the type of the converted file
* success: indicates, if the converting was successful. If it was not successful the name is empty, and the fileContent contains the error message
* fileContent: the content of the file

## Testing

For testing you need to run a postgres database.
The `docker-compose_text.yml` file starts a postgres database for testing.
If you use your own solution, check that the URL in `src/main/ressources/test.conf` fits the ip and port of your database.

To run the `ApplicationTest` test class, run:
`gradle test --tests "de.featureide.service.ApplicationTest"`
