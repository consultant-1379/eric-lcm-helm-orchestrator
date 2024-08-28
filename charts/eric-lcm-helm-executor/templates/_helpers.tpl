{{/*
Create a map from ".Values.global" with defaults if missing in values file.
This hides defaults from values file.
*/}}
{{ define "eric-lcm-helm-executor.global" }}
  {{- $globalDefaults := dict "timezone" "UTC" -}}
  {{- $globalDefaults := merge $globalDefaults (dict "nodeSelector" (dict)) -}}
  {{ if .Values.global }}
    {{- mergeOverwrite $globalDefaults .Values.global | toJson -}}
  {{ else }}
    {{- $globalDefaults | toJson -}}
  {{ end }}
{{ end }}

{{/*
Create a merged set of nodeSelectors from global and service level.
*/}}
{{ define "eric-lcm-helm-executor.NodeSelector" }}
  {{- $global := fromJson (include "eric-lcm-helm-executor.global" .) -}}
  {{- if .Values.nodeSelector -}}
    {{- range $key, $localValue := .Values.nodeSelector -}}
      {{- if hasKey $global.nodeSelector $key -}}
          {{- $globalValue := index $global.nodeSelector $key -}}
          {{- if ne $globalValue $localValue -}}
            {{- printf "nodeSelector \"%s\" is specified in both global (%s: %s) and service level (%s: %s) with differing values which is not allowed." $key $key $globalValue $key $localValue | fail -}}
          {{- end -}}
      {{- end -}}
    {{- end -}}
    {{- toYaml (merge $global.nodeSelector .Values.nodeSelector) | trim -}}
  {{- else -}}
    {{- toYaml $global.nodeSelector | trim -}}
  {{- end -}}
{{ end }}

{{/*
Define RoleBinding value, note: returns boolean as string
*/}}
{{- define "eric-lcm-helm-executor.roleBinding" -}}
{{- $rolebinding := false -}}
{{- if .Values.global -}}
    {{- if .Values.global.security -}}
        {{- if .Values.global.security.policyBinding -}}
            {{- if hasKey .Values.global.security.policyBinding "create" -}}
                {{- $rolebinding = .Values.global.security.policyBinding.create -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
{{- end -}}
{{- $rolebinding -}}
{{- end -}}

{{/*
Define reference to SecurityPolicy
*/}}
{{- define "eric-lcm-helm-executor.securityPolicyReference" -}}
{{- $policyreference := "default-restricted-security-policy" -}}
{{- if .Values.global -}}
    {{- if .Values.global.security -}}
        {{- if .Values.global.security.policyReferenceMap -}}
            {{- if hasKey .Values.global.security.policyReferenceMap "default-restricted-security-policy" -}}
                {{- $policyreference = index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
{{- end -}}
{{- $policyreference -}}
{{- end -}}

{{/*
Setup timezone for app
*/}}
{{- define "eric-lcm-helm-executor.timezone" -}}
    {{- $timezone := "UTC" -}}
    {{- if .Values.timezone -}}
            {{- $timezone = .Values.timezone -}}
    {{- end -}}
    {{- if .Values.global -}}
        {{- if .Values.global.timezone -}}
            {{- $timezone = .Values.global.timezone -}}
        {{- end -}}
    {{- end -}}
    {{- print $timezone -}}
{{- end -}}

{{/*
Setup TLS enable for app
*/}}
{{- define "eric-lcm-helm-executor.tls-enable" -}}
{{- $tls := false -}}
{{- if .Values.global -}}
  {{- if .Values.global.security -}}
    {{- if .Values.global.security.tls -}}
      {{- if hasKey .Values.global.security.tls "enabled" -}}
        {{- $tls = .Values.global.security.tls.enabled -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- $tls -}}
{{- end -}}

{{/*
Define the appArmor annotation for eric-lcm-helm-executor container
*/}}

{{- define "eric-lcm-helm-executor.appArmorProfile.eric-lcm-helm-executor" -}}
{{- if .Values.appArmorProfile }}
{{- $profile := .Values.appArmorProfile }}
    {{- if index .Values.appArmorProfile "eric-lcm-helm-executor" -}}
    {{- $profile = index .Values.appArmorProfile "eric-lcm-helm-executor" -}}
    {{- end -}}
    {{- if $profile.type -}}
    {{- if eq "runtime/default" (lower $profile.type) -}}
container.apparmor.security.beta.kubernetes.io/eric-lcm-helm-executor: runtime/default
    {{- else if eq "unconfined" (lower $profile.type) -}}
container.apparmor.security.beta.kubernetes.io/eric-lcm-helm-executor: unconfined
    {{- else if eq "localhost" (lower $profile.type) -}}
        {{- if $profile.localhostProfile }}
           {{- $localhostProfileList := (split "/" $profile.localhostProfile) -}}
            {{- if $localhostProfileList._1 -}}
container.apparmor.security.beta.kubernetes.io/eric-lcm-helm-executor: localhost/{{ $localhostProfileList._1 }}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- end -}}
    {{- end -}}
{{- end -}}

{{/*
Define the appArmor annotation for eric-lcm-helm-executor init container
*/}}

{{- define "eric-lcm-helm-executor.appArmorProfile.init" -}}
{{- if .Values.appArmorProfile }}
{{- $profile := .Values.appArmorProfile }}
    {{- if index .Values.appArmorProfile "eric-lcm-helm-executor" -}}
    {{- $profile = index .Values.appArmorProfile "eric-lcm-helm-executor" -}}
    {{- end -}}
    {{- if $profile.type -}}
    {{- if eq "runtime/default" (lower $profile.type) -}}
container.apparmor.security.beta.kubernetes.io/init: runtime/default
    {{- else if eq "unconfined" (lower $profile.type) -}}
container.apparmor.security.beta.kubernetes.io/init: unconfined
    {{- else if eq "localhost" (lower $profile.type) -}}
        {{- if $profile.localhostProfile }}
           {{- $localhostProfileList := (split "/" $profile.localhostProfile) -}}
            {{- if $localhostProfileList._1 -}}
container.apparmor.security.beta.kubernetes.io/init: localhost/{{ $localhostProfileList._1 }}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- end -}}
    {{- end -}}
{{- end -}}

