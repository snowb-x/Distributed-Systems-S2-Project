# Stage 1
## COMP3100 Assignment

    Student Name: Sandra Trinh

    Student Number: 45915881

    Student Email: sandra.trinh@students.mq.edu.au

This is the code for the stage one code

### Start the code 

Start the server first then run the client code.
Do so in different terminals at the root folder.

  ds-server

        ./ds-server -c ds-sample-config01.xml -v brief -n

  client side

      - compile MyClient.java
        
          javac MyClient.java
      
      - run MyClient.class
      
          java MyClient


### TEST code script.

Using WK6 workshop to test my client code

  First compile the code.

        javac MyClient.java

  Next move a copy of the MyClient.class into the week06TEST folder. In the week06TEST folder run the following command in the terminal.

        ./S1Tests-wk6.sh MyClient.class -n

### Inclass DEMO

  Using S1Demo folder

  First complile the client java code.

      javac MyClient.java

  copy the MyClient.class file into the S1Demo folder.

  Next move to ./S1Demo folder.

  open the termial in this dir. S1Demo folder. Type the following command in the terminal: 

    ./demoS1.sh MyClient.class -n

        PASSED!

# Stage 2

## Schduler used:
-   SendGetAllLarge

    get largest server. in stage 1 used with serverID update to create a large round robin algorithm

-   sendGetFc

    Get first capable server in the list.   

## stage2 testing

in root folder compile the client

    javac MyClient.java

then move class file to testing folder

     mv MyClient.class ./stage2-test

go into the stage2-test folder and start the test

    cd ./stage2-test

    ./stage2-test-x86 "java MyClient -a superClient" -o tt -n

Note to change the test file's permissions

    chmod +x stage2-test-x86
