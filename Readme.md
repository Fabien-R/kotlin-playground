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

- [Authentication](#Authentication)
- [Troubleshooting](#Troubleshooting)
- [Insee](#Insee)

## Authentication 

I use a free Auth0 instance for the Identity and Access Management service. It supports JWT standard.

TODO  - Authorization

## Insee

[Insee](https://www.insee.fr/en/accueil) is a French institute that collects information on the French economy and societe.
They freely open their enterprises-database we can use as an Identity provider for french enterprises.
They provide several APIs at their [API-store](https://api.insee.fr/catalogue/).

We use the SIREN API allowing us to retrieve the flat list of establishments of legal entity (enterprises).

To be able to query the service we need to create an account and retrieve the consumer key and secret to fill `base64ConsumerKeySecret` of [application.yaml](src/main/resources/application.yaml) as explained [here](https://api.insee.fr/catalogue/site/themes/wso2/subthemes/insee/pages/help.jag)

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