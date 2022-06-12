# FeatureIDE-Service
A microservice proving an RESTful API for the feature model conversion using the FeatureIDE library.

## Installation
### Building
1. Download the [FeatureIDE](https://featureide.github.io/) jar and save it in the `lib` folder.
2. Download the needed jars (currently found at [GitHub](https://github.com/FeatureIDE/FeatureIDE/tree/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib)): 
the [antlr-3.4.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/antlr-3.4.jar), 
   [org.sat4j.core.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/org.sat4j.core.jar),
   and [uvl-parser.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/uvl-parser.jar) 
   into the `lib` folder.
3. In the `build.gradle.kts` file check that the version numbers of the libraries match.
4. Run docker compose

Building the jar takes some time (1-2 minutes).

### Build yourself
If you do not want to build the code everytime, you can build the code and use the `Dockerfile_No_Build` instead.
Rename it to `Dockerfile`.

You can also use the `de.featureide.service.jar`.

To start the service run docker compose.

## How to Use

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