{{/*
Define the seccompProfile for container
*/}}

{{- define "eric-lcm-helm-executor.podSeccompProfile" -}}
{{- if and .Values.seccompProfile .Values.seccompProfile.type }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
  {{- if eq .Values.seccompProfile.type "Localhost" }}
  {{- if .Values.seccompProfile.localhostProfile }}
  localhostProfile: {{ .Values.seccompProfile.localhostProfile }}
  {{- else }}
  {{- fail "The 'Localhost' seccomp profile requires a profile name to be provided in localhostProfile parameter." -}}
  {{- end }}
  {{- end }}
{{- end }}
{{- end -}}

{{/*
Create the fsGroup value according to DR-D1123-136.
*/}}
{{- define "eric-lcm-helm-executor.fsGroup.coordinated" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.fsGroup -}}
      {{- if not (kindIs "invalid" .Values.global.fsGroup.manual) -}}
        {{- if (kindIs "string" .Values.global.fsGroup.manual) -}}
          {{- fail "global.fsGroup.manual shall be a positive integer or 0 and not a string" }}
        {{- end -}}
        {{- if ge (.Values.global.fsGroup.manual | int ) 0 }}
          {{- .Values.global.fsGroup.manual | int }}
        {{- else }}
          {{- fail "global.fsGroup.manual shall be a positive integer or 0 if given" }}
        {{- end }}
      {{- else -}}
        {{- if not (kindIs "invalid" .Values.global.fsGroup.namespace) -}}
          {{- if eq (.Values.global.fsGroup.namespace | toString) "true" -}}
            # The 'default' defined in the Security Policy will be used.
          {{- else }}
            {{- if eq (.Values.global.fsGroup.namespace | toString) "false" -}}
              10000
            {{- else }}
              {{- fail "global.fsGroup.namespace shall be true or false if given" }}
            {{- end -}}
          {{- end -}}
        {{- else -}}
          10000
        {{- end -}}
      {{- end -}}
    {{- else -}}
      10000
    {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
{{- end -}}

{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "eric-lcm-helm-executor.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{/*
Create release name used for cluster role.
*/}}
{{- define "eric-lcm-helm-executor.release.name" -}}
{{- default .Release.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Define affinity property
*/}}
{{- define "eric-lcm-helm-executor.affinity" -}}
{{- if eq .Values.affinity.podAntiAffinity "hard" -}}
podAntiAffinity:
  requiredDuringSchedulingIgnoredDuringExecution:
  - labelSelector:
      matchExpressions:
      - key: app
        operator: In
        values:
        - {{ template "eric-lcm-helm-executor.name" . }}
    topologyKey: {{ .Values.affinity.topologyKey }}
{{- else if eq .Values.affinity.podAntiAffinity "soft" -}}
podAntiAffinity:
  preferredDuringSchedulingIgnoredDuringExecution:
  - weight: 100
    podAffinityTerm:
      labelSelector:
        matchExpressions:
        - key: app
          operator: In
          values:
          - {{ template "eric-lcm-helm-executor.name" . }}
      topologyKey: {{ .Values.affinity.topologyKey }}
{{- end -}}
{{- end -}}

{{- define "eric-lcm-helm-executor.tolerations" -}}
    {{- $global := (list) -}}
    {{- if (.Values.global).tolerations -}}
      {{- $global = .Values.global.tolerations -}}
    {{- end -}}
    {{- $local := (list) -}}
    {{- if eq (typeOf .Values.tolerations) ("[]interface {}") -}}
      {{- $local = .Values.tolerations -}}
    {{- else if (index .Values.tolerations "eric-lcm-helm-executor") -}}
      {{- $local = index .Values.tolerations "eric-lcm-helm-executor" -}}
    {{- end -}}
    {{- $merged := (list) -}}
    {{- if $global -}}
        {{- $merged = $global -}}
    {{- end -}}
    {{- if $local -}}
      {{- range $i, $localToleration := $local -}}
        {{- $localValue := get $localToleration "key" -}}
        {{- range $g, $globalToleration := $merged -}}
          {{- $globalValue := get $globalToleration "key" -}}
          {{- if eq $localValue $globalValue -}}
            {{- $merged = without $merged $globalToleration -}}
          {{- end -}}
        {{- end -}}
      {{- end -}}
      {{- $merged = concat $merged $local -}}
    {{- end -}}
    {{- if $merged -}}
        {{- toYaml $merged -}}
    {{- end -}}
    {{- /* Do nothing if both global and local groups are not set */ -}}
  {{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "eric-lcm-helm-executor.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-lcm-helm-executor.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-lcm-helm-executor.version" -}}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create image registry url
*/}}
{{- define "eric-lcm-helm-executor.registryUrl" -}}
    {{- $registryUrl := "armdocker.rnd.ericsson.se" -}}
    {{- if index .Values.imageCredentials "eric-lcm-helm-executor" "registry" -}}
        {{- if  index .Values.imageCredentials "eric-lcm-helm-executor" "registry" "url" -}}
            {{- $registryUrl = index .Values.imageCredentials "eric-lcm-helm-executor" "registry" "url" -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- print $registryUrl -}}
{{- end -}}

