# jLCM

A Java implementation of the LCM (Linear Closed itemsets Miner) algorithm, as proposed by T.Uno & H.Arimura. It is multi-threaded, as proposed by Négrevergne et al., hence the name of its main class : PLCM.

Reference papers :

* "An efficient algorithm for enumerating closed patterns in transaction 
databases" by T. Uno, T. Asai, Y. Uchida and H. Arimura, in Discovery Science, 
2004
* "Discovering closed frequent itemsets on multicore: Parallelizing computations 
and optimizing memory accesses" by B. Negrevergne, A. Termier, J-F. Mehaut, 
and T. Uno in International Conference on High Performance Computing & 
Simulation, 2010

## Compiling and running jLCM

Use [Maven](http://maven.apache.org/) to build the program. This should create `jLCM-1.0-jar-with-dependencies.jar`. Run this jar without arguments to have more details about the invocation.

You may find example input files in the [FIMI repository](http://fimi.ua.ac.be/data/).

## License and copyright owners

This work is released under the Apache License 2.0 (see LICENSE).

Copyright 2013,2014 Martin Kirchgessner, Vincent Leroy, Alexandre Termier, 
Sihem Amer-Yahia, Marie-Christine Rousset, Université Joseph Fourier and CNRS.


