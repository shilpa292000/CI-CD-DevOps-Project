Pre-Requisite

1 Vagrant

2 Virtual Box

3 IDE(Sublime, Atom, or Visual Studio)

4 Kubernetes Cluster (We need kubernetes cluster already setup - If you do not have kubernetes cluster then please do follow this lab session for setting it up - Kubernetes Cluster Setup)

1.## Understanding the complete setup
Alright so before we start setting up our CI/CD pipeline I am assuming you have setup your kuernetes cluster already.

As you can see in the sketch we will have three servers(virtual machine) -

~ Jenkins Server

~ Kubernetes Master

~ Kubernetes Worker

![jenkins-k8smaster-k8sworker-setup (1)](https://user-images.githubusercontent.com/93249038/218738862-fc97b321-5135-473d-a79b-0ac6ae82c175.png)


2. ## Where does Github and Docker Hub fits in the CI/CD

![jenkins-ci-cd-flow](https://user-images.githubusercontent.com/93249038/218739298-3901078d-c298-44a8-946e-977db6e60a87.png)

Step 1 - Checkin/Push your code to GitHub

Step 2 - Pull your code from GitHub into your Jenkins server

Step 3 - Use Gradle/Maven build tool for building the artifacts

Step 4 - Create Docker image

Step 5 - Push your latest Docker image to DockerHub

Step 6 - Pull the latest image from DockerHub into jenkins.

Step 7 - Then use k8s-spring-boot-deployment.yml to deploy your application inside your kubernetes cluster.

3. Install Jenkins on your jenkinsserver
Alright lets start with Jenkins installation on our jenkinsserver


3.1 Start vagrant box
If your vagrant box is not running then start up your vagrant box -

vagrant up 
BASH

3.2 Log into vagrant
After starting your vagrant box you can login into it -

vagrant ssh jenkinsserver
BASH

3.3 Update ubuntu repositories
Update the repositories of your ubuntu -

sudo apt update
BASH

3.4 Install Java
You also need Java as pre-requisite for installing jenkins, so lets first install java -

sudo apt install openjdk-8-jdk
BASH
Verify your java installation by running the following command

java -version
BASH
It should return with java version

openjdk version "1.8.0_265"
OpenJDK Runtime Environment (build 1.8.0_265-8u265-b01-0ubuntu2~18.04-b01)
OpenJDK 64-Bit Server VM (build 25.265-b01, mixed mode) 
BASH

3.5 Install Jenkins
Now we can install the jenkins on our Ubuntu server, use the following command to install it -

wget -q -O - https://pkg.jenkins.io/debian-stable/jenkins.io.key | sudo apt-key add -
BASH
sudo sh -c 'echo deb https://pkg.jenkins.io/debian-stable binary/ > \
    /etc/apt/sources.list.d/jenkins.list'
BASH
sudo apt-get update
BASH
sudo apt-get install jenkins
BASH
(Note : For more installation options for MacOS, Windows, CentOS or RedHat please refer to official documentation for jenkins installation)


3.6 Verify Jenkins installation
After installing Jenkins you can verify the jenkins installation by accessing the initial login page of the jenkins.

Since we have installed Jenkins on virtual machine with IP 100.0.0.1, so you can access using following URL

http://100.0.0.1:8080/
BASH
And if you have installed the jenkins correctly then you should be able to see the following initial login page of Jenkins


Unlock jenkins page after jenkins installation


4. Fetch Default jenkins password
As you can see you need to provide Default jenkins(initialAdminPassword) administrator password.

So the question is - How to find Default Jenkins password(initialAdminPassword)?

There are two ways find Default Jenkins password-

Using the Jenkins Logs file located at - /var/log/jenkins/jenkins.log
Using the Jenkins secret - /var/lib/jenkins/secrets/initialAdminPassword
For Option 1 - Run the following command -

cat /var/log/jenkins/jenkins.log
BASH
And then look for following -

*************************************************************
*************************************************************
*************************************************************

Jenkins initial setup is required. An admin user has been created and a password generated.
Please use the following password to proceed to installation:

e0d325fb18a64797bd81dcb77e865237

This may also be found at: /var/lib/jenkins/secrets/initialAdminPassword

*************************************************************
*************************************************************
*************************************************************
BASH
For Option 2 - Run the following command -

cat /var/lib/jenkins/secrets/initialAdminPassword
BASH
It should return you back the password

e0d325fb18a64797bd81dcb77e865237
BASH


Customize Jenkins and install suggested plugin
After you have entered your initialAdminPassword you should be able to see the following Customize jenkins page -


Customize Jenkins page and install suggested plugins
Select install suggested plugin and then it should install all the default plugins which is required for running Jenkins.


After successful login setup username and password
After you have installed all the suggested default plugins, it will prompt you for setting up username and password -


Jenkins Create First Admin User
Set desired username and password (In this lab session I am going to keep Username - admin and password - admin)

After that it will ask for Jenkins URL and I would recommend you to keep the default one -


Jenkins Create First Admin User
Now you jenkins is ready to use -


Jenkins is Ready

5. Jenkins - Install "SSH Pipeline Steps" plugin and "Gradle"

5.1 SSH Pipeline Steps
Moving ahead we need to install one more plugin SSH Pipeline Steps which we are going to use for SSH into k8smaster and k8sworker server.

For installing plugin please goto - Manage Jenkins -> Manage Plugin -> Available then in the search box type SSH Pipeline Steps.

Select the plugin and install it without restart.


5.2 Setup Gradle
For this lab session we are going to use Spring Boot Application, so for that we need Gradle as our build tool.

To setup Gradle Goto - Manage Jenkins -> Global Tool Configuration -> Gradle

Click on Add Grdle and then enter name default.

After that click on the checkbox - Install Automatically and from the drop down Install from Gradle.org select latest version.

Refer to the following screenshot


Set up Gradle in Jenkins Global Tool Configuration
Now we have installed Jenkins and all the required plugins which is needed for this lab session.


6. Install Docker on jenkinsserver
Alright now we need to install Docker on jenkinsserver since we need to push our docker image to DockerHub.

(Note - We are not installing any plugins for jenkins, till the step no 5 we have completed the jenkins and jenkins plugin setup)


6.1 Logback into jenkinsserver
vagrant ssh jenkinsserver
BASH

6.2 Install Docker
Use the following command to install the Docker -

sudo apt install docker.io
BASH
After installing docker verify the installation with following docker command -

Client:
 Version:           19.03.6
 API version:       1.40
 Go version:        go1.12.17
 Git commit:        369ce74a3c
 Built:             Wed Oct 14 19:00:27 2020
 OS/Arch:           linux/amd64
 Experimental:      false 
BASH

6.3 Add Current User to Docker Group
The next step you need to do is to add the current user to Docker Group to avoid the error - Got permission denied while trying to connect to the Docker daemon socket at unix:///var/run/docker.sock (For more detail refer to this post)

Use the following command for adding current user to Docker Group

sudo usermod -aG docker $USER 
BASH

6.4 Add Jenkins User to Docker Group
Similar to previous step we also need to add Jenkins User to Docker Group, so that Jenkins can use Docker for building and pusing the docker images.

Run the following command to add Jenkins user to Docker Group

sudo usermod -aG docker jenkins 
BASH

7. Take a look at Spring Boot Application
Note - In this step we are using Spring Boot Application but if you have different application like NodeJs, Angular etc than you can use that application. Rest of the steps are going to be the same.

Now its time for you to look at our application which we are going to deploy inside kubernetes cluster using Jenkins Pipeline.

To keep the lab session simple we are going to write Hello World rest webservice using Spring Boot.

Here is Java class of rest end point -

@RestController
public class JhooqDockerDemoController {

    @GetMapping("/hello")
    public String hello() {
        return "Docker Demo - Hello Jhooq";
    }
}
JAVA
You can clone the code repo from - Source Code

The source code also include the Dockerfile for building the dockerimage -

FROM openjdk:11
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
DOCKERFILE

8. Write the Pipeline script
To make this lab session more simple I have divided CI/CD Jenkins Pipeline script into 11 steps


8.1 Create Jenkins Pipeline
The first step for you would be to create a pipeline.

Goto : Jenkins -> New Items
Enter an item name : Jhooq-SpringBoot
Select Pipeline
Click Ok

8.2 Clone the Git Repo
The first principle of the CI/CD pipeline is to clone/checkout the source code, using the same principle we are going to clone the GIT Repo inside Jenkins

stage("Git Clone"){

        git credentialsId: 'GIT_HUB_CREDENTIALS', url: 'https://github.com/rahulwagh/spring-boot-docker.git'
    }
BASH

8.3 Jenkins store git credentials
As you know we cannot store plain text password inside jenkins scripts, so we need to store it somewhere securely.

Jenkins Manage Credential provides very elegant way to store GitHub Username and Password.

Goto : Jenkins -> Manage Jenkins -> Manage Credentials


jenkins store git credentials
After that Goto : Stores scoped to Jenkins -> global


jenkins store git credentials
Then select Username with Password


Jenkins Global Credential GitHub App
And then input you GitHub Username and Password. But always remember the ID


Jenkins Global Credential GitHub App
Note - Keep the ID somewhere store so that you remember - GIT_HUB_CREDENTIALS


8.4 Build the Spring Boot Application
Next step would be to build the Spring Boot Application Using Gradle

stage('Gradle Build') {

       sh './gradlew build'

    } 
BASH

8.5 Build Docker image and tag it
After successful Gradle Build we are going to build Docker Image and after that I am going to tag it with the name jhooq-docker-demo

stage("Docker build"){
        sh 'docker version'
        sh 'docker build -t jhooq-docker-demo .'
        sh 'docker image list'
        sh 'docker tag jhooq-docker-demo rahulwagh17/jhooq-docker-demo:jhooq-docker-demo'
    } 
BASH

8.6 Jenkins store DockerHub credentials
For storing DockerHub Credentials you need to GOTO: Jenkins -> Manage Jenkins -> Manage Credentials -> Stored scoped to jenkins -> global -> Add Credentials

From the Kind dropdown please select Secret text

Secret - Type in the DockerHub Password
ID - DOCKER_HUB_PASSWORD
Description - Docker Hub password

Jenkins store DockerHub credentials

8.7 Docker Login via CLI
Since I am working inside Jenkins so every step I perform I need to write pipeline script. Now after building and tagging the Docker Image we need to push it to the DockerHub. But before you push to DockerHub you need to authenticate yourself via CLI(command line interface) using docker login

So here is the pipeline step for Docker Login

stage("Docker Login"){
        withCredentials([string(credentialsId: 'DOCKER_HUB_PASSWORD', variable: 'PASSWORD')]) {
            sh 'docker login -u rahulwagh17 -p $PASSWORD'
        }
    } 
BASH
$DOCKER_HUB_PASSWORD - Since I cann't disclose my DockerHub password, so I stored my DockerHub Password into Jenkins Manage Jenkins and assigned the ID $DOCKER_HUB_PASSWORD


8.8 Push Docker Image into DockerHub
After successful Docker login now we need to push the image to DockerHub

stage("Push Image to Docker Hub"){
        sh 'docker push  rahulwagh17/jhooq-docker-demo:jhooq-docker-demo'
    }
BASH

8.9 SSH Into k8smaster server
If you remember we have installed SSH Pipeline Steps in step no - 5, now we are going to use that plugin to SSH into k8smaster server

stage("SSH Into k8s Server") {
        def remote = [:]
        remote.name = 'K8S master'
        remote.host = '100.0.0.2'
        remote.user = 'vagrant'
        remote.password = 'vagrant'
        remote.allowAnyHosts = true
} 
BASH

8.10 Copy k8s-spring-boot-deployment.yml to k8smaster server
After successful login copy k8s-spring-boot-deployment.yml into k8smaster server

stage('Put k8s-spring-boot-deployment.yml onto k8smaster') {
            sshPut remote: remote, from: 'k8s-spring-boot-deployment.yml', into: '.'
        } 
BASH

8.11 Create kubernetes deployment and service
Apply the k8s-spring-boot-deployment.yml which will eventually -

Create deployment with name - jhooq-springboot
Expose service on NodePort
stage('Deploy spring boot') {
          sshCommand remote: remote, command: "kubectl apply -f k8s-spring-boot-deployment.yml"
        }
BASH
So here is the final complete pipeline script for my CI/CD Jenkins kubernetes pipeline

node {

    stage("Git Clone"){

        git credentialsId: 'GIT_CREDENTIALS', url: 'https://github.com/rahulwagh/spring-boot-docker.git'
    }

     stage('Gradle Build') {

       sh './gradlew build'

    }

    stage("Docker build"){
        sh 'docker version'
        sh 'docker build -t jhooq-docker-demo .'
        sh 'docker image list'
        sh 'docker tag jhooq-docker-demo rahulwagh17/jhooq-docker-demo:jhooq-docker-demo'
    }

    withCredentials([string(credentialsId: 'DOCKER_HUB_PASSWORD', variable: 'PASSWORD')]) {
        sh 'docker login -u rahulwagh17 -p $PASSWORD'
    }

    stage("Push Image to Docker Hub"){
        sh 'docker push  rahulwagh17/jhooq-docker-demo:jhooq-docker-demo'
    }

    stage("SSH Into k8s Server") {
        def remote = [:]
        remote.name = 'K8S master'
        remote.host = '100.0.0.2'
        remote.user = 'vagrant'
        remote.password = 'vagrant'
        remote.allowAnyHosts = true

        stage('Put k8s-spring-boot-deployment.yml onto k8smaster') {
            sshPut remote: remote, from: 'k8s-spring-boot-deployment.yml', into: '.'
        }

        stage('Deploy spring boot') {
          sshCommand remote: remote, command: "kubectl apply -f k8s-spring-boot-deployment.yml"
        }
    }

}
 


