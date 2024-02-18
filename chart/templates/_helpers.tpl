{{/*
Expand the name of the chart.
*/}}
{{- define "chart.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "chart.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "chart.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "chart.labels" -}}
helm.sh/chart: {{ include "chart.chart" . }}
{{ include "chart.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "chart.selectorLabels" -}}
app.kubernetes.io/name: {{ include "chart.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "chart.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "chart.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
retrieveOrComputeSecret extract existing secret from a forced value password or use the existing secretData or compute a random one if missing
This function needs 3 arguments:
- The current secretData if exists
- The secret key to search for inside the secretData
- an override to use a forced value for the secret
*/}}
{{- define "retrieveOrComputeSecret" }}
{{- $existingSecretData := index . 0 -}}
{{- $secretKey := index . 1 -}}
{{- $secretObj := index . 2 -}}
{{- $fallbackValue := (get $existingSecretData $secretKey) | default (randAlphaNum 20 | b64enc) }}
{{/* workaround for b64enc func that does not support `nil` input */}}
{{- $secretContent := $secretObj.password | default "" | b64enc | default $fallbackValue }}
{{- $secretKey }}: {{ $secretContent }}
{{- end -}}


{{/*
Name of the config
*/}}
{{-  define "configName" -}}
{{ include "chart.name" .}}-config
{{- end }}
{{/*
Name of the secret
*/}}
{{-  define "secretName" -}}
{{ include "chart.name" .}}-secret
{{- end }}