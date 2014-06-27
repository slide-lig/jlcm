# jLCM

A Java implementation of the LCM (Linear Closed itemsets Miner) algorithm, as proposed by T.Uno & H.Arimura. It is multi-threaded, as proposed by Négrevergne et al., hence the name of its main class: PLCM.

Reference papers:

* "An efficient algorithm for enumerating closed patterns in transaction 
databases" by T. Uno, T. Asai, Y. Uchida and H. Arimura, in Discovery Science, 
2004
* "Discovering closed frequent itemsets on multicore: Parallelizing computations 
and optimizing memory accesses" by B. Negrevergne, A. Termier, J-F. Mehaut, 
and T. Uno in International Conference on High Performance Computing & 
Simulation, 2010

Please use [Maven](http://maven.apache.org/) to build the program. 

## jLCM as a command-line utility

Download [jLCM-cli's JAR](https://github.com/martinkirch/jlcm-cli/raw/binary/jLCM-cli-1.1-wdeps.jar) and invoke `java -jar jLCM-cli-1.1-wdeps.jar` to show the complete manual. Note that this program's `main` function lives in [a separated project](https://github.com/martinkirch/jlcm-cli/).

This tool uses ASCII files as input: each line represents a transaction. You may find example input files in the [FIMI repository](http://fimi.ua.ac.be/data/), or start with a small one embedded in `src/test/resources` like [50retail.dat](https://github.com/martinkirch/jlcm/raw/master/src/test/resources/50retail.dat).

## jLCM as a library/Maven dependency

Add the following dependency to your `pom.xml`

    <dependency>
        <groupId>fr.liglab.jlcm</groupId>
        <artifactId>jLCM</artifactId>
        <version>1.1</version>
    </dependency>

To perform the mining you will have to instanciate an `ExplorationStep`, a `PatternsCollector` and the main class `PLCM`. Depending on how you want to do the I/O you may have to implement your own `Iterable<TransactionReader>` (for input) and/or `PatternsWriter` (for output).

The [main class of jLCM-cli](https://github.com/martinkirch/jlcm-cli/blob/master/src/main/java/fr/liglab/jlcm/RunPLCM.java) provides an example use of the library.


## License and copyright owners

This work is released under the Apache License 2.0 (see LICENSE).

Copyright 2013,2014 Martin Kirchgessner, Vincent Leroy, Alexandre Termier, 
Sihem Amer-Yahia, Marie-Christine Rousset, Université Joseph Fourier and CNRS.


