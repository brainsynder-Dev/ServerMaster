<a name="readme-top"></a>

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![GNU License][license-shield]][license-url]


<br />
<div align="center">
  <a href="https://github.com/brainsynder-Dev/ServerMaster">
    <img src="https://github.com/brainsynder-Dev/ServerMaster/blob/master/src/main/resources/org/bsdevelopment/serverapp/icon.png?raw=true" alt="Logo" width="250" height="250">
  </a>

<h3 align="center">Server Master</h3>

  <p align="center">
    A server console to run different server versions and types all off of 1 server folder
    <br />
    <a href="https://github.com/brainsynder-Dev/ServerMaster/tree/master/src/main/java/org/bsdevelopment/serverapp"><strong>Explore the Code »</strong></a>
    <br />
    <br />
    <a href="https://github.com/brainsynder-Dev/ServerMaster/issues">Report Bug</a>
    ·
    <a href="https://github.com/brainsynder-Dev/ServerMaster/issues">Request Feature</a>
    ·
    <a href="https://github.com/brainsynder-Dev/ServerMaster/pulls">Pull Request</a>
  </p>
</div>



<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li><a href="#getting-started">Getting Started</a></li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>



<!-- ABOUT THE PROJECT -->
## About The Project

![Server Master][product-screenshot]

[Server Master](https://github.com/brainsynder-Dev/ServerMaster/) started off as a batch file used to run different server types `(such as Spigot, Paper, Etc)` and server versions `(1.8, 1.19.4, Etc)`

After using the batch file for a while I wanted to make something better and easily expandable, as the current batch file you had to hard code the server type and version.

#### Who Could Use This?
Well anyone could technically use this, I personally was aiming this for plugin developers who have to test their plugins on multiple server types and versions of Minecraft but anyone who is able to use it.

<p align="right">(<a href="#readme-top">back to top</a>)</p>



### Built With

* ![Java][Java]
* ![JavaFX][JavaFX]
* ![IntelliJ][IntelliJ]

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- GETTING STARTED -->
## Getting Started

#### Prerequisites

* Java 17
* At least 1 server jar (It is useless without it)

#### Installation

1. Grab the latest [release](https://github.com/brainsynder-Dev/ServerMaster/releases)
2. Put the `ServerMaster.jar` in a folder that you want the server to be run from
3. Once you have the `ServerMaster.jar` in the folder, proceed to add any server jars you want to use
4. When adding the server jar files, ensure they follow this pattern: `ServerType-MCVersion.jar` (Example: `spigot-1.18.2.jar`)
5. Run the server at least once `(If the folder was empty)` after it was run, configure all the main settings for the server

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- USAGE EXAMPLES -->
## Usage

To start a server, All you have to do is `Right Click` in the Application, and it will open a context menu that you can select what type of server and also what version.

Once you have a server started you can also re-open the context menu to `Stop` the server and also `Force Stop` the server

* Stop - Will auto run the `/stop` command in the console saving all the data
* Force Stop - Should only be used in emergencies as it will NOT save anything, it does that it says it forcibly stops the server no matter what...
```manifest
Note: Closing the Java Application has the same affects as `Force Stopping` the server
```

Console commands can be entered into the TextField at the bottom and you can simply press your `Enter` key to send the command 

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- ROADMAP -->
## Roadmap

- [ ] Cleanup the code `(Its a bit of a mess because it was just completed :P)`
- [x] Theme Selection
- [x] Server Port Setting
- [x] EULA set to true by default
- [ ] Console Colorization `(Really need to figure this out...)`
- [ ] Server Jar Installer

Have an idea? Create an enhancement/feature [issue](https://github.com/github_username/repo_name/issues) 

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- CONTRIBUTING -->
## Contributing

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a [Pull Request](https://github.com/brainsynder-Dev/ServerMaster/pulls)

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- LICENSE -->
## License

Distributed under the GPL-3.0 License. See [LICENSE](https://github.com/brainsynder-Dev/ServerMaster/blob/master/LICENSE) for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- CONTACT -->
## Contact

Brian - [Discord Server](https://discord.bsdevelopment.org/)

Project Link: [https://github.com/brainsynder-Dev/ServerMaster](https://github.com/brainsynder-Dev/ServerMaster)

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

None yet...

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/brainsynder-Dev/ServerMaster.svg?style=for-the-badge
[contributors-url]: https://github.com/brainsynder-Dev/ServerMaster/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/brainsynder-Dev/ServerMaster.svg?style=for-the-badge
[forks-url]: https://github.com/brainsynder-Dev/ServerMaster/network/members
[stars-shield]: https://img.shields.io/github/stars/brainsynder-Dev/ServerMaster.svg?style=for-the-badge
[stars-url]: https://github.com/brainsynder-Dev/ServerMaster/stargazers
[issues-shield]: https://img.shields.io/github/issues/brainsynder-Dev/ServerMaster.svg?style=for-the-badge
[issues-url]: https://github.com/brainsynder-Dev/ServerMaster/issues
[license-shield]: https://img.shields.io/github/license/brainsynder-Dev/ServerMaster.svg?style=for-the-badge
[license-url]: https://github.com/brainsynder-Dev/ServerMaster/blob/master/LICENSE
[product-screenshot]: https://i.imgur.com/8xU3aTx.png

[IntelliJ]: https://img.shields.io/badge/IntelliJIDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white
[Java]: https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white
[JavaFX]: https://img.shields.io/badge/javafx-%239999FF.svg?style=for-the-badge&logoColor=white