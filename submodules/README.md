# Why Do This?

These submodules are not depended-upon directly in this repository (in any build or runtime process). Instead, in order to simplify the build systems, all dependant artifacts are pre-built and manually copied over to the [artifacts directory](https://github.com/aionnetwork/tetryon-bench/tree/master/artifacts). 

Submodules are maintained in this folder to *document* the source repository "pointer" at which the dependant artifacts were generated. 

* `avm` generates `artifacts/avm/*`
* `bn128-jni` generates `artifacts/libbn_jni.so`
* `zokrates` generates `artifacts/zokrates`
* `mdkt-compiler` generates `artifacts/mdkt-compiler-1.4.0.jar`

This enables users to regenerate platform-dependant artifacts for their target platform (e.g. the Alt-Bn 128 shared library or the ZoKrates binary).  