#!/bin/bash -x

# shellcheck source=/dev/null
source ./variables.sh

kind delete cluster --name="${KIND_CLUSTER_NAME}" || exit 1

kind create cluster --config="${RESOURCES_DIRECTORY}/cluster.yaml" --name="${KIND_CLUSTER_NAME}" || exit 1

cd "${ROOT_DIRECTORY}/${APP_DIRECTORY}" || exit 1
./gradlew assemble || exit 1
./gradlew jibDockerBuild || exit 1
cd "${ROOT_DIRECTORY}/${LOCAL_CLUSTER_DIRECTORY}" || exit 1

kind load docker-image "${APP_IMAGE_NAME}" --name="${KIND_CLUSTER_NAME}" || exit 1

cd "${ROOT_DIRECTORY}/${CHART_DIRECTORY}" || exit 1
helm dependency update || exit 1
helm dependency build || exit 1
cd "${ROOT_DIRECTORY}/${LOCAL_CLUSTER_DIRECTORY}" || exit 1

kubectl apply -f "${RESOURCES_DIRECTORY}/postgres-cluster-db.yaml"
kubectl wait \
  --for=condition=ready pod \
  --selector=app=postgres-cluster-db \
  --timeout=35s || exit 1

helm install "${CHART_NAME}" "${ROOT_DIRECTORY}/${CHART_DIRECTORY}" \
  --debug ||
  exit 1
#  --set image.repository="kotlin-playground" \
