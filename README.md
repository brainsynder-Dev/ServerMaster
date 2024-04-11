<a name="readme-top"></a>
<br />
<div align="center">

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![GNU License][license-shield]][license-url]  
  <a href="https://github.com/brainsynder-Dev/ServerMaster">
    <img src="https://github.com/brainsynder-Dev/ServerMaster/blob/master/images/icon.png?raw=true" alt="Logo" width="250" height="250">
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

<li style="display: flex; align-items:center; gap:10px"><img src="./src/main/resources/images/readme/java-logo.png" alt="Java" height="32"/>
Java 17</li>
<li style="display: flex; align-items:center; padding-top:10px; gap:10px"><img src="./src/main/resources/images/readme/intellij-logo.png" alt="Java" height="32"/>
<a href="https://www.jetbrains.com/idea/">IntelliJ</a></li>
<li style="display: flex; align-items:center; padding-top:10px; gap:10px"><img src="./src/main/resources/images/readme/vaadin.png" alt="Java" height="32"/>
<a href="https://vaadin.com/flow">Vaadin</a> 24</li>

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- GETTING STARTED -->
## Getting Started

#### Prerequisites

* Java 17
* A Folder for the application
* A Folder for all the server content

#### Installation

1. Grab the latest [release](https://github.com/brainsynder-Dev/ServerMaster/releases)
2. Put the `ServerMaster.jar` (or the `ServerMaster.exe`) in a folder for itself
3. Once you have the `ServerMaster` executable in a folder, proceed to running the app and setting the server folders location
4. Once you have the server folders location set, you can proceed to run our installer if you want a Paper, Purpur, or Pufferfish server
    - Spigot servers need to be manually added to the folder using the formats provided when you enter the `?? jar` command in the app
5. After you have your server jar(s) in your server folder, go ahead and setup your ram and java version for the server(s)
6. After everything is setup go ahead and select which server type, version, and build(if applicable)... then start the server!

<p align="right">(<a href="#readme-top">back to top</a>)</p>


### v1.4.0 New design!
<img src="https://github.com/brainsynder-Dev/ServerMaster/blob/master/src/main/resources/images/app-images/server-installer.png?raw=true" alt="Logo" width="500" height="250">
<img src="https://github.com/brainsynder-Dev/ServerMaster/blob/master/src/main/resources/images/app-images/base-app-image-light.png?raw=true" alt="Logo" width="500" height="250">
<img src="https://github.com/brainsynder-Dev/ServerMaster/blob/master/src/main/resources/images/app-images/base-app-image-dark.png?raw=true" alt="Logo" width="500" height="250">

<!-- USAGE EXAMPLES -->
## Usage

### How to start a server: (Old GUI same principal)
<div align="center">

<img src="https://i.imgur.com/z4vW8SQ.png" alt="Initial view" width="412" height="190">
<span style="color: gray"></br>Initial view of the application</span>
<br><br>

1] Select what type of server you would like to run

<img src="https://imgur.com/JddJsfH.png" alt="server type selection" width="412" height="190">

2] Once you have a server type you would like to start, then you must select what version you would like to run

<img src="https://imgur.com/AthKg9H.png" alt="server version selection" width="412" height="190">

3] After you have selected your server type and version, you can go ahead and click the "Start Server" button

<img src="https://i.imgur.com/LMQwP6P.png" alt="Start server button" width="312" height="190">
</div>

### How to stop the server
After you have finished with the server you can stop it a few ways such as:
- Using the `stop` command
- Clicking the `STOP SERVER` button (will essentially run the `stop` command)
- Clicking the `FORCE STOP` button (This will force the server task to end, resulting in 0 saving)
- Clicking the `X` at the top to close the window

<div align="center">
    <img src="https://i.imgur.com/zY7QdJE.png" alt="Stop/Force Stop buttons" width="400" height="100">
</div>

> [!IMPORTANT]
> Console commands can be entered into the TextField at the bottom, and you can simply press your `Enter` key to send the command 

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- ROADMAP -->
## Roadmap for the future

- [ ] Spigot Installer (Run BuildTools in the same console?)
- [ ] Customization for server.properties
- [ ] Move away from using so many Dialog windows
- [ ] TBD...

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
[product-screenshot]: ./src/main/resources/images/app-images/base-app-image-dark.png

[IntelliJ]: https://img.shields.io/badge/IntelliJIDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white
[Java]: ./src/main/resources/images/java-logo.png
[JavaSwing]: https://img.shields.io/badge/java%20swing-%239999FF.svg?style=for-the-badge&logoColor=white
