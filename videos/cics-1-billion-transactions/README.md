## How fast can you process SMF Data from 1 billion CICS transactions?

These are  sample programs for the video: [How fast can you process SMF data from 1 billion CICS transactions?](https://www.youtube.com/watch?v=5bw58tfiZuk)

### [Cics1B.java](./src/main/java/Cics1B.java)

Process CICS monitoring performance records from an input file and produce a report.

### [Cics1BParallel.java](./src/main/java/Cics1BParallel.java)

Process CICS monitoring performance records from an input file using Java Streams with parallel threads and produce a report.

### [GenerateInput.java](./src/main/java/GenerateInput.java)

Generate a file containing the required number of CICS SMF records by repeatedly reading an input file and writing it to the output file.

This is used to generate the input if a large enough file is not available. 

As long as the original file is large enough, repeating the data is unlikely to affect the timing of the test programs.