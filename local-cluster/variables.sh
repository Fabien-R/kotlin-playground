#!/bin/bash -x

# shellcheck disable=SC2034
KIND_CLUSTER_NAME='kotlin-playground'
ROOT_DIRECTORY='..'
APP_DIRECTORY='app'
LOCAL_CLUSTER_DIRECTORY='local-cluster'
RESOURCES_DIRECTORY='resources'
CHART_DIRECTORY='chart'
CHART_NAME='kotlin-playground'
APP_IMAGE_NAME='ghcr.io/fabien-r/kotlin-playground:local'