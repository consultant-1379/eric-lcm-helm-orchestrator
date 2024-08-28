{{/* vim: set filetype=mustache: */}}


{{/*
Create a map from ".Values.global" with defaults if missing in values file.
This hides defaults from values file.
*/}}
{{ define "eric-ran-rad-rce-base.global" }}
  {{- $globalDefaults := dict "security" (dict "tls" (dict "enabled" true)) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "annotations" (dict)) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "labels" (dict)) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "nodeSelector" (dict)) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "registry" (dict "url" "")) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "registry" (dict "imagePullPolicy" "IfNotPresent")) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "pullSecret" "") -}}
  {{- $globalDefaults := merge $globalDefaults (dict "timezone" "UTC") -}}
  {{ if .Values.global }}
    {{- mergeOverwrite $globalDefaults .Values.global | toJson -}}
  {{ else }}
    {{- $globalDefaults | toJson -}}
  {{ end }}
{{ end }}


{{/*
Create full image path
*/}}
{{- define "eric-ran-rad-rce-base.imagePath" }}
    {{- $productInfo := index (.root.Files.Get "eric-product-info.yaml" | fromYaml).images .container -}}
    {{- $global := fromJson (include "eric-ran-rad-rce-base.global" .root) -}}
    {{- $registryUrl := $global.registry.url | default $productInfo.registry -}}
    {{- $repoPath := $productInfo.repoPath -}}
    {{- $image := $productInfo.name -}}
    {{- $tag := $productInfo.tag -}}
    {{- if .root.Values.global -}}
        {{- if .root.Values.global.registry -}}
            {{- if .root.Values.global.registry.url -}}
                {{- $registryUrl = .root.Values.global.registry.url -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .root.Values.global.registry.repoPath) -}}
                 {{- $repoPath = .root.Values.global.registry.repoPath -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- $registryUrl = index .root.Values.imageCredentials .container "registry" "url" | default $registryUrl -}}
    {{- if not (kindIs "invalid" (index .root.Values.imageCredentials .container "repoPath")) -}}
        {{- $repoPath = index .root.Values.imageCredentials .container "repoPath" -}}
    {{- end -}}
    {{- if .root.Values.images -}}
        {{- if index .root.Values.images .container -}}
            {{- $image = index .root.Values.images .container "name" | default $image -}}
            {{- $tag = index .root.Values.images .container "tag" | default $tag -}}
        {{- end -}}
    {{- end -}}
    {{- $imagePath := printf "%s/%s/%s:%s" $registryUrl $repoPath $image $tag -}}
    {{- print (regexReplaceAll "[/]+" $imagePath "/") -}}
{{- end -}}


{{/*
*/}}
{{- define "eric-ran-rad-rce-base.tls" -}}
    {{- $global := fromJson (include "eric-ran-rad-rce-base.global" .) -}}
    {{- $global.security.tls.enabled -}}
{{- end -}}