{{- define "eric-lcm-helm-executor.mainImagePath" }}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := index $productInfo.images "eric-lcm-helm-executor" "registry" -}}
    {{- $repoPath := index $productInfo.images "eric-lcm-helm-executor" "repoPath" -}}
    {{- $name := index $productInfo.images "eric-lcm-helm-executor" "name" -}}
    {{- $tag := index $productInfo.images "eric-lcm-helm-executor" "tag" -}}
    {{- if ((.Values).global).registry -}}
        {{- if .Values.global.registry.url -}}
            {{- $registryUrl = .Values.global.registry.url -}}
        {{- end -}}
        {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
            {{- $repoPath = .Values.global.registry.repoPath -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if index .Values.imageCredentials "eric-lcm-helm-executor" -}}
            {{- if  index .Values.imageCredentials "eric-lcm-helm-executor" "registry" -}}
                {{- if index .Values.imageCredentials "eric-lcm-helm-executor" "registry" "url" -}}
                    {{- $registryUrl = index .Values.imageCredentials "eric-lcm-helm-executor" "registry" "url" -}}
                {{- end -}}
            {{- end -}}
            {{- if not (kindIs "invalid" ( index .Values.imageCredentials "eric-lcm-helm-executor" "repoPath")) -}}
                {{- $repoPath = index .Values.imageCredentials "eric-lcm-helm-executor" "repoPath" -}}
            {{- end -}}
        {{- end -}}
        {{- if not (kindIs "invalid" ( index .Values.imageCredentials "eric-lcm-helm-executor" "repoPath")) -}}
            {{- $repoPath = index .Values.imageCredentials "eric-lcm-helm-executor" "repoPath" -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

{{- define "eric-lcm-helm-executor.initImagePath" }}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := $productInfo.images.initContainer.registry -}}
    {{- $repoPath := $productInfo.images.initContainer.repoPath -}}
    {{- $name := $productInfo.images.initContainer.name -}}
    {{- $tag := $productInfo.images.initContainer.tag -}}
    {{- if ((.Values).global).registry -}}
        {{- if .Values.global.registry.url -}}
            {{- $registryUrl = .Values.global.registry.url -}}
        {{- end -}}
        {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
            {{- $repoPath = .Values.global.registry.repoPath -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if .Values.imageCredentials.initContainer -}}
            {{- if .Values.imageCredentials.initContainer.registry -}}
                {{- if .Values.imageCredentials.initContainer.registry.url -}}
                    {{- $registryUrl = .Values.imageCredentials.initContainer.registry.url -}}
                {{- end -}}
            {{- end -}}
            {{- if not (kindIs "invalid" ( .Values.imageCredentials.initContainer.repoPath)) -}}
                {{- $repoPath = .Values.imageCredentials.initContainer.repoPath -}}
            {{- end -}}
        {{- end -}}
        {{- if not (kindIs "invalid" ( .Values.imageCredentials.initContainer.repoPath)) -}}
            {{- $repoPath = index .Values.imageCredentials "initContainer" "repoPath" -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

{{/*
Create prometheus info
*/}}
{{- define "eric-lcm-helm-executor.prometheus" -}}
prometheus.io/path: {{ .Values.prometheus.path | quote }}
prometheus.io/port: {{ .Values.service.port | quote }}
prometheus.io/scrape: {{ .Values.prometheus.scrape | quote }}
prometheus.io/scrape-role: {{ .Values.prometheus.role | quote }}
prometheus.io/scrape-interval: {{ .Values.prometheus.interval | quote }}
{{- end -}}

{{/*
Create image pull secrets
*/}}
{{- define "eric-lcm-helm-executor.pullSecrets" -}}
    {{- $globalPullSecret := "" -}}
    {{- if .Values.global -}}
        {{- if .Values.global.pullSecret -}}
            {{- $globalPullSecret = .Values.global.pullSecret -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials.pullSecret -}}
        {{- print .Values.imageCredentials.pullSecret -}}
    {{- else if index .Values.imageCredentials "eric-lcm-helm-executor" "pullSecret" -}}
        {{- print (index .Values.imageCredentials "eric-lcm-helm-executor" "pullSecret") -}}
    {{- else if $globalPullSecret -}}
        {{- print $globalPullSecret -}}
    {{- end -}}
{{- end -}}

{{/*
Create pullPolicy for eric-lcm-helm-executor container
*/}}
{{- define "eric-lcm-helm-executor.imagePullPolicy" -}}
    {{- $globalRegistryPullPolicy := "IfNotPresent" -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.imagePullPolicy -}}
                {{- $globalRegistryPullPolicy = .Values.global.registry.imagePullPolicy -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if index .Values.imageCredentials "eric-lcm-helm-executor" "registry" -}}
        {{- if index .Values.imageCredentials "eric-lcm-helm-executor" "registry" "imagePullPolicy" -}}
        {{- $globalRegistryPullPolicy = index .Values.imageCredentials "eric-lcm-helm-executor" "registry" "imagePullPolicy" -}}
        {{- end -}}
    {{- end -}}
    {{- print $globalRegistryPullPolicy -}}
{{- end -}}

{{- /*
Annotation kubernetes.io
*/ -}}
{{- define "eric-lcm-helm-executor.kubernetes-io" }}
  kubernetes.io/egress-bandwidth: {{ .Values.bandwidth.maxEgressRate }}
{{- end -}}

{{/*
Standard labels of Helm and Kubernetes
*/}}
{{- define "eric-lcm-helm-executor.standard-labels" -}}
app: {{ template "eric-lcm-helm-executor.name" . }}
chart: {{ template "eric-lcm-helm-executor.chart" . }}
release: {{ .Release.Name | quote }}
heritage: {{ .Release.Service }}
app.kubernetes.io/name: {{ template "eric-lcm-helm-executor.name" . }}
app.kubernetes.io/version: {{ template "eric-lcm-helm-executor.version" . }}
app.kubernetes.io/instance: {{ .Release.Name | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "eric-lcm-helm-executor.networkpolicy-labels" -}}
{{- if .Values.networkPolicy -}}
{{- if .Values.networkPolicy.enabled -}}
eric-log-transformer-access: "true"
{{ .Values.postgress.service }}-access: "true"
{{ index .Values "helm-registry" "service" }}-access: "true"
eric-lcm-container-registry-access: "true"
{{ .Values.dst.service }}-access: "true"
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Common labels and custom labels set in values.labels
*/}}
{{- define "eric-lcm-helm-executor.labels" }}
  {{- $standard := include "eric-lcm-helm-executor.standard-labels" . | fromYaml -}}
  {{- $networkpolicy := include "eric-lcm-helm-executor.networkpolicy-labels" . | fromYaml -}}
  {{- $global := (.Values.global).labels -}}
  {{- $service := .Values.labels -}}
  {{- $servicemesh := include "eric-lcm-helm-executor.servicemesh.sidecar" . | fromYaml -}}
  {{- include "eric-lcm-helm-executor.mergeLabels" (dict "location" .Template.Name "sources" (list $standard $global $service $servicemesh $networkpolicy)) | trim }}
{{- end -}}

{{/*
Create a dict of annotations for the product information (DR-D1121-064, DR-D1121-067).
*/}}
{{- define "eric-lcm-helm-executor.product-info" }}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{ regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{/*
Create a user defined annotation (DR-D1121-065, DR-D1121-060)
*/}}
{{ define "eric-lcm-helm-executor.config-annotations" }}
  {{- $global := (.Values.global).annotations -}}
  {{- $service := .Values.annotations -}}
  {{- $servicemesh := include "eric-lcm-helm-executor.servicemesh.sidecar" . | fromYaml -}}
  {{- $appArmorProfile := include "eric-lcm-helm-executor.appArmorProfile.eric-lcm-helm-executor" . | fromYaml -}}
  {{- $appArmorProfileinit := include "eric-lcm-helm-executor.appArmorProfile.init" . | fromYaml -}}
  {{- include "eric-lcm-helm-executor.mergeAnnotations" (dict "location" .Template.Name "sources" (list $global $service $servicemesh $appArmorProfile $appArmorProfileinit)) }}
{{- end }}

{{/*
Common annotations
*/}}
{{- define "eric-lcm-helm-executor.annotations" -}}
  {{- $productInfo := include "eric-lcm-helm-executor.product-info" . | fromYaml -}}
  {{- $config := include "eric-lcm-helm-executor.config-annotations" . | fromYaml -}}
  {{- include "eric-lcm-helm-executor.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $config)) | trim }}
{{- end -}}

{{/*
Common annotations with prometheus
*/}}
{{- define "eric-lcm-helm-executor.annotations-with-prometheus" -}}
  {{- $annotations := include "eric-lcm-helm-executor.annotations" . | fromYaml -}}
  {{- $prometheus := include "eric-lcm-helm-executor.prometheus" . | fromYaml -}}
  {{- include "eric-lcm-helm-executor.mergeAnnotations" (dict "location" .Template.Name "sources" (list $annotations $prometheus)) | trim }}
{{- end -}}


{{- define "eric-lcm-helm-executor.service-mesh-enabled" }}
  {{- $globalMeshEnabled := "false" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
        {{- $globalMeshEnabled = .Values.global.serviceMesh.enabled -}}
    {{- end -}}
  {{- end -}}
  {{- $globalMeshEnabled -}}
{{- end -}}


{{/*
Service Mesh annotations
*/}}
{{- define "eric-lcm-helm-executor.servicemesh.sidecar" -}}
{{- if eq (include "eric-lcm-helm-executor.service-mesh-enabled" .) "true" }}
sidecar.istio.io/inject: "true"
{{- else -}}
sidecar.istio.io/inject: "false"
{{- end -}}
{{- end -}}


{{/*
Defines the annotation for define service mesh volume
*/}}
{{- define "eric-lcm-helm-executor.service-mesh-volume" }}
{{- if and (eq (include "eric-lcm-helm-executor.service-mesh-enabled" .) "true") (eq (include "eric-lcm-helm-executor.tls-enable" .) "true") }}
  {{- if .Values.dst.enabled }}
sidecar.istio.io/userVolume: '{"{{ include "eric-lcm-helm-executor.name" . }}-hcr-sip-tls-cert":{"secret":{"secretName":"{{ include "eric-lcm-helm-executor.name" . }}-hcr-sip-tls-cert","optional":true}},"{{ include "eric-lcm-helm-executor.name" . }}-pg-sip-tls-cert":{"secret":{"secretName":"{{ include "eric-lcm-helm-executor.name" . }}-pg-sip-tls-cert","optional":true}},"{{ include "eric-lcm-helm-executor.name" . }}-dst-sip-tls-cert":{"secret":{"secretName":"{{ include "eric-lcm-helm-executor.name" . }}-dst-sip-tls-cert","optional":true}},"eric-sec-sip-tls-trusted-root-cert":{"secret":{"secretName":"eric-sec-sip-tls-trusted-root-cert","optional":true}}}'
sidecar.istio.io/userVolumeMount: '{"{{ include "eric-lcm-helm-executor.name" . }}-hcr-sip-tls-cert":{"mountPath":"/etc/istio/tls/{{ index .Values "helm-registry" "service" }}","readOnly":true},"{{ include "eric-lcm-helm-executor.name" . }}-pg-sip-tls-cert":{"mountPath":"/etc/istio/tls/{{ .Values.postgress.service }}","readOnly":true},"{{ include "eric-lcm-helm-executor.name" . }}-dst-sip-tls-cert":{"mountPath":"/etc/istio/tls/{{ .Values.dst.service }}","readOnly":true},"eric-sec-sip-tls-trusted-root-cert":{"mountPath":"/etc/istio/tls-ca","readOnly":true}}'
  {{- else -}}
sidecar.istio.io/userVolume: '{"{{ include "eric-lcm-helm-executor.name" . }}-hcr-sip-tls-cert":{"secret":{"secretName":"{{ include "eric-lcm-helm-executor.name" . }}-hcr-sip-tls-cert","optional":true}},"{{ include "eric-lcm-helm-executor.name" . }}-pg-sip-tls-cert":{"secret":{"secretName":"{{ include "eric-lcm-helm-executor.name" . }}-pg-sip-tls-cert","optional":true}},"eric-sec-sip-tls-trusted-root-cert":{"secret":{"secretName":"eric-sec-sip-tls-trusted-root-cert","optional":true}}}'
sidecar.istio.io/userVolumeMount: '{"{{ include "eric-lcm-helm-executor.name" . }}-hcr-sip-tls-cert":{"mountPath":"/etc/istio/tls/{{ index .Values "helm-registry" "service" }}","readOnly":true},"{{ include "eric-lcm-helm-executor.name" . }}-pg-sip-tls-cert":{"mountPath":"/etc/istio/tls/{{ .Values.postgress.service }}","readOnly":true},"eric-sec-sip-tls-trusted-root-cert":{"mountPath":"/etc/istio/tls-ca","readOnly":true}}'
  {{ end }}
{{ end }}
{{- end -}}


{{/*
This helper defines which out-mesh services are reached by the app.
*/}}
{{- define "eric-lcm-helm-executor.service-mesh-ism2osm-labels" }}
{{- if eq (include "eric-lcm-helm-executor.service-mesh-enabled" .) "true" }}
  {{- if eq (include "eric-lcm-helm-executor.tls-enable" .) "true" }}
{{ index .Values "helm-registry" "service" }}-ism-access: "true"
{{ .Values.postgress.service }}-ism-access: "true"
    {{- if .Values.dst.enabled }}
{{ .Values.dst.service }}-ism-access: "true"
    {{- end }}
  {{- end }}
{{- end -}}
{{- end -}}


{{/* Define networkpolicy know services */}}
{{ define "eric-lcm-helm-executor.networkPolicy.matchLabels"  }}
{{- range $index, $label := .Values.networkPolicy.matchLabels }}
- podSelector:
    matchLabels:
      app.kubernetes.io/name: {{ $label }}
{{- end -}}
{{- end -}}

{{- define "eric-lcm-helm-executor.tmp" -}}
/tmp
{{- end -}}

{{/*
This helper defines whether DST is enabled or not.
*/}}
{{- define "eric-lcm-helm-executor.dst-enabled" }}
  {{- $dstEnabled := "false" -}}
  {{- if .Values.dst -}}
    {{- if .Values.dst.enabled -}}
        {{- $dstEnabled = .Values.dst.enabled -}}
    {{- end -}}
  {{- end -}}
  {{- $dstEnabled -}}
{{- end -}}

{{/*
Define the labels needed for DST
*/}}
{{- define "eric-lcm-helm-executor.dstLabels" -}}
{{- if eq (include "eric-lcm-helm-executor.dst-enabled" .) "true" }}
eric-dst-collector-access: "true"
{{- end }}
{{- end -}}

{{/*
Define the annotations needed for DST
*/}}
{{- define "eric-lcm-helm-executor.dstAnnotations" -}}
{{- if and (eq (include "eric-lcm-helm-executor.dst-enabled" .) "true") (eq (include "eric-lcm-helm-executor.tls-enable" .) "true") }}
traffic.sidecar.istio.io/excludeOutboundPorts: 4318,4317,14250,14268,14269,9411
{{- end }}
{{- end -}}

{{/*
This helper defines which exporter port must be used depending on protocol
*/}}
{{- define "eric-lcm-helm-executor.exporter-port" }}
  {{- $dstExporterPort := .Values.dst.collector.portOtlpGrpc -}}
    {{- if .Values.dst.collector.protocol -}}
      {{- if eq .Values.dst.collector.protocol "http" -}}
        {{- $dstExporterPort = .Values.dst.collector.portOtlpHttp -}}
      {{- end -}}
    {{- end -}}
  {{- $dstExporterPort -}}
{{- end -}}

{{/*
Define DST environment variables
*/}}
{{ define "eric-lcm-helm-executor.dstEnv" }}
{{- if eq (include "eric-lcm-helm-executor.dst-enabled" .) "true" }}
- name: ERIC_TRACING_ENABLED
  value: "true"
- name: ERIC_PROPAGATOR_PRODUCE
  value: {{ .Values.dst.producer.type }}
- name: ERIC_EXPORTER_PROTOCOL
  value: {{ .Values.dst.collector.protocol }}
- name: ERIC_EXPORTER_ENDPOINT
  value: {{ .Values.dst.collector.host }}:{{ include "eric-lcm-helm-executor.exporter-port" . }}
- name: ERIC_SAMPLER_JAEGER_REMOTE_ENDPOINT
  value: {{ .Values.dst.collector.host }}:{{ .Values.dst.collector.portJaegerGrpc }}
{{- if eq .Values.dst.collector.protocol "http"}}
- name: OTEL_EXPORTER_OTLP_TRACES_PROTOCOL
  value: http/protobuf
{{- end }}
{{- else }}
- name: ERIC_TRACING_ENABLED
  value: "false"
{{- end -}}
{{ end }}

{{/*
Define credentials for PG
*/}}
{{ define "eric-lcm-helm-executor.pg-credentials" -}}
{{- $secretName := "eric-data-document-database-pg-credentials" }}
{{- $keyForUsername := "custom-user" }}
{{- $keyForPass := "custom-pwd" }}
{{- if .Values.postgress.credentials.kubernetesSecretName -}}
  {{- $secretName = .Values.postgress.credentials.kubernetesSecretName }}
{{- end -}}
{{- if .Values.postgress.credentials.keyForUserId -}}
  {{- $keyForUsername = .Values.postgress.credentials.keyForUserId }}
{{- end -}}
{{- if .Values.postgress.credentials.keyForUserPw -}}
  {{- $keyForPass = .Values.postgress.credentials.keyForUserPw }}
{{- end -}}
        username: {{ printf "${%s.%s}" $secretName $keyForUsername }}
        password: {{ printf "${%s.%s}" $secretName $keyForPass }}
{{- end -}}

{{/*
Define credentials for PG username
*/}}
{{ define "eric-lcm-helm-executor.pg-creds-username" -}}
{{- $secretName := "eric-data-document-database-pg-credentials" }}
{{- $keyForUsername := "custom-user" }}
{{- if .Values.postgress.credentials.kubernetesSecretName -}}
  {{- $secretName = .Values.postgress.credentials.kubernetesSecretName }}
{{- end -}}
{{- if .Values.postgress.credentials.keyForUserId -}}
  {{- $keyForUsername = .Values.postgress.credentials.keyForUserId }}
{{- end -}}
{{ printf "${%s.%s}" $secretName $keyForUsername }}
{{- end -}}

{{/*
Define credentials for HCR
*/}}
{{ define "eric-lcm-helm-executor.hcr-credentials" -}}
{{- $secretName := "eric-lcm-helm-chart-registry" }}
{{- $keyForUsername := "BASIC_AUTH_USER" }}
{{- $keyForPass := "BASIC_AUTH_PASS" }}
{{- if index .Values "helm-registry" "credentials" "kubernetesSecretName" -}}
  {{- $secretName = index .Values "helm-registry" "credentials" "kubernetesSecretName" }}
{{- end -}}
{{- if index .Values "helm-registry" "credentials" "keyForUserId" -}}
  {{- $keyForUsername = index .Values "helm-registry" "credentials" "keyForUserId" }}
{{- end -}}
{{- if index .Values "helm-registry" "credentials" "keyForUserPw" -}}
  {{- $keyForPass = index .Values "helm-registry" "credentials" "keyForUserPw" }}
{{- end -}}
      username: {{ printf "${%s.%s}" $secretName $keyForUsername }}
      password: {{ printf "${%s.%s}" $secretName $keyForPass }}
{{- end -}}

{{/*
Define credentials for Container registry
*/}}
{{ define "eric-lcm-helm-executor.cr-credentials" -}}
{{- $secretName := "eric-lcm-helm-executor-container-creds" }}
{{- $keyForUsername := "container_cred_id" }}
{{- $keyForPass := "container_cred_pass" }}
{{- if index .Values "container-registry" "credentials" "kubernetesSecretName" -}}
  {{- $secretName = index .Values "container-registry" "credentials" "kubernetesSecretName" }}
{{- end -}}
{{- if index .Values "container-registry" "credentials" "keyForUserId" -}}
  {{- $keyForUsername = index .Values "container-registry" "credentials" "keyForUserId" }}
{{- end -}}
{{- if index .Values "container-registry" "credentials" "keyForUserPw" -}}
  {{- $keyForPass = index .Values "container-registry" "credentials" "keyForUserPw" }}
{{- end -}}
      username: {{ printf "${%s.%s}" $secretName $keyForUsername }}
      password: {{ printf "${%s.%s}" $secretName $keyForPass }}
{{- end -}}
