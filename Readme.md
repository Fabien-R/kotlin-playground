# Kotlin Playground

During my last experience in a type-script, react web-application which most of its logic was front-end (start-up environment),
I have missed the back side.

Following the shutdown of the business, I'm going to use this project to go back to the JVM and especially play with Kotlin.
I have always been attracted to it because of its expressiveness, readability and want to compare it to Java and JS/TS.

So this project is my playground to discover some of its benefits and limits

## Goals

* Set up a server
* Use external APIs
* Implement a DSL
* Provide APIs 
* Integrate with an external Authentication and Authorization service
* Integrate with a DB

Let's play!

## Table of Contents

- [Third Parties](#Third Parties)
- [DSL](#DSL)
- [GitHub workflows](#GitHub workflows)
- [Troubleshooting](#Troubleshooting)


## Third Parties

### Authentication 

I use a free Auth0 instance for the Identity and Access Management service. It supports JWT standard.

### Insee

[Insee](https://www.insee.fr/en/accueil) is a French institute that collects information on the French economy and societe.
They freely open their enterprises-database we can use as an Identity provider for french enterprises.
They provide several APIs at their [API-store](https://api.insee.fr/catalogue/).

I use the SIREN API to retrieve the flat list of establishments of legal entities (enterprises).

To be able to query the service I have created an account and retrieve the consumer key and secret to fill `base64ConsumerKeySecret` of [application.yaml](src/main/resources/application.yaml) as explained [here](https://api.insee.fr/catalogue/site/themes/wso2/subthemes/insee/pages/help.jag)

### Codecov

[Codecov](https://about.codecov.io) is a centralized platform to which I send my code coverage reports so that I have:
* a single place to check some quality metrics of different projects and different technos
* the past trend of the metrics for each project

## DSL
I have created a simple DSL for querying the INSEE SIREN API [here](src/main/kotlin/com/fabien/organisationIdentity/insee/InseeQueryDsl.kt).
The goal is to search establishments matching its nationalId or its name.
1. The nationalId search retrieves all establishments whose nationalId wraps the wanted one.
2. The name search uses a set of different INSEE fields: it retrieves all establishments whose at least one of these fields is approximately the name we search for.
3. The zipCode is used to limit the results.

It's really nice to see how much more readable, upgradable it is to use for external APIs.

## GitHub workflows
All workflows use [GitHub Actions](https://github.com/features/actions)  to easily automate my pipelines.
Since this is a fully managed service by GitHub
* it is fully integrated with my repository and its lifecylce
* I don't need to operate the infrastructure (or know how to scale it)
* I have 2000 minutes free each month

From my experience it is lighter than Gitlab CI or even more than Jenkins. 
As long as we use generic kind of pipeline, it is easier to write and maintain.

A [CI pipeline](.github/workflows/ci.yml) is triggered on each pull request or main push and includes:
* linter
* build
* test
* test coverage report

## Troubleshooting

### Insee certificate
```
sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```
1. Need to download pem file using browser
2. Go to the lock icon and click on View certificate
3. Download it with pem format
4. Transform it into cer format ```openssl x509 -in ...api-insee-fr.pem -outform der -out api-insee-fr-root.cer```
5. Upload it to the current jdk cacert stores ```keytool -importcert -keystore ...\openjdk19\current\lib\security\cacerts -file ...\api-insee-fr-root.cer -alias "certignaroot"```