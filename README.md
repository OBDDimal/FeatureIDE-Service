# FeatureIDE-Service
A microservice proving an RESTful API for the feature model conversion using the FeatureIDE library.

## Installation

## How to Use CLI 

### Start in Intellij

Use the CLI Main Method to start the Application and use the configurations option in Intellij to configure Commandline Arguments.

### Start in Terminal

Use "./gradlew run -Pcli="typeOfCLI" --args="fill in arguments"" in a Terminal

The different types are: 
* conv - Converting files and checking
* slice - Slicing FMs
* conf - Configuration Samples
* prop - Decision Propagation

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
    --time, -ti [-1] -> The maximum time that an algorithm can use for the execution only with yasa { Int } <br />

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


  
## How to Start Server API

### Build and Run as Docker Container

1. Change dir to ktor-api directory 
2. Run docker compose up

Building the jar takes some time (1-2 minutes).

### Use it in Intellij
1. Run Main Method of Application

### Use it in Terminal
1. Run "./gradlew run" in Commandline

## How to Use API 

### CONVERT

Send the file you want to convert with an HTTP POST to the path `/convert`.
The content of the HTTP POST need to be the following.

#### Input
* name: String - The name of the featuremodel
* typeOutput: Array\<String> - The list of types to convert the file to (currently supported: dimacs, uvl, sxfm, featureIde)
* content: Array\<Byte> - The content of the featuremodel

After this you receive a HTTP Message with the content "Request Accepted" and a Location header to now receive your converted file/s make a HTTP GET Request until you receive the HTTP STATUS OK.
The content of the message will be following:

#### Output
* name: Array\<String> - The name of the different files
* typeOutput: Array\<String> - The list of types to convert the file to (currently supported: dimacs, uvl, sxfm, featureIde)
* content: Array\<Byte> - The contents of the different files


### SLICE

Send the file you want to slice with an HTTP POST to the path `/slice`.
The content of the HTTP POST need to be the following.

#### Input
* name: String - The name of the featuremodel
* selection: Array\<String> - The features that should be sliced
* content: Array\<Byte> - The content of the featuremodel

After this you receive a HTTP Message with the content "Request Accepted" and a Location header. To receive your sliced file make a HTTP GET Request on the Location until you receive the HTTP STATUS OK.
The content of the message will be following:

#### Output
* name: String - The name of the feature model
* selection: Array\<String> - The features that were sliced
* content: Array\<Byte> - The contents of the sliced feature model


### CONFIGURATION

Send the file you want to generate a sample with an HTTP POST to the path `/configuration`.
The content of the HTTP POST need to be the following.

#### Input
* name: String - The name of the featuremodel file
* algorithm: String - The algorithm that should be used to generate the sample. Special case for "yasa" with "yasa_Number" the iterations can be changed
* t: Int - The T-Wise configuration for the algorithm
* limit: Int - The maximum amount of samples that the algorithm should use
* content: Array\<Byte> - The content of the featuremodel file in xml

After this you receive a HTTP Message with the content "Request Accepted" and a Location header to now receive your converted file/s make a HTTP GET Request until you receive the HTTP STATUS OK.
The content of the message will be following:

#### Output
* name: String - The name of the featuremodel file
* algorithm: String - The algorithm that should be used to generate the sample. Special case for "yasa" with "yasa_Number" the iterations can be changed
* t: Int - The T-Wise configuration for the algorithm
* limit: Int - The maximum amount of samples that the algorithm should use
* content: Array\<Byte> - The content of the CSV file that was generated

### PROPAGATION

Send the file you want to get an decision propagation with an HTTP POST to the path `/propagation`.
The content of the HTTP POST need to be the following.

#### Input
* name: String - The name of the featuremodel
* selection: Array\<String> - The features that are selected
* content: Array\<Byte> - The content of the featuremodel

After this you receive a HTTP Message with the content "Request Accepted" and a Location header to now receive your converted file/s make a HTTP GET Request until you receive the HTTP STATUS OK.
The content of the message will be following:

#### Output
* name: String - The name of the featuremodel
* selection: Array\<String> - The features that are selected
* impliedSelection: Array\<String> - The features that need to be selected because of the first selection
* content: Array\<Byte> - The content of the featuremodel


## Testing

To run all tests:
`gradle test `