{{/*
Expand the name of the chart.
*/}}
{{- define "eric-ran-rad-rce-base.name" -}}
    {{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}


{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-ran-rad-rce-base.version" -}}
    {{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}


{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-ran-rad-rce-base.chart" -}}
    {{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}


{{/*
Create container name for eric-ran-rad-rce-base
*/}}
{{- define "eric-ran-rad-rce-base.eric-ran-rad-rce-base-container-name" -}}
{{- printf "eric-ran-rad-rce-base" -}}
{{- end -}}


{{/*
Create container name for the Helm test container
*/}}
{{- define "eric-ran-rad-rce-base.helm-test-container-name" -}}
{{- printf "helm-test" -}}
{{- end -}}


{{/*
Create image pull secret
*/}}
{{- define "eric-ran-rad-rce-base.pullSecrets" -}}
    {{- $global := fromJson (include "eric-ran-rad-rce-base.global" .) -}}
    {{- .Values.imageCredentials.pullSecret | default $global.pullSecret -}}
{{- end -}}


{{/*
Create image pull policy
*/}}
{{- define "eric-ran-rad-rce-base.imagePullPolicy" -}}
    {{- $global := fromJson (include "eric-ran-rad-rce-base.global" .root) -}}
    {{- index .root.Values.imageCredentials .container "registry" "imagePullPolicy" | default $global.registry.imagePullPolicy -}}
{{- end -}}


{{/*
Create timezone
*/}}
{{- define "eric-ran-rad-rce-base.timezone" -}}
  {{- $global := fromJson (include "eric-ran-rad-rce-base.global" .) -}}
  {{- print $global.timezone | quote -}}
{{- end -}}


{{/*
Create Ericsson product specific annotations (DR-D1121-064)
*/}}
{{- define "eric-ran-rad-rce-base.product-info" -}}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end -}}


{{/*
Create the set of user defined annotations to be applied on all resources
*/}}
{{ define "eric-ran-rad-rce-base.config-annotations" }}
    {{- $global := fromJson (include "eric-ran-rad-rce-base.global" .) -}}
    {{- include "eric-ran-rad-rce-base.mergeAnnotations" (dict "location" .Template.Name "sources" (list $global.annotations .Values.annotations)) }}
{{- end }}


{{/*
Create the set of labels to be used by Selector.
*/}}
{{- define "eric-ran-rad-rce-base.selector-labels" -}}
app.kubernetes.io/name: {{ include "eric-ran-rad-rce-base.label" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}


{{/*
Create the set of standard labels to be applied on all resources
*/}}
{{- define "eric-ran-rad-rce-base.standard-labels" -}}
{{- include "eric-ran-rad-rce-base.selector-labels" . }}
sidecar.istio.io/inject: "false"
app.kubernetes.io/version: {{ include "eric-ran-rad-rce-base.version" . | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service | quote }}
helm.sh/chart: {{ include "eric-ran-rad-rce-base.chart" . | quote }}
{{- end }}

{{/*
Create the set of user defined labels to be applied on all resources
*/}}
{{- define "eric-ran-rad-rce-base.config-labels" -}}
    {{- $global := fromJson (include "eric-ran-rad-rce-base.global" .) -}}
    {{- include "eric-ran-rad-rce-base.mergeLabels" (dict "location" .Template.Name "sources" (list $global.labels .Values.labels)) }}
{{- end -}}


{{/*
Create the set of common annotations to be applied on all resources
*/}}
{{- define "eric-ran-rad-rce-base.annotations" -}}
    {{- $productInfo := include "eric-ran-rad-rce-base.product-info" . | fromYaml -}}
    {{- $config := include "eric-ran-rad-rce-base.config-annotations" . | fromYaml -}}
    {{- include "eric-ran-rad-rce-base.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $config)) | trim }}
{{- end -}}


{{/*
Create the set of common labels to be applied on all resources
*/}}
{{- define "eric-ran-rad-rce-base.labels" -}}
    {{- $standard := include "eric-ran-rad-rce-base.standard-labels" . | fromYaml -}}
    {{- $config := include "eric-ran-rad-rce-base.config-labels" . | fromYaml -}}
    {{- include "eric-ran-rad-rce-base.mergeLabels" (dict "location" .Template.Name "sources" (list $standard $config)) | trim }}
{{- end -}}


{{/*
Define Security Policy Role Binding creation condition, note: returns boolean as string
*/}}
{{- define "eric-ran-rad-rce-base.securityPolicy.roleBinding" -}}
{{- $psprolebinding := false -}}
{{- if .Values.global -}}
    {{- if .Values.global.security -}}
        {{- if .Values.global.security.policyBinding -}}
            {{- if hasKey .Values.global.security.policyBinding "create" -}}
               {{- $psprolebinding = .Values.global.security.policyBinding.create -}}
           {{- end -}}
        {{- end -}}
    {{- end -}}
{{- end -}}
{{- $psprolebinding -}}
{{- end -}}


{{/*
Define reference to Security Policy mapping
*/}}
{{- define "eric-ran-rad-rce-base.securityPolicy.reference" -}}
{{- $securitypolicyreference := "default-restricted-security-policy" -}}
{{- if .Values.global -}}
    {{- if .Values.global.security -}}
        {{- if .Values.global.security.policyReferenceMap -}}
            {{- if hasKey .Values.global.security.policyReferenceMap "default-restricted-security-policy" -}}
                {{- $securitypolicyreference = index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
{{- end -}}
{{- $securitypolicyreference -}}
{{- end -}}


{{- define "eric-ran-rad-rce-base.securityPolicy.annotations" -}}
# Automatically generated annotations for documentation purposes.
ericsson.com/security-policy.type: "restricted/default"
ericsson.com/security-policy.capabilities: ""
{{- end -}}


{{/*
Create a merged set of nodeSelectors from global and micro-service level for a specific workload.
*/}}
{{ define "eric-ran-rad-rce-base.nodeSelector" }}
    {{- $global := fromJson (include "eric-ran-rad-rce-base.global" .) -}}
    {{- include "eric-ran-rad-rce-base.aggregatedMerge" (dict "context" "nodeSelector" "location" .Template.Name "sources" (list $global.nodeSelector (index .Values.nodeSelector .workload))) | trim }}
{{ end }}


{{/*
Create the list of topologySpreadConstraints
*/}}
{{- define "eric-ran-rad-rce-base.topologySpreadConstraints" }}
{{- range $values := .Values.topologySpreadConstraints }}
- topologyKey: {{ $values.topologyKey }}
  maxSkew: {{ $values.maxSkew }}
  whenUnsatisfiable: {{ $values.whenUnsatisfiable }}
  labelSelector:
    matchLabels: {{- include "eric-ran-rad-rce-base.selector-labels" $ | nindent 6 }}
{{- end }}
{{- end }}


{{/*
Define affinity
*/}}
{{- define "eric-ran-rad-rce-base.affinity" -}}
{{- if eq .Values.affinity.podAntiAffinity "hard" -}}
affinity:
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
        matchExpressions:
        - key: app.kubernetes.io/name
          operator: In
          values:
          - {{ include "eric-ran-rad-rce-base.label" . }}
      topologyKey: "kubernetes.io/hostname"
{{- else if eq .Values.affinity.podAntiAffinity "soft" -}}
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 100
      podAffinityTerm:
        labelSelector:
          matchExpressions:
          - key: app.kubernetes.io/name
            operator: In
            values:
            - {{ include "eric-ran-rad-rce-base.label" . }}
        topologyKey: "kubernetes.io/hostname"
{{- end -}}
{{- end -}}
