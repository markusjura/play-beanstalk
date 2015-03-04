# Play + Docker + Jenkins + AWS Beanstalk

It's possible to deploy [Play](https://playframework.com/) application on [AWS Beanstalk](http://aws.amazon.com/elasticbeanstalk/) as an docker image.
This project shows how to automatically deploy the latest Play version on AWS Beanstalk as an docker image via Jenkins.

## Prerequisites
- Dev environment: OSX 10.10 or above
- Docker
- SBT 0.13.5 or above
- AWS Account

## Enable SBT Native Packager Docker Plugin
The [sbt native packager](https://github.com/sbt/sbt-native-packager) provides experimental docker support. This means we can create a docker image during our Play staging process.
Our sample app already uses this feature, take a look at the `build.sbt`.
For more information check out the [sbt native packager documentation](https://github.com/sbt/sbt-native-packager#experimental-docker-support).

## Install additional software on development environment
Before we are using Jenkins to deploy our Play application automatically after each Github push we are gonna deploy
our Play application from our development environment. Do do that we need to install the following tools.

### Install EB CLI
The [Elastic Beanstalk command line interface](http://aws.amazon.com/cli/) helps us to create a Beanstalk environment.
Follow [this](http://docs.aws.amazon.com/elasticbeanstalk/latest/dg/eb-cli3-getting-set-up.html) guide to install the EB CLI.

### Instal AWS CLI
We will use the [AWS command line interface](http://aws.amazon.com/cli/) to upload our docker image to an AWS S3 bucket, create a new application version on Beanstalk and to update the Beanstalk environment with our newly created application version.
Follow [this](http://docs.aws.amazon.com/cli/latest/userguide/installing.html) guide to install the AWS CLI.

### Configure AWS CLI
Follow [this](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html) guide to initially configure the AWS CLI.
As `AWS Region` specify the region where your S3 bucket and your Beanstalk instance is located. As `output format` use `json`.


## Create Beanstalk environment
First let's create a new beanstalk environment in which we can deploy our Play application later on:
```
cd play-docker-beanstalk
eb init ### Select your AWS region

```