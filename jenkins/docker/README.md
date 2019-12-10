# Dockerized Jenkins with ability to run docker containers (docker-in-docker)
This folder contains everything needed to run Jenkins (the latest monthly build) in a Docker container, with the ability to run an additional docker container inside the Jenkins container. In other words, a docker container running inside another docker container (yes, it's confusing). The provided Dockerfile and the instructions below provide an easy way to get up and running. For this reason, configurable parameters has been kept to a minimum. It is therefore advised to mainly use this Dockerfile for testing purposes, and not in a production environment.

## 1. Docker version compatibility
Describes the environment, docker application and docker version that the docker functionality in this folder has been tested with.

|environment|application|version|tested|working|date tested|notes|
|---|---|---|---|---|---|---|
| Windows 10 Pro | Docker Desktop Community | 18.09.02 | yes | yes | 2019-04-24 | - |

## 2. Directory structure

| path  | description   |
|---|---|
| README.md | This file |
| Dockerfile | The file used to generate a docker image |

## 3. Get up and running
Start Windows Powershell and navigate to this directory. Then execute the commands below to start a docker container running Jenkins.

1. Create a docker image named `jenkins-image` from the Dockerfile: 
	* `docker build -t jenkins-image .`
2. Create and run a docker container named `jenkins-container` based on the `jenkins-image` from step 1. The `-v` flag is used to create a volume on the host to persist data. The `-e` flag is used to specify an environment variable. Here, the `TZ` variable specifies the timezone for the server. The `-p` option specifies a host port followed by the mapped port in the docker container. The host port can be changed to your liking, but here we default to the ports 8080 and 50000.
	* `docker run --name jenkins-container -p 8080:8080 -p 50000:50000 -e TZ=Europe/Stockholm -v /var/run/docker.sock:/var/run/docker.sock -v jenkins_home:/var/jenkins_home jenkins-image`
3. The first time you run the container the initial admin password to Jenkins will be generated and printed in the log files that you should see in the output. If you don't find it manually, you can open a new Powershell window and run the following command to print the password:
	* `docker exec jenkins-container cat /var/jenkins_home/secrets/initialAdminPassword`
4. Navigate your browser to `localhost:8080` and enter the password when Jenkins prompts you.
5. Install any Jenkins plugins that you wish to use.
6. You should now be able to run Jenkins and docker containers in your Jenkins jobs.

## 4. Tips
1. To copy data from a docker container to your local computer, use the `docker cp CONTAINER:SRC_PATH DEST_PATH` command. For example, to copy a Jenkins job's config file, run:
	* `docker cp jenkins-container:/var/jenkins_home/jobs/my-job/config.xml C:\Jenkins\jobs\my-job`
2. To view the logs of a docker container, run the `docker logs <CONTAINER_NAME>` command.
3. To enter the docker container to perform commands, run:
	* `docker exec -it -u root jenkins-container bash`
4. Any configurations and data are stored in the `/var/jenkins_home` folder in the container. If you want to remove all the data you need to remove the volume created in the run step, by running:
	* `docker volume rm jenkins_home`
5. To run a docker container in detached mode (in the background), add the `-d` flag when running the docker container.

## 5. References
1. The Dockerfile and instructions above are based on this blog post by Miiro Juuso: https://getintodevops.com/blog/the-simple-way-to-run-docker-in-docker-for-ci
2. Guide to installing Docker on Windows: https://runnable.com/docker/install-docker-on-windows-10