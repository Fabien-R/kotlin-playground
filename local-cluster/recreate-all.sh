#!/bin/bash -x

# shellcheck source=/dev/null
source ./variables.sh

kind delete cluster --name="${KIND_CLUSTER_NAME}" || exit 1

kind create cluster --config="${RESOURCES_DIRECTORY}/cluster.yaml" --name="${KIND_CLUSTER_NAME}" || exit 1

cd "${ROOT_DIRECTORY}/${APP_DIRECTORY}" || exit 1
./gradlew assemble || exit 1
./gradlew jibDockerBuild || exit 1
cd "${ROOT_DIRECTORY}/${CHART_DIRECTORY}" || exit 1

kind load docker-image "${APP_IMAGE_NAME}" --name="${KIND_CLUSTER_NAME}" || exit 1

helm dependency build "${CHART_DIRECTORY}" || exit 1 # TODO

#kubectl apply -f "${RESOURCES_DIRECTORY}/postgres-cluster-db.yaml"
#kubectl wait \
#  --for=condition=ready pod \
#  --selector=app=postgres-cluster-db \
#  --timeout=20s || exit 1
#
#helm install"${CHART_NAME}" "${CHART_DIRECTORY}" \
#  --set image.repository="kotlin-playground" \
#  --debug ||
#  exit 1
