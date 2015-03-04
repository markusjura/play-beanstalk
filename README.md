# Deploy Play app on AWS Beanstalk with Docker and Jenkins

It's possible to deploy [Play](https://playframework.com/) applications on [AWS Beanstalk](http://aws.amazon.com/elasticbeanstalk/).
This project shows how to automatically deploy the latest Play version on AWS Beanstalk as an docker image via Jenkins.

> **Note:** In order to deploy this Play application with automatically with Jenkins you need to fork this project. This is necessary in order to configure the Jenkins service hook for your Github project.

## Prerequisites
### Dev environment
- OSX 10.10 or above
- Docker
- SBT 0.13.5 or above
- [Configured AWS keypair](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html)

### General
- AWS Account
- Dockerhub account

## Enable SBT Native Packager Docker Plugin
The [sbt native packager](https://github.com/sbt/sbt-native-packager) provides experimental docker support. This means we can create a docker files during our Play staging process.
Our sample app already uses this feature, take a look at the `build.sbt`.
For more information check out the [sbt native packager documentation](https://github.com/sbt/sbt-native-packager#experimental-docker-support).

## Install additional software on development environment
Before we are using Jenkins to deploy our Play application automatically after each push to Github we are gonna deploy
our Play application from our development environment. Do do that we need to install the following tools.

### Install EB CLI
The [Elastic Beanstalk command line interface](http://aws.amazon.com/cli/) helps us to create a Beanstalk application.
Follow [this](http://docs.aws.amazon.com/elasticbeanstalk/latest/dg/eb-cli3-getting-set-up.html) guide to install the EB CLI.

### Instal AWS CLI
We will use the [AWS command line interface](http://aws.amazon.com/cli/) to:
- Upload our Play application to an AWS S3 bucket
- Create a new environment version on Beanstalk
- Deploy the Play application to Beanstalk.

Follow [this](http://docs.aws.amazon.com/cli/latest/userguide/installing.html) guide to install the AWS CLI.

### Configure AWS CLI
Follow [this](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html) guide to initially configure the AWS CLI.
- AWS Region: Specify the region where your S3 bucket and your Beanstalk instance is located
- Output Format: `json`

## Setup S3 Bucket
A S3 bucket is a good choice to host the different version of our Play application. It is secure, accessible from the dev environment, from Jenkins and Beanstalk.

Our Play application is based on another docker image `dockerfile/java`. This base image contains Ubuntu and OpenJDK 7 and is hosted on Dockerhub.
During the deployment Beanstalk will build and run the docker image. In order to do that it needs to have access to our Dockerhub account.
If you have [configured your Dockerhub account](http://docs.docker.com/userguide/dockerrepos/) locally you will find a file called `~/.dockercfg` which contains your Dockerhub credentials.
Before we can add this file to S3 we need to create a copy with a different name because S3 is not allowing files starting with a dot:
1. On your dev environment create a new copy of your `.dockercfg` file:
```
cp ~/.dockercfg ~/dockercfg
```
1. In your S3 bucket create a folder `docker`
2. In this folder upload the `dockercfg` file

## Stage app and deploy on Beanstalk (via dev environment)
### Create Beanstalk application and environment
First let's create a beanstalk application for our Play app called `play-beanstalk`:
```
cd play-beanstalk
eb init
```

The `eb init` command will ask you several questions:
1. Select default region: [your AWS region]
2. Select an application to use: Create new Application
3. Enter Application Name: [Your beanstalk application name]
4. Select a platform: Docker
5. Do you want to set up SSH for your instances: y
6. Select a keypair: [Your AWS keypair]

At the end a new Beanstalk application has been created. A beanstalk application is a container which can hold multiple environments. In our case we will create one environment called `play-beanstalk-prod`:
```
eb create -i t2.small play-beanstalk-prod
```

> **Note:** You should use at least a `t2.small` instance to allocate enough memory for docker and the Play application.

This will create a beanstalk environment with and EC2 instance `t2.small`, an elastic load balancer and security groups.
Also AWS tried to deploy an application directly which will fail. Therefor the *Environment health* will turn to *RED* because we haven't specified a `Dockerfile` and `Dockerrun.aws.json` file.
This is fine for the moment, we will create these files and deploy our first application version later on.

You can leave the *AWS print status terminal* with `CTRL + C` after you have seen something like:
```
WARNING: Environment health has transitioned from YELLOW to RED
```

### Stage Play application + Create docker image
Thanks to the sbt native packager docker plugin we can create docker files during our Play stage process:
```
sbt clean docker:stage
```

This creates a target/docker directory containing the Dockerfile based on our `build.sbt` configuration setting and the docker files itself.

### Prepare Docker Image for Beanstalk
Now we need to create a `Dockerrun.aws.json` file:
```
cd target/docker
touch Dockerrun.aws.json
```

This file contains all the necessary information so that Beanstalk can deploy our docker image:
- S3 bucket name which will contain the docker image. Specify the S3 bucket in which you want to host your docker images
- Docker config file which we have added before to S3 that contains our Dockerhub credentials. These are necessary so that Beanstalk can download the `dockerfile/java` docker image which we are using as a base image in our Play application.
- Container port

Add the following content to this file:
```
{
 "AWSEBDockerrunVersion": "1",
 "Authentication": {
   "Bucket": "YOUR.S3.BUCKET",
   "Key": "docker/dockercfg"
 },
 "Ports": [
   {
     "ContainerPort": "9000"
   }
 ]
}
```

During the next steps we will upload our Docker files to the S3 bucket. Beanstalk can either deploy docker images based on a Dockerfile or based on a zip file which contains the Dockerfile. In our case we want to deploy a zip file because we have additionally created a `Dockerrun.aws.json` file:
```
zip -r ../../build-play-beanstalk-1.0.zip .
cd ../..
```

### Upload zip to S3 and deploy to Beanstalk
Finally let's upload the created zip file to S3. Then we will create a new Beanstalk application version based and update the Beanstalk environment with this version:
```
aws s3api put-object --bucket YOUR_S3_BUCKET --key build-play-beanstalk-1.0.zip --body build-play-beanstalk-1.0.zip
aws elasticbeanstalk create-application-version --application-name play-beanstalk --version-label 1.0 --source-bundle S3Bucket=YOUR_S3_BUCKET,S3Key=build-play-beanstalk-1.0.zip
```

This deploys our Play application to AWS Beanstalk. The app should be now accessible in the internet.

## Automate Deployment with Jenkins
We can easily automate this process with Jenkins so that whenever we push our changes to Github, Jenkins will pull this new version and deploy it to Beanstalk automatically.

### Prerequisites Jenkins Server
- EC2 Instance with Amazon Linux
- Jenkins with [Github Plugin](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+Plugin)
- Java 7 or above
- AWS CLI

### Configure Jenkins project
1. Add your Github credentials as gloal credentials:
   `Jenkins` > `Credentials` > `Global credentials` > `Add Credentials`
   - Kind: `Username with password`
   - Scope: `Global`
   - Username: `[YOUR GITHUB USERNAME]`
   - Password: `[YOUR GITHUB PASSWORD]`
2. Create a new `Freestyle project` with the name `play-beanstalk-prod`
3. Select `Git` in `Source Code Management`
   - Specify the https repository URL of your Github project, e.g. `https://github.com/markusjura/play-beanstalk.git`
   - Select your added credentials in `Credentials`
   - Branches to build: `*/master`
4. Check in `Build Triggers` the option `Build when a change is pushed to GitHub`
5. In the `Build` section add a `Execute shell` build step to build the Play application and create the docker files:
   ```
   # Test and build app
   sbt clean compile docker:stage
   ```
6. Add another `Execute shell` build step to add the `Dockerrun.aws.json`, zip the content, upload the zip to S3 and deploy the application:
    ```
    # Docker commands
    # ~~~
    # Retrieve current version from build.sbt
    SBT_APP_VERSION=`less build.sbt | grep version | awk '{print $3}' | sed s/\"//g`
    S3_BUCKET=YOUR_BUCKET_NAME
    APP_VERSION="$SBT_APP_VERSION-$BUILD_NUMBER-$GIT_COMMIT"
    APP_ZIP="build-$JOB_NAME-$APP_VERSION.zip"
    BEANSTALK_APP="play-beanstalk"
    BEANSTALK_ENV="play-beanstalk-prod"

    cd target/docker

    # Add Dockerrun.aws.json to app
    cp $JENKINS_HOME/beanstalk/Dockerrun.aws.json .

    # Zip content based on commit id
    zip -r ../../$APP_ZIP .

    # Upload zip to S3 and deploy to Beanstalk
    cd ../..

    aws s3api put-object --bucket $S3_BUCKET --key $APP_ZIP --body $APP_ZIP

    aws elasticbeanstalk create-application-version --application-name $BEANSTALK_APP --version-label $APP_VERSION --source-bundle S3Bucket=$S3_BUCKET,S3Key=$APP_ZIP

    aws elasticbeanstalk update-environment --environment-name $BEANSTALK_ENV --version-label $APP_VERSION
    ```
7. Save the project

### Add `Dockerrun.aws.json` to Jenkins server
Now add the `Dockerrun.aws.json` file which have created on our development environment to the EC2 Jekins instance. In our case we add the `Dockerrun.aws.json` file into `$JENKINS_HOME/beanstalk/Dockerrun.aws.json`.

### Add Jenkins service to Github
Finally Jenkins should deploy the Play application whenever we push something to origin master. Therefor we need to add the Jenkins server as a service to our github project:

1. Go to your Github project, e.g. `https://github.com/markusjura/play-beanstalk`
2. Navigate to `Settings` > `Webhooks & Services`
3. Add a new `Jenkins (Git plugin)` service
4. Specify your the URL to the Jenkins server, e.g. `http://username:password@54.173.13.157:8080/github-webhook/`

> **Note:** In this simple case is the Jenkins server publicly available. We are transferring the Jenkins username and password directly in the URL.

> **Note:** You need to have access to add the service to your Github project. As a quick test you could fork this project to your Github account.



### Test it out
Change some code in the Play application and push the changes to Github origin master. Afterwards the Jenkins project should pick up the changes and depoy the app to AWS beanstalk.