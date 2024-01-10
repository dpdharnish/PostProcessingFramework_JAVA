# PostProcessingFramework_JAVA
## The major objective of the code 
### -> image analysis using headspin API
### -> audio analysis using headspin API
### -> text extraction using headspin API 

## Step to run the script
### -> specify all details in the config.properties file
### -> run the PostProcessign class

## structure of framework(maven)
.
├── PostprocessingFramework
│   ├── pom.xml
│   ├── src
│   │   ├── main
│   │   │   ├── java
│   │   │   └── resources
│   │   └── test
│   │       ├── java
│   │       │   └── headspin
│   │       │       └── io
│   │       │           ├── PostProcessing.java
│   │       │           ├── hsapi
│   │       │           │   ├── GlobalVar.java
│   │       │           │   ├── HsApi.java
│   │       │           │   └── PropertyFileReader.java
│   │       │           └── image
│   │       │               ├── Screenshot 2024-01-09 at 13.10.21.png
│   │       │               ├── audio-1_x2Omh5ju.wav
│   │       │               └── headpsin.png
│   │       └── resources
│   │           └── config.properties
│   └── target
│       ├── generated-test-sources
│       │   └── test-annotations
│       └── test-classes
│           ├── config.properties
│           └── headspin
│               └── io
│                   ├── PostProcessing.class
│                   └── hsapi
│                       ├── GlobalVar.class
│                       ├── HsApi$1.class
│                       ├── HsApi$2.class
│                       ├── HsApi.class
│                       └── PropertyFileReader.class
└── README.md
