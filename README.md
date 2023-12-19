# A01 MediTrack 

## Team

| Number | Name              | User                             | E-mail                              |
| -------|-------------------|----------------------------------| ------------------------------------|
| 95552  | Diogo Silva       | <https://github.com/diogosil01>   | <mailto:diogo.armando.barreiro.da.silva@tecnico.ulisboa.pt>   |
| 97281  | Allan Fernandes   | <https://github.com/doonic>     | <mailto:allancravid@tecnico.ulisboa.pt>     |
| 99330  | Stanislaw Talejko | <https://github.com/stani-s>    | <mailto:stanislaw.talejko@tecnico.ulisboa.pt> |

<img src='img/diogo.png' height='150'> <img src='img/allan.jpg' height='150'> <img src='img/stas.jpg' height='150'>


## Contents

This repository contains documentation and source code for the *Network and Computer Security (SIRS)* project.

The [REPORT](REPORT.md) document provides a detailed overview of the key technical decisions and various components of the implemented project.
It offers insights into the rationale behind these choices, the project's architecture, and the impact of these decisions on the overall functionality and performance of the system.

This document presents installation and demonstration instructions.

*(adapt all of the following to your project, changing to the specific Linux distributions, programming languages, libraries, etc)*

## Installation

To see the project in action, it is necessary to setup a virtual environment, with N networks and M machines.  

The following diagram shows the networks and machines:

*(include a text-based or an image-based diagram)*

### Prerequisites

All the virtual machines are based on: Linux 64-bit, Kali 2023.3  
We create our whole infrastructure using Vagrant.

### Instructions to run the project
```
git clone https://github.com/tecnico-sec/a01-diogo-allan-stanislaw.git
cd a01-diogo-allan-stanislaw/scripts
vagrant up 
// This may take some minutes
```

After it completes, we need to run in VM VirtualBox the 3 virtual machines we are going to use, server, db and client.
The login is made using "vagrant" as username and password.

For each machine, there is an initialization script inside scripts folder with the machine name, with prefix `init-` and suffix `.sh`, that makes all required configurations in the a clean machine.

Next we have custom instructions for each machine.

#### Machine DB

This machine runs ...

*(describe what kind of software runs on this machine, e.g. a database server (PostgreSQL 16.1))*

To verify:

```sh
$ setup command
```

*(replace with actual commands)*

To test:

```sh
$ test command
```

*(replace with actual commands)*

The expected results are ...

*(explain what is supposed to happen if all goes well)*

If you receive the following message ... then ...

*(explain how to fix some known problem)*

#### Machine ...

*(similar content structure as Machine 1)*

## Demonstration

Now that all the networks and machines are up and running, ...

*(give a tour of the best features of the application; add screenshots when relevant)*

```sh
$ demo command
```

*(replace with actual commands)*

*(IMPORTANT: show evidence of the security mechanisms in action; show message payloads, print relevant messages, perform simulated attacks to show the defenses in action, etc.)*

This concludes the demonstration.

## Additional Information

### Links to Used Tools and Libraries

- [Java 11.0.16.1](https://openjdk.java.net/)
- [Maven 3.9.5](https://maven.apache.org/)
- ...

### Versioning

We use [SemVer](http://semver.org/) for versioning.  

### License

This project is licensed under the MIT License - see the [LICENSE.txt](LICENSE.txt) for details.

*(switch to another license, or no license, as you see fit)*

----
END OF README